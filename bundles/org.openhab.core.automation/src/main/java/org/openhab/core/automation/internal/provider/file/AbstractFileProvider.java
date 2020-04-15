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
package org.openhab.core.automation.internal.provider.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.template.TemplateProvider;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is base for {@link ModuleTypeProvider} and {@link TemplateProvider}, responsible for importing the
 * automation objects from local file system.
 * <p>
 * It provides functionality for tracking {@link Parser} services and provides common functionality for notifying the
 * {@link ProviderChangeListener}s for adding, updating and removing the {@link ModuleType}s or {@link Template}s.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractFileProvider<@NonNull E> implements Provider<E> {

    protected static final String CONFIG_PROPERTY_ROOTS = "roots";
    protected final Logger logger = LoggerFactory.getLogger(AbstractFileProvider.class);

    protected final String rootSubdirectory;
    protected String[] configurationRoots;

    /**
     * This Map provides structure for fast access to the provided automation objects. This provides opportunity for
     * high performance at runtime of the system, when the Rule Engine asks for any particular object, instead of
     * waiting it for parsing every time.
     * <p>
     * The Map has for keys URLs of the files containing automation objects and for values - parsed objects.
     */
    protected final Map<String, E> providedObjectsHolder = new ConcurrentHashMap<>();

    /**
     * This Map provides structure for fast access to the {@link Parser}s. This provides opportunity for high
     * performance at runtime of the system.
     */
    private final Map<String, Parser<E>> parsers = new ConcurrentHashMap<>();

    /**
     * This map is used for mapping the imported automation objects to the file that contains them. This provides
     * opportunity when an event for deletion of the file is received, how to recognize which objects are removed.
     */
    private final Map<URL, List<String>> providerPortfolio = new ConcurrentHashMap<>();

    /**
     * This Map holds URL resources that waiting for a parser to be loaded.
     */
    private final Map<String, List<URL>> urls = new ConcurrentHashMap<>();
    private final List<ProviderChangeListener<E>> listeners = new ArrayList<>();

    public AbstractFileProvider(String root) {
        this.rootSubdirectory = root;
        configurationRoots = new String[] { ConfigConstants.getConfigFolder() + File.separator + "automation" };
    }

    public void activate(Map<String, Object> config) {
        modified(config);
    }

    public void deactivate() {
        for (String root : this.configurationRoots) {
            deactivateWatchService(root + File.separator + rootSubdirectory);
        }
        urls.clear();
        parsers.clear();
        synchronized (listeners) {
            listeners.clear();
        }
        providerPortfolio.clear();
        providedObjectsHolder.clear();
    }

    public synchronized void modified(Map<String, Object> config) {
        String roots = (String) config.get(CONFIG_PROPERTY_ROOTS);
        if (roots != null) {
            for (String root : this.configurationRoots) {
                if (!roots.contains(root)) {
                    deactivateWatchService(root + File.separator + rootSubdirectory);
                }
            }
            this.configurationRoots = roots.split(",");
        }
        for (int i = 0; i < this.configurationRoots.length; i++) {
            initializeWatchService(this.configurationRoots[i] + File.separator + rootSubdirectory);
        }
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<E> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<E> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Imports resources from the specified file or directory.
     *
     * @param file the file or directory to import resources from
     */
    public void importResources(File file) {
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isHidden()) {
                        importResources(f);
                    }
                }
            } else {
                try {
                    URL url = file.toURI().toURL();
                    String parserType = getParserType(url);
                    importFile(parserType, url);
                } catch (MalformedURLException e) {
                    // can't happen for the 'file' protocol handler with a correctly formatted URI
                    logger.debug("Can't create a URL", e);
                }
            }
        }
    }

    /**
     * Removes resources that were loaded from the specified file or directory when the file or directory disappears.
     *
     * @param file the file or directory to import resources from
     */
    public void removeResources(File file) {
        String path = file.getAbsolutePath();
        for (URL key : providerPortfolio.keySet()) {
            try {
                File f = new File(key.toURI());
                if (f.getAbsolutePath().startsWith(path)) {
                    List<String> portfolio = providerPortfolio.remove(key);
                    removeElements(portfolio);
                }
            } catch (URISyntaxException e) {
                // can't happen for the 'file' protocol handler with a correctly formatted URI
                logger.debug("Can't create a URI", e);
            }
        }
    }

    /**
     * This method provides functionality for tracking {@link Parser} services.
     *
     * @param parser {@link Parser} service
     * @param properties
     */
    public void addParser(Parser<E> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.put(parserType, parser);
        List<URL> value = urls.get(parserType);
        if (value != null && !value.isEmpty()) {
            for (URL url : value) {
                importFile(parserType, url);
            }
        }
    }

    /**
     * This method provides functionality for tracking {@link Parser} services.
     *
     * @param parser {@link Parser} service
     * @param properties
     */
    public void removeParser(Parser<E> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.remove(parserType);
    }

    /**
     * This method is responsible for importing a set of Automation objects from a specified URL resource.
     *
     * @param parserType is relevant to the format that you need for conversion of the Automation objects in text.
     * @param url a specified URL for import.
     */
    protected void importFile(String parserType, URL url) {
        Parser<E> parser = parsers.get(parserType);
        if (parser != null) {
            InputStream is = null;
            InputStreamReader inputStreamReader = null;
            try {
                is = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                inputStreamReader = new InputStreamReader(bis);
                Set<E> providedObjects = parser.parse(inputStreamReader);
                updateProvidedObjectsHolder(url, providedObjects);
            } catch (ParsingException e) {
                logger.debug("{}", e.getMessage(), e);
            } catch (IOException e) {
                logger.debug("{}", e.getMessage(), e);
            } finally {
                if (inputStreamReader != null) {
                    try {
                        inputStreamReader.close();
                    } catch (IOException e) {
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else {
            synchronized (urls) {
                List<URL> value = urls.get(parserType);
                if (value == null) {
                    value = new ArrayList<>();
                    urls.put(parserType, value);
                }
                value.add(url);
            }
            logger.debug("Parser {} not available", parserType, new Exception());
        }
    }

    protected void updateProvidedObjectsHolder(URL url, Set<E> providedObjects) {
        if (providedObjects != null && !providedObjects.isEmpty()) {
            List<String> uids = new ArrayList<>();
            for (E providedObject : providedObjects) {
                String uid = getUID(providedObject);
                uids.add(uid);
                E oldProvidedObject = providedObjectsHolder.put(uid, providedObject);
                notifyListeners(oldProvidedObject, providedObject);
            }
            providerPortfolio.put(url, uids);
        }
    }

    protected void removeElements(List<String> objectsForRemove) {
        if (objectsForRemove != null) {
            for (String removedObject : objectsForRemove) {
                notifyListeners(providedObjectsHolder.remove(removedObject));
            }
        }
    }

    protected void notifyListeners(E oldElement, E newElement) {
        synchronized (listeners) {
            for (ProviderChangeListener<E> listener : listeners) {
                if (oldElement != null) {
                    listener.updated(this, oldElement, newElement);
                } else {
                    listener.added(this, newElement);
                }
            }
        }
    }

    protected void notifyListeners(E removedObject) {
        if (removedObject != null) {
            synchronized (listeners) {
                for (ProviderChangeListener<E> listener : listeners) {
                    listener.removed(this, removedObject);
                }
            }
        }
    }

    protected abstract String getUID(E providedObject);

    protected abstract void initializeWatchService(String watchingDir);

    protected abstract void deactivateWatchService(String watchingDir);

    private String getParserType(URL url) {
        String fileName = url.getPath();
        int index = fileName.lastIndexOf(".");
        String extension = index != -1 ? fileName.substring(index + 1) : "";
        return extension.isEmpty() || "txt".equals(extension) ? Parser.FORMAT_JSON : extension;
    }
}
