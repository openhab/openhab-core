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
package org.eclipse.smarthome.automation.internal.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is base for {@link ModuleTypeProvider}, {@link TemplateProvider} and RuleImporter which are responsible
 * for execution of automation commands.
 * <p>
 * It provides functionality for tracking {@link Parser} services by implementing {@link ServiceTrackerCustomizer} and
 * provides common functionality for exporting automation objects.
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 *
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractCommandProvider<E> implements ServiceTrackerCustomizer {

    protected Logger logger;

    /**
     * A bundle's execution context within the Framework.
     */
    protected BundleContext bc;

    /**
     * This Map provides reference between provider of resources and the loaded objects from these resources.
     * <p>
     * The Map has for keys - {@link URL} resource provider and for values - Lists with UIDs of the objects.
     */
    Map<URL, List<String>> providerPortfolio = new HashMap<URL, List<String>>();

    /**
     * This field is a {@link ServiceTracker} for {@link Parser} services.
     */
    protected ServiceTracker<Parser, Parser> parserTracker;

    /**
     * This Map provides structure for fast access to the {@link Parser}s. This provides opportunity for high
     * performance at runtime of the system.
     */
    protected Map<String, Parser<E>> parsers = new HashMap<String, Parser<E>>();

    /**
     * This Map provides structure for fast access to the provided automation objects. This provides opportunity for
     * high performance at runtime of the system, when the Rule Engine asks for any particular object, instead of
     * waiting it for parsing every time.
     * <p>
     * The Map has for keys UIDs of the objects and for values {@link Localizer}s of the objects.
     */
    protected Map<String, E> providedObjectsHolder = new HashMap<String, E>();

    protected List<ProviderChangeListener<E>> listeners;

    /**
     * This constructor is responsible for creation and opening a tracker for {@link Parser} services.
     *
     * @param context is the {@link BundleContext}, used for creating a tracker for {@link Parser} services.
     */
    @SuppressWarnings("unchecked")
    public AbstractCommandProvider(BundleContext context) {
        this.bc = context;
        logger = LoggerFactory.getLogger(AbstractCommandProvider.this.getClass());
        parserTracker = new ServiceTracker(context, Parser.class.getName(), this);
        parserTracker.open();
    }

    /**
     * This method is inherited from {@link AbstractPersistentProvider}.
     * Extends parent's functionality with closing the {@link Parser} service tracker.
     * Sets <code>null</code> to {@link #parsers}, {@link #providedObjectsHolder}, {@link #providerPortfolio}
     */
    public void close() {
        if (parserTracker != null) {
            parserTracker.close();
            parserTracker = null;
            parsers = null;
            synchronized (providedObjectsHolder) {
                providedObjectsHolder = null;
            }
            synchronized (providerPortfolio) {
                providerPortfolio = null;
            }
        }
    }

    /**
     * This method tracks the {@link Parser} services and stores them into the Map "{@link #parsers}" in the
     * memory, for fast access on demand.
     *
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(ServiceReference reference) {
        @SuppressWarnings("unchecked")
        Parser<E> service = (Parser<E>) bc.getService(reference);
        String key = (String) reference.getProperty(Parser.FORMAT);
        key = key == null ? Parser.FORMAT_JSON : key;
        parsers.put(key, service);
        return service;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        // do nothing
    }

    /**
     * This method removes the {@link Parser} service objects from the Map "{@link #parsers}".
     *
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void removedService(ServiceReference reference, Object service) {
        String key = (String) reference.getProperty(Parser.FORMAT);
        key = key == null ? Parser.FORMAT_JSON : key;
        parsers.remove(key);
    }

    /**
     * This method is responsible for execution of the {@link AutomationCommandExport} operation by choosing the
     * {@link Parser} which to be used for exporting a set of automation objects, in a file. When the choice is made,
     * the chosen {@link Parser} is used to do the export.
     *
     * @param parserType is a criteria for choosing the {@link Parser} which to be used.
     * @param set a Set of automation objects for export.
     * @param file is the file in which to export the automation objects.
     * @throws Exception is thrown when I/O operation has failed or has been interrupted or generating of the text fails
     *             for some reasons.
     */
    public String exportData(String parserType, Set<E> set, File file) throws Exception {
        Parser<E> parser = parsers.get(parserType);
        if (parser != null) {
            try (OutputStreamWriter oWriter = new OutputStreamWriter(new FileOutputStream(file))) {
                parser.serialize(set, oWriter);
                return AutomationCommand.SUCCESS;
            }
        } else {
            return String.format("%s! Parser \"%s\" not found!", AutomationCommand.FAIL, parserType);
        }
    }

    /**
     * This method is responsible for execution of the {@link AutomationCommandImport} operation.
     *
     * @param parser the {@link Parser} which to be used for operation.
     * @param inputStreamReader
     * @return the set of automation objects created as result of the {@link AutomationCommandImport} operation.
     *         Operation can be successful or can fail because of {@link ParsingException}.
     * @throws ParsingException is thrown when there are exceptions during the parsing process. It accumulates all of
     *             them.
     */
    protected abstract Set<E> importData(URL url, Parser<E> parser, InputStreamReader inputStreamReader)
            throws ParsingException;

}
