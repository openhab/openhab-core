/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.addon.finder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.candidate.MdnsCandidate;
import org.openhab.core.config.discovery.addon.candidate.UpnpCandidate;
import org.openhab.core.config.discovery.addon.dto.SuggestedAddonCandidates;
import org.openhab.core.config.discovery.addon.xml.AddonCandidatesSerializer;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStreamException;

/**
 * This is a {@link AddonSuggestionFinderService} which discovers suggested
 * addons for the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AddonService.class, name = AddonSuggestionFinderService.SERVICE_NAME)
public class AddonSuggestionFinderService implements AddonService, AutoCloseable {

    /**
     * Inner ServiceListener implementation that ignores call-backs.
     */
    private static class NoOp implements ServiceListener {

        @Override
        public void serviceAdded(@Nullable ServiceEvent event) {
        }

        @Override
        public void serviceRemoved(@Nullable ServiceEvent event) {
        }

        @Override
        public void serviceResolved(@Nullable ServiceEvent event) {
        }
    }

    public static final String SERVICE_ID = "suggestions";
    public static final String SERVICE_NAME = "suggested-addon-finder";

    private static final String XML_RESOURCE_NAME = "suggested-addon-candidates.xml";

    private final AddonCandidatesSerializer addonCandidatesSerializer = new AddonCandidatesSerializer();
    private final Set<AddonService> addonServices = new CopyOnWriteArraySet<>();
    private final Logger logger = LoggerFactory.getLogger(AddonSuggestionFinderService.class);
    private final List<MdnsCandidate> mdnsCandidates = new ArrayList<>();
    private final NoOp noop = new NoOp();
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(SERVICE_NAME);
    private final Set<String> suggestedAddonUids = ConcurrentHashMap.newKeySet();
    private final List<UpnpCandidate> upnpCandidates = new ArrayList<>();

    private final MDNSClient mdnsClient;
    private final UpnpService upnpService;

    private @Nullable Future<?> upnpScanTask;
    private @Nullable Future<?> mdnsScanTask;

    @Activate
    public AddonSuggestionFinderService(@Reference MDNSClient mdnsClient, @Reference UpnpService upnpService)
            throws XStreamException, IOException, IllegalStateException {
        this.mdnsClient = mdnsClient;
        this.upnpService = upnpService;
        initialize();
        startScan();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonService(AddonService addonService) {
        // exclude ourself from addonServices set in order to prevent infinite recursion
        if (!SERVICE_ID.equals(addonService.getId())) {
            addonServices.add(addonService);
        }
    }

    public void removeAddonService(AddonService addonService) {
        addonServices.remove(addonService);
    }

    @Deactivate
    @Override
    public void close() throws Exception {
        mdnsCandidates.forEach(c -> mdnsClient.removeServiceListener(c.getMdnsServiceType(), noop));
        mdnsCandidates.clear();
        upnpCandidates.clear();
        suggestedAddonUids.clear();
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        return getAddons(locale).stream().filter(a -> id.equals(a.getId())).findAny().orElse(null);
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        return addonServices.stream().filter(s -> !SERVICE_ID.equals(s.getId())).map(s -> s.getAddons(locale))
                .flatMap(Collection::stream).filter(a -> suggestedAddonUids.contains(a.getUid())).toList();
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return List.of(AddonType.BINDING);
    }

    /**
     * Initialize the class by loading an XML file that contains the list of
     * potential addon candidates to be suggested.
     * 
     * @throws IOException if there was an error reading the xml file.
     * @throws XStreamException if the xml cannot be deserialized.
     */
    private void initialize() throws XStreamException, IOException, IllegalStateException {
        ClassLoader loader = getClass().getClassLoader();
        if (loader == null) {
            throw new IllegalStateException("Class loader is null");
        }
        InputStream stream = loader.getResourceAsStream(XML_RESOURCE_NAME);
        if (stream == null) {
            throw new IllegalStateException("Resource stream is null");
        }
        String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        SuggestedAddonCandidates candidates = addonCandidatesSerializer.fromXML(xml);
        candidates.getCandidates().stream().forEach(c -> c.getDiscoveryMethods().stream().forEach(m -> {
            switch (m.getServiceType()) {
                case MDNS:
                    mdnsCandidates
                            .add(new MdnsCandidate(c.getAddonUid(), m.getMatchProperties(), m.getMdnsServiceType()));
                    break;
                case UPNP:
                    upnpCandidates.add(new UpnpCandidate(c.getAddonUid(), m.getMatchProperties()));
                    break;
                default:
                    break;
            }
        }));
        mdnsCandidates.forEach(c -> mdnsClient.addServiceListener(c.getMdnsServiceType(), noop));
    }

    @Override
    public void install(String id) {
        // note: startScan() is called from the constructor
    }

    @Override
    public void refreshSource() {
        startScan();
    }

    public boolean scanDone() {
        Future<?> mdnsScanTask = this.mdnsScanTask;
        Future<?> upnpScanTask = this.upnpScanTask;
        return mdnsScanTask != null && mdnsScanTask.isDone() && upnpScanTask != null && upnpScanTask.isDone();
    }

    /**
     * Start the search process to find addons to suggest to be installed.
     */
    private void startScan() {
        if (!mdnsCandidates.isEmpty()) {
            startScanMdns();
        }
        if (!upnpCandidates.isEmpty()) {
            startScanUpnp();
        }
    }

    /**
     * Run a scheduled task to search for matching addon suggestion candidates using
     * the MDNS service.
     */
    @SuppressWarnings("unlikely-arg-type")
    private void startScanMdns() {
        Future<?> task = mdnsScanTask;
        if (task != null) {
            task.cancel(false);
        }
        mdnsScanTask = scheduler.submit(() -> {
            mdnsCandidates.forEach(c -> {
                Arrays.stream(mdnsClient.list(c.getMdnsServiceType())).filter(s -> c.equals(s)).forEach(s -> {
                    suggestionFound(c.getAddonId());
                });
            });
        });
    }

    /**
     * Run a scheduled task to search for matching addon suggestion candidates using
     * the UPnP service.
     */
    @SuppressWarnings("unlikely-arg-type")
    private void startScanUpnp() {
        Future<?> task = upnpScanTask;
        if (task != null) {
            task.cancel(false);
        }
        upnpScanTask = scheduler.submit(() -> {
            upnpService.getRegistry().getRemoteDevices().forEach(d -> {
                upnpCandidates.stream().filter(c -> c.equals(d)).forEach(c -> {
                    suggestionFound(c.getAddonId());
                });
            });
        });
    }

    /**
     * Called back when a new addon suggestion is found.
     * 
     * @param addonUid the Uid of the found addon.
     */
    private synchronized void suggestionFound(String addonUid) {
        if (!suggestedAddonUids.contains(addonUid)) {
            logger.debug("found suggested addon id:{}", addonUid);
            suggestedAddonUids.add(addonUid);
        }
    }

    @Override
    public void uninstall(String id) {
        Future<?> task = upnpScanTask;
        if (task != null) {
            task.cancel(true);
        }
        task = mdnsScanTask;
        if (task != null) {
            task.cancel(true);
        }
    }
}
