/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.config.xml.osgi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.xml.util.XmlDocumentReader;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerUtils;
import org.openhab.core.service.ReadyService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link XmlDocumentBundleTracker} tracks files in the specified XML folder
 * of modules and tries to parse them as XML file with the specified
 * {@link XmlDocumentReader}. Any converted XML files are assigned to its
 * according bundle and added to an {@link XmlDocumentProvider} for further
 * processing. For each module an own {@link XmlDocumentProvider} is created by
 * using the specified {@link XmlDocumentProviderFactory}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Benedikt Niehues - Changed resource handling so that resources can be
 *         patched by fragments.
 * @author Simon Kaufmann - Tracking of remaining bundles
 * @author Markus Rathgeb - Harden the usage
 *
 * @param <T> the result type of the conversion
 */
public class XmlDocumentBundleTracker<T> extends BundleTracker<Bundle> {

    /**
     * Execute given method while take the lock and unlock before return.
     *
     * @param lock the lock that should be taken
     * @param supplier the method to execute. The return value of this method is returned
     * @return the return value of the supplier
     */
    private static <T> T withLock(final Lock lock, final Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    /*
     * We use three open states, so we can ensure that open is only allowed to be called if the current state is
     * "created" and cannot be called after a close.
     * If the open is called asynchronously, there is a little chance that the serialized call hierarchy would be:
     * create, close, open
     * This can be handled correctly using three states and checking the transition.
     */
    private static enum OpenState {
        CREATED,
        OPENED,
        CLOSED
    };

    public static final String THREAD_POOL_NAME = "file-processing";

    private final Logger logger = LoggerFactory.getLogger(XmlDocumentBundleTracker.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
    private final String xmlDirectory;
    private final XmlDocumentReader<T> xmlDocumentTypeReader;
    private final XmlDocumentProviderFactory<T> xmlDocumentProviderFactory;
    private final Map<Bundle, XmlDocumentProvider<T>> bundleDocumentProviderMap = new ConcurrentHashMap<>();
    private final Map<Bundle, Future<?>> queue = new ConcurrentHashMap<>();
    private final Set<Bundle> finishedBundles = new CopyOnWriteArraySet<>();
    private final Map<String, ReadyMarker> bundleReadyMarkerRegistrations = new ConcurrentHashMap<>();
    private final String readyMarkerKey;

    private final ReadWriteLock lockOpenState = new ReentrantReadWriteLock();
    private OpenState openState = OpenState.CREATED;

    @SuppressWarnings("rawtypes")
    private BundleTracker relevantBundlesTracker;
    private final ReadyService readyService;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param bundleContext the bundle context to be used for tracking bundles (must not
     *            be null)
     * @param xmlDirectory the directory to search for XML files (must neither be null,
     *            nor empty)
     * @param xmlDocumentTypeReader the XML converter to be used (must not be null)
     * @param xmlDocumentProviderFactory the result object processor to be used (must not be null)
     * @param readyMarkerKey the key to use for registering {@link ReadyMarker}s
     * @throws IllegalArgumentException if any of the arguments is null
     */
    public XmlDocumentBundleTracker(BundleContext bundleContext, String xmlDirectory,
            XmlDocumentReader<T> xmlDocumentTypeReader, XmlDocumentProviderFactory<T> xmlDocumentProviderFactory,
            String readyMarkerKey, ReadyService readyService) throws IllegalArgumentException {
        super(bundleContext, Bundle.ACTIVE, null);

        if (bundleContext == null) {
            throw new IllegalArgumentException("The BundleContext must not be null!");
        }
        if ((xmlDirectory == null) || (xmlDirectory.isEmpty())) {
            throw new IllegalArgumentException("The XML directory must neither be null, nor empty!");
        }
        if (xmlDocumentTypeReader == null) {
            throw new IllegalArgumentException("The XmlDocumentTypeReader must not be null!");
        }
        if (xmlDocumentProviderFactory == null) {
            throw new IllegalArgumentException("The XmlDocumentProviderFactory must not be null!");
        }
        if (readyService == null) {
            throw new IllegalArgumentException("The ReadyService must not be null!");
        }

        this.readyMarkerKey = readyMarkerKey;
        this.xmlDirectory = xmlDirectory;
        this.xmlDocumentTypeReader = xmlDocumentTypeReader;
        this.xmlDocumentProviderFactory = xmlDocumentProviderFactory;
        this.readyService = readyService;
    }

    private boolean isBundleRelevant(Bundle bundle) {
        return isNotFragment(bundle) && isResourcePresent(bundle, xmlDirectory);
    }

    private boolean isNotFragment(Bundle bundle) {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null;
    }

    private Set<Bundle> getRelevantBundles() {
        if (relevantBundlesTracker.getBundles() == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(relevantBundlesTracker.getBundles()).collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public final synchronized void open() {
        final OpenState openState = withLock(lockOpenState.writeLock(), () -> {
            if (this.openState == OpenState.CREATED) {
                this.openState = OpenState.OPENED;
            }
            return this.openState;
        });
        if (openState != OpenState.OPENED) {
            logger.warn("Open XML document bundle tracker forbidden (state: {})", openState);
            return;
        }

        relevantBundlesTracker = new BundleTracker(context,
                Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE, null) {
            @Override
            public Object addingBundle(Bundle bundle, BundleEvent event) {
                return withLock(lockOpenState.readLock(),
                        () -> openState == OpenState.OPENED && isBundleRelevant(bundle) ? bundle : null);
            }
        };
        relevantBundlesTracker.open();

        super.open();
    }

    @Override
    public final synchronized void close() {
        withLock(lockOpenState.writeLock(), () -> openState = OpenState.CLOSED);

        clearQueue();

        super.close();
        unregisterReadyMarkers();
        bundleDocumentProviderMap.clear();
        if (relevantBundlesTracker != null) {
            relevantBundlesTracker.close();
        }
        finishedBundles.clear();
    }

    private void clearQueue() {
        for (Future<?> future : queue.values()) {
            future.cancel(true);
        }
        queue.clear();
    }

    private XmlDocumentProvider<T> acquireXmlDocumentProvider(Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        XmlDocumentProvider<T> xmlDocumentProvider = bundleDocumentProviderMap.get(bundle);
        if (xmlDocumentProvider == null) {
            xmlDocumentProvider = xmlDocumentProviderFactory.createDocumentProvider(bundle);
            logger.trace("Create an empty XmlDocumentProvider for the module '{}'.",
                    ReadyMarkerUtils.getIdentifier(bundle));
            bundleDocumentProviderMap.put(bundle, xmlDocumentProvider);
        }
        return xmlDocumentProvider;
    }

    private void releaseXmlDocumentProvider(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        XmlDocumentProvider<T> xmlDocumentProvider = bundleDocumentProviderMap.get(bundle);
        if (xmlDocumentProvider == null) {
            return;
        }
        try {
            logger.debug("Releasing the XmlDocumentProvider for module '{}'.", ReadyMarkerUtils.getIdentifier(bundle));
            xmlDocumentProvider.release();
        } catch (Exception e) {
            logger.error("Could not release the XmlDocumentProvider for '{}'!", ReadyMarkerUtils.getIdentifier(bundle),
                    e);
        }
        bundleDocumentProviderMap.remove(bundle);
    }

    private void addingObject(Bundle bundle, T object) {
        XmlDocumentProvider<T> xmlDocumentProvider = acquireXmlDocumentProvider(bundle);
        if (xmlDocumentProvider != null) {
            xmlDocumentProvider.addingObject(object);
        }
    }

    private void addingFinished(Bundle bundle) {
        XmlDocumentProvider<T> xmlDocumentProvider = bundleDocumentProviderMap.get(bundle);
        if (xmlDocumentProvider == null) {
            return;
        }
        try {
            xmlDocumentProvider.addingFinished();
        } catch (Exception ex) {
            logger.error("Could not send adding finished event for the module '{}'!",
                    ReadyMarkerUtils.getIdentifier(bundle), ex);
        }
    }

    @Override
    public final synchronized Bundle addingBundle(Bundle bundle, BundleEvent event) {
        addingBundle(bundle);
        return bundle;
    }

    @Override
    public final synchronized void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        logger.trace("Removing the XML related objects from module '{}'...", ReadyMarkerUtils.getIdentifier(bundle));
        finishedBundles.remove(bundle);
        Future<?> future = queue.remove(bundle);
        if (future != null) {
            future.cancel(true);
        }
        releaseXmlDocumentProvider(bundle);
        unregisterReadyMarker(bundle);
    }

    /**
     * This method creates a list where all resources are contained
     * except the ones from the host bundle which are also contained in
     * a fragment. So the fragment bundle resources can override the
     * host bundles resources.
     *
     * @param xmlDocumentPaths the paths within the bundle/fragments
     * @param bundle the host bundle
     * @return the URLs of the resources, never {@code null}
     */
    private Collection<URL> filterPatches(Enumeration<URL> xmlDocumentPaths, Bundle bundle) {
        List<URL> hostResources = new ArrayList<>();
        List<URL> fragmentResources = new ArrayList<>();

        while (xmlDocumentPaths.hasMoreElements()) {
            URL path = xmlDocumentPaths.nextElement();
            if (bundle.getEntry(path.getPath()) != null && bundle.getEntry(path.getPath()).equals(path)) {
                hostResources.add(path);
            } else {
                fragmentResources.add(path);
            }
        }
        if (!fragmentResources.isEmpty()) {
            Map<String, URL> helper = new HashMap<>();
            for (URL url : hostResources) {
                helper.put(url.getPath(), url);
            }
            for (URL url : fragmentResources) {
                helper.put(url.getPath(), url);
            }
            return helper.values();
        }
        return hostResources;
    }

    /**
     * Checks for the existence of a given resource inside the bundle and its attached fragments.
     *
     * Helper method which can be used in {@link #isBundleRelevant(Bundle)}.
     *
     * @param bundle
     * @param path the directory name to look for
     * @return <code>true</code> if the bundle or one of its attached fragments contain the given directory
     */
    private final boolean isResourcePresent(Bundle bundle, String path) {
        return bundle.getEntry(path) != null;
    }

    /**
     * Add a bundle which potentially needs to be processed.
     *
     * This method should be called in order to queue a new bundle for asynchronous processing.
     * It can be used e.g. by a {@link BundleTracker}, detecting a new bundle.
     * <p>
     * If the bundle actually will be put into the queue depends on the presence of the corresponding XML configuration
     * directory.
     *
     * @param bundle
     */
    private void addingBundle(Bundle bundle) {
        if (withLock(lockOpenState.readLock(), () -> openState != OpenState.OPENED)) {
            return;
        }

        queue.put(bundle, scheduler.submit(new Runnable() {
            // this should remain an anonymous class and not be converted to a lambda because of
            // http://bugs.java.com/view_bug.do?bug_id=8073755
            @Override
            public void run() {
                if (withLock(lockOpenState.readLock(), () -> openState != OpenState.OPENED)) {
                    return;
                }
                try {
                    processBundle(bundle);
                } catch (final RuntimeException ex) {
                    // Check if our OSGi instance is still active.
                    // If the component has been deactivated while the execution hide the exception.
                    if (withLock(lockOpenState.readLock(), () -> openState == OpenState.OPENED)) {
                        throw ex;
                    }
                }
            }
        }));
    }

    private void finishBundle(Bundle bundle) {
        queue.remove(bundle);
        finishedBundles.add(bundle);
        registerReadyMarker(bundle);

        Set<Bundle> remainingBundles = getRemainingBundles();
        if (!remainingBundles.isEmpty()) {
            logger.trace("Remaining bundles with {}: {}", xmlDirectory, remainingBundles);
        } else {
            logger.trace("Finished loading bundles with {}", xmlDirectory);
            loadingCompleted();
        }
    }

    private Set<Bundle> getRemainingBundles() {
        return getRelevantBundles().stream().filter(b -> !finishedBundles.contains(b)).collect(Collectors.toSet());
    }

    private void processBundle(Bundle bundle) {
        if (isNotFragment(bundle)) {
            Enumeration<URL> xmlDocumentPaths = bundle.findEntries(xmlDirectory, "*.xml", true);
            if (xmlDocumentPaths != null) {
                Collection<URL> filteredPaths = filterPatches(xmlDocumentPaths, bundle);
                parseDocuments(bundle, filteredPaths);
            }
        }
        finishBundle(bundle);
    }

    private void parseDocuments(Bundle bundle, Collection<URL> filteredPaths) {
        int numberOfParsedXmlDocuments = 0;
        for (URL xmlDocumentURL : filteredPaths) {
            String moduleName = ReadyMarkerUtils.getIdentifier(bundle);
            String xmlDocumentFile = xmlDocumentURL.getFile();
            logger.debug("Reading the XML document '{}' in module '{}'...", xmlDocumentFile, moduleName);
            try {
                T object = xmlDocumentTypeReader.readFromXML(xmlDocumentURL);
                addingObject(bundle, object);
                numberOfParsedXmlDocuments++;
            } catch (Exception ex) {
                // If we are not open, we can stop here.
                if (withLock(lockOpenState.readLock(), () -> openState != OpenState.OPENED)) {
                    return;
                }
                logger.warn("The XML document '{}' in module '{}' could not be parsed: {}", xmlDocumentFile, moduleName,
                        ex.getLocalizedMessage(), ex);
            }
        }
        if (numberOfParsedXmlDocuments > 0) {
            addingFinished(bundle);
        }
    }

    private void registerReadyMarker(Bundle bundle) {
        final String identifier = ReadyMarkerUtils.getIdentifier(bundle);
        if (!bundleReadyMarkerRegistrations.containsKey(identifier)) {
            ReadyMarker readyMarker = new ReadyMarker(readyMarkerKey, identifier);
            readyService.markReady(readyMarker);
            bundleReadyMarkerRegistrations.put(identifier, readyMarker);
        }
    }

    private void unregisterReadyMarker(Bundle bundle) {
        final String identifier = ReadyMarkerUtils.getIdentifier(bundle);
        ReadyMarker readyMarker = bundleReadyMarkerRegistrations.remove(identifier);
        if (readyMarker != null) {
            readyService.unmarkReady(readyMarker);
        }
    }

    private void unregisterReadyMarkers() {
        for (ReadyMarker readyMarker : bundleReadyMarkerRegistrations.values()) {
            readyService.unmarkReady(readyMarker);
        }
        bundleReadyMarkerRegistrations.clear();
    }

    private void loadingCompleted() {
    }

    @Override
    public String toString() {
        return super.toString() + "(" + xmlDirectory + ")";
    }

}
