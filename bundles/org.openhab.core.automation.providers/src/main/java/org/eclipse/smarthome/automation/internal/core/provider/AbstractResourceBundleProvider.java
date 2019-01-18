/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.internal.core.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.config.core.i18n.ConfigDescriptionI18nUtil;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is base for {@link ModuleTypeProvider}, {@link TemplateProvider} and {@code RuleImporter} which are
 * responsible for importing and persisting the {@link ModuleType}s, {@link RuleTemplate}s and {@link Rule}s from
 * bundles which provides resource files.
 * <p>
 * It tracks {@link Parser} services by implementing {@link #addParser(Parser, Map)} and
 * {@link #removeParser(Parser, Map)} methods.
 * <p>
 * The functionality, responsible for tracking the bundles with resources, comes from
 * {@link AutomationResourceBundlesTracker} by implementing a {@link BundleTrackerCustomizer} but the functionality for
 * processing them, comes from this class.
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractResourceBundleProvider<E> {

    public AbstractResourceBundleProvider() {
        logger = LoggerFactory.getLogger(this.getClass());
        providedObjectsHolder = new ConcurrentHashMap<String, E>();
        providerPortfolio = new ConcurrentHashMap<Vendor, List<String>>();
        queue = new AutomationResourceBundlesEventQueue<E>(this);
        parsers = new ConcurrentHashMap<String, Parser<E>>();
        waitingProviders = new ConcurrentHashMap<Bundle, List<URL>>();
    }

    /**
     * This static field provides a root directory for automation object resources in the bundle resources.
     * It is common for all resources - {@link ModuleType}s, {@link RuleTemplate}s and {@link Rule}s.
     */
    protected static final String ROOT_DIRECTORY = "ESH-INF/automation";

    /**
     * This field holds a reference to the service instance for internationalization support within the platform.
     */
    protected TranslationProvider i18nProvider;

    /**
     * This field keeps instance of {@link Logger} that is used for logging.
     */
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * A bundle's execution context within the Framework.
     */
    protected BundleContext bc;

    /**
     * This field is initialized in constructors of any particular provider with specific path for the particular
     * resources from specific type as {@link ModuleType}s, {@link RuleTemplate}s and {@link Rule}s:
     * <li>for
     * {@link ModuleType}s it is a "ESH-INF/automation/moduletypes/"
     * <li>for {@link RuleTemplate}s it is a
     * "ESH-INF/automation/templates/"
     * <li>for {@link Rule}s it is a "ESH-INF/automation/rules/"
     */
    protected String path;

    /**
     * This Map collects all binded {@link Parser}s.
     */
    protected Map<String, Parser<E>> parsers;

    /**
     * This Map provides structure for fast access to the provided automation objects. This provides opportunity for
     * high performance at runtime of the system, when the Rule Engine asks for any particular object, instead of
     * waiting it for parsing every time.
     * <p>
     * The Map has for keys UIDs of the objects and for values one of {@link ModuleType}s, {@link RuleTemplate}s and
     * {@link Rule}s.
     */
    protected Map<String, E> providedObjectsHolder;

    /**
     * This Map provides reference between provider of resources and the loaded objects from these resources.
     * <p>
     * The Map has for keys - {@link Vendor}s and for values - Lists with UIDs of the objects.
     */
    protected Map<Vendor, List<String>> providerPortfolio;

    /**
     * This Map holds bundles whose {@link Parser} for resources is missing in the moment of processing the bundle.
     * Later, if the {@link Parser} appears, they will be added again in the {@link #queue} for processing.
     */
    protected Map<Bundle, List<URL>> waitingProviders;

    /**
     * This field provides an access to the queue for processing bundles.
     */
    protected AutomationResourceBundlesEventQueue queue;

    protected List<ProviderChangeListener<E>> listeners;

    protected void activate(BundleContext bc) {
        this.bc = bc;
    }

    protected void deactivate() {
        bc = null;
        if (queue != null) {
            queue.stop();
        }
        synchronized (parsers) {
            parsers.clear();
        }
        synchronized (providedObjectsHolder) {
            providedObjectsHolder.clear();
        }
        synchronized (providerPortfolio) {
            providerPortfolio.clear();
        }
        synchronized (waitingProviders) {
            waitingProviders.clear();
        }
        if (listeners != null) {
            synchronized (listeners) {
                listeners.clear();
            }
        }
    }

    /**
     * This method is used to initialize field {@link #queue}, when the instance of
     * {@link AutomationResourceBundlesEventQueue} is created.
     *
     * @param queue provides an access to the queue for processing bundles.
     */
    protected AutomationResourceBundlesEventQueue getQueue() {
        return queue;
    }

    /**
     * This method is called before the {@link Parser} services to be added to the {@code ServiceTracker} and storing
     * them in the {@link #parsers} into the memory, for fast access on demand. The returned service object is stored in
     * the {@code ServiceTracker} and is available from the {@code getService} and {@code getServices} methods.
     * <p>
     * Also if there are bundles that were stored in {@link #waitingProviders}, to be processed later, because of
     * missing {@link Parser} for particular format,
     * <p>
     * and then the {@link Parser} service appears, they will be processed.
     *
     * @param parser {@link Parser} service
     * @param properties of the service that has been added.
     */
    protected void addParser(Parser<E> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.put(parserType, parser);
        for (Bundle bundle : waitingProviders.keySet()) {
            if (bundle.getState() != Bundle.UNINSTALLED) {
                processAutomationProvider(bundle);
            }
        }
    }

    /**
     * This method is called after a service is no longer being tracked by the {@code ServiceTracker} and removes the
     * {@link Parser} service objects from the structure Map "{@link #parsers}".
     *
     * @param parser The {@link Parser} service object for the specified referenced service.
     * @param properties of the service that has been removed.
     */
    protected void removeParser(Parser<E> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.remove(parserType);
    }

    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = null;
    }

    /**
     * This method provides common functionality for {@link ModuleTypeProvider} and {@link TemplateProvider} to process
     * the bundles. For {@link RuleResourceBundleImporter} this method is overridden.
     * <p>
     * Checks for availability of the needed {@link Parser}. If it is not available - the bundle is added into
     * {@link #waitingProviders} and the execution of the method ends.
     * <p>
     * If it is available, the execution of the method continues with checking if the version of the bundle is changed.
     * If the version is changed - removes persistence of old variants of the objects, provided by this bundle.
     * <p>
     * Continues with loading the new version of these objects. If this bundle is added for the very first time, only
     * loads the provided objects.
     * <p>
     * The loading can fail because of {@link IOException}.
     *
     * @param bundle it is a {@link Bundle} which has to be processed, because it provides resources for automation
     *            objects.
     */
    protected void processAutomationProvider(Bundle bundle) {
        Enumeration<URL> urlEnum = null;
        try {
            if (bundle.getState() != Bundle.UNINSTALLED) {
                urlEnum = bundle.findEntries(path, null, true);
            }
        } catch (IllegalStateException e) {
            logger.debug("Can't read from resource of bundle with ID {}. The bundle is uninstalled.",
                    bundle.getBundleId(), e);
            processAutomationProviderUninstalled(bundle);
        }
        Vendor vendor = new Vendor(bundle.getSymbolicName(), bundle.getVersion().toString());
        List<String> previousPortfolio = getPreviousPortfolio(vendor);
        List<String> newPortfolio = new LinkedList<String>();
        if (urlEnum != null) {
            while (urlEnum.hasMoreElements()) {
                URL url = urlEnum.nextElement();
                if (url.getPath().endsWith(File.separator)) {
                    continue;
                }
                String parserType = getParserType(url);
                Parser<E> parser = parsers.get(parserType);
                updateWaitingProviders(parser, bundle, url);
                if (parser != null) {
                    Set<E> parsedObjects = parseData(parser, url, bundle);
                    if (parsedObjects != null && !parsedObjects.isEmpty()) {
                        addNewProvidedObjects(newPortfolio, previousPortfolio, parsedObjects);
                    }
                }
            }
            putNewPortfolio(vendor, newPortfolio);
        }
        removeUninstalledObjects(previousPortfolio, newPortfolio);
    }

    @SuppressWarnings("unchecked")
    protected void removeUninstalledObjects(List<String> previousPortfolio, List<String> newPortfolio) {
        if (previousPortfolio != null && !previousPortfolio.isEmpty()) {
            for (String uid : previousPortfolio) {
                if (!newPortfolio.contains(uid)) {
                    E removedObject = providedObjectsHolder.remove(uid);
                    if (listeners != null) {
                        List<ProviderChangeListener<E>> snapshot = null;
                        synchronized (listeners) {
                            snapshot = new LinkedList<ProviderChangeListener<E>>(listeners);
                        }
                        for (ProviderChangeListener<E> listener : snapshot) {
                            listener.removed((Provider<E>) this, removedObject);
                        }
                    }
                }
            }
        }
    }

    protected List<String> getPreviousPortfolio(Vendor vendor) {
        List<String> portfolio = providerPortfolio.remove(vendor);
        if (portfolio == null) {
            for (Vendor v : providerPortfolio.keySet()) {
                if (v.getVendorSymbolicName().equals(vendor.getVendorSymbolicName())) {
                    return providerPortfolio.remove(v);
                }
            }
        }
        return portfolio;
    }

    protected void putNewPortfolio(Vendor vendor, List<String> portfolio) {
        providerPortfolio.put(vendor, portfolio);
    }

    /**
     * This method is used to determine which parser to be used.
     *
     * @param url the URL of the source of data for parsing.
     * @return the type of the parser.
     */
    protected String getParserType(URL url) {
        String fileName = url.getPath();
        int fileExtesionStartIndex = fileName.lastIndexOf(".") + 1;
        if (fileExtesionStartIndex == -1) {
            return Parser.FORMAT_JSON;
        }
        String fileExtesion = fileName.substring(fileExtesionStartIndex);
        if (fileExtesion.equals("txt")) {
            return Parser.FORMAT_JSON;
        }
        return fileExtesion;
    }

    /**
     * This method provides common functionality for {@link ModuleTypeProvider} and {@link TemplateProvider} to process
     * uninstalling the bundles. For {@link RuleResourceBundleImporter} this method is overridden.
     * <p>
     * When some of the bundles that provides automation objects is uninstalled, this method will remove it from
     * {@link #waitingProviders}, if it is still there or from {@link #providerPortfolio} in the other case.
     * <p>
     * Will remove the provided objects from {@link #providedObjectsHolder} and will remove their persistence, injected
     * in the system from this bundle.
     *
     * @param bundle the uninstalled {@link Bundle}, provider of automation objects.
     */
    @SuppressWarnings("unchecked")
    protected void processAutomationProviderUninstalled(Bundle bundle) {
        waitingProviders.remove(bundle);
        Vendor vendor = new Vendor(bundle.getSymbolicName(), bundle.getVersion().toString());
        List<String> portfolio = providerPortfolio.remove(vendor);
        if (portfolio != null && !portfolio.isEmpty()) {
            for (String uid : portfolio) {
                E removedObject = providedObjectsHolder.remove(uid);
                if (listeners != null) {
                    List<ProviderChangeListener<E>> snapshot = null;
                    synchronized (listeners) {
                        snapshot = new LinkedList<ProviderChangeListener<E>>(listeners);
                    }
                    for (ProviderChangeListener<E> listener : snapshot) {
                        listener.removed((Provider<E>) this, removedObject);
                    }
                }
            }
        }
    }

    /**
     * This method is used to get the bundle providing localization resources for {@link ModuleType}s or
     * {@link Template}s.
     *
     * @param uid is the unique identifier of {@link ModuleType} or {@link Template} that must be localized.
     * @return the bundle providing localization resources.
     */
    protected Bundle getBundle(String uid) {
        String symbolicName = null;
        for (Entry<Vendor, List<String>> entry : providerPortfolio.entrySet()) {
            if (entry.getValue().contains(uid)) {
                symbolicName = entry.getKey().getVendorSymbolicName();
                break;
            }
        }
        if (symbolicName != null) {
            Bundle[] bundles = bc.getBundles();
            for (int i = 0; i < bundles.length; i++) {
                if (bundles[i].getSymbolicName().equals(symbolicName)) {
                    return bundles[i];
                }
            }
        }
        return null;
    }

    protected List<ConfigDescriptionParameter> getLocalizedConfigurationDescription(TranslationProvider i18nProvider,
            List<ConfigDescriptionParameter> config, Bundle bundle, String uid, String prefix, Locale locale) {
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<ConfigDescriptionParameter>();
        if (config != null) {
            ConfigDescriptionI18nUtil util = new ConfigDescriptionI18nUtil(i18nProvider);
            for (ConfigDescriptionParameter parameter : config) {
                String parameterName = parameter.getName();
                URI uri = null;
                try {
                    uri = new URI(prefix + ":" + uid + ".name");
                } catch (URISyntaxException e) {
                    logger.error("Constructed invalid uri '{}:{}.name'", prefix, uid, e);
                }
                String llabel = parameter.getLabel();
                if (llabel != null) {
                    llabel = util.getParameterLabel(bundle, uri, parameterName, llabel, locale);
                }
                String ldescription = parameter.getDescription();
                if (ldescription != null) {
                    ldescription = util.getParameterDescription(bundle, uri, parameterName, ldescription, locale);
                }
                String lpattern = parameter.getPattern();
                if (lpattern != null) {
                    lpattern = util.getParameterPattern(bundle, uri, parameterName, lpattern, locale);
                }
                List<ParameterOption> loptions = parameter.getOptions();
                if (loptions != null && !loptions.isEmpty()) {
                    for (ParameterOption option : loptions) {
                        String label = util.getParameterOptionLabel(bundle, uri, parameterName, option.getValue(),
                                option.getLabel(), locale);
                        option = new ParameterOption(option.getValue(), label);
                    }
                }
                String lunitLabel = parameter.getUnitLabel();
                if (lunitLabel != null) {
                    lunitLabel = util.getParameterUnitLabel(bundle, uri, parameterName, parameter.getUnit(), lunitLabel,
                            locale);
                }
                configDescriptions.add(ConfigDescriptionParameterBuilder.create(parameterName, parameter.getType())
                        .withMinimum(parameter.getMinimum()).withMaximum(parameter.getMaximum())
                        .withStepSize(parameter.getStepSize()).withPattern(lpattern)
                        .withRequired(parameter.isRequired()).withMultiple(parameter.isMultiple())
                        .withReadOnly(parameter.isReadOnly()).withContext(parameter.getContext())
                        .withDefault(parameter.getDefault()).withLabel(llabel).withDescription(ldescription)
                        .withFilterCriteria(parameter.getFilterCriteria()).withGroupName(parameter.getGroupName())
                        .withAdvanced(parameter.isAdvanced()).withOptions(loptions)
                        .withLimitToOptions(parameter.getLimitToOptions())
                        .withMultipleLimit(parameter.getMultipleLimit()).withUnit(parameter.getUnit())
                        .withUnitLabel(lunitLabel).build());
            }
        }
        return configDescriptions;
    }

    /**
     * This method is called from {@link #processAutomationProvider(Bundle)} to process the loading of the provided
     * objects.
     *
     * @param vendor is a holder of information about the bundle providing data for import.
     * @param parser the {@link Parser} which is responsible for parsing of a particular format in which the provided
     *            objects are presented
     * @param url the resource which is used for loading the objects.
     * @param bundle is the resource holder
     * @return a set of automation objects - the result of loading.
     */
    protected Set<E> parseData(Parser<E> parser, URL url, Bundle bundle) {
        InputStreamReader reader = null;
        InputStream is = null;
        try {
            is = url.openStream();
            reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            return parser.parse(reader);
        } catch (ParsingException e) {
            logger.error("{}", e.getLocalizedMessage(), e);
        } catch (IOException e) {
            logger.error("Can't read from resource of bundle with ID {}", bundle.getBundleId(), e);
            processAutomationProviderUninstalled(bundle);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void addNewProvidedObjects(List<String> newPortfolio, List<String> previousPortfolio,
            Set<E> parsedObjects) {
        List<ProviderChangeListener<E>> snapshot = null;
        synchronized (listeners) {
            snapshot = new LinkedList<ProviderChangeListener<E>>(listeners);
        }
        for (E parsedObject : parsedObjects) {
            String uid = getUID(parsedObject);
            E oldElement = providedObjectsHolder.get(uid);
            if (oldElement != null && !previousPortfolio.contains(uid)) {
                logger.warn("{} with UID '{}' already exists! Failed to add a second with the same UID!",
                        parsedObject.getClass().getName(), uid);
                continue;
            } else {
                newPortfolio.add(uid);
                providedObjectsHolder.put(uid, parsedObject);
                for (ProviderChangeListener<E> listener : snapshot) {
                    if (oldElement == null) {
                        listener.added((Provider<E>) this, parsedObject);
                    } else {
                        listener.updated((Provider<E>) this, oldElement, parsedObject);
                    }
                }
            }
        }
    }

    protected void updateWaitingProviders(Parser<E> parser, Bundle bundle, URL url) {
        List<URL> urlList = waitingProviders.get(bundle);
        if (parser == null) {
            if (urlList == null) {
                urlList = new ArrayList<URL>();
            }
            urlList.add(url);
            waitingProviders.put(bundle, urlList);
            return;
        }
        if (urlList != null && urlList.remove(url) && urlList.isEmpty()) {
            waitingProviders.remove(bundle);
        }
    }

    /**
     * @param parsedObject
     * @return
     */
    protected abstract String getUID(E parsedObject);

}
