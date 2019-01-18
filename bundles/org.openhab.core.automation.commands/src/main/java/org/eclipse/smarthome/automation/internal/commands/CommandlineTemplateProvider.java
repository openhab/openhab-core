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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.parser.ParsingNestedException;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.RuleTemplateProvider;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.automation.template.TemplateRegistry;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * This class is implementation of {@link TemplateProvider}. It extends functionality of {@link AbstractCommandProvider}
 * <p>
 * It is responsible for execution of {@link AutomationCommandsPluggable}, corresponding to the {@link RuleTemplate}s:
 * <ul>
 * <li>imports the {@link RuleTemplate}s from local files or from URL resources
 * <li>provides functionality for persistence of the {@link RuleTemplate}s
 * <li>removes the {@link RuleTemplate}s and their persistence
 * </ul>
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 *
 */
public class CommandlineTemplateProvider extends AbstractCommandProvider<RuleTemplate> implements RuleTemplateProvider {

    /**
     * This field holds a reference to the {@link ModuleTypeProvider} service registration.
     */
    @SuppressWarnings("rawtypes")
    protected ServiceRegistration tpReg;
    private final TemplateRegistry<RuleTemplate> templateRegistry;

    /**
     * This constructor creates instances of this particular implementation of {@link TemplateProvider}. It does not add
     * any new functionality to the constructors of the providers. Only provides consistency by invoking the parent's
     * constructor.
     *
     * @param context is the {@link BundleContext}, used for creating a tracker for {@link Parser} services.
     */
    public CommandlineTemplateProvider(BundleContext context, TemplateRegistry<RuleTemplate> templateRegistry) {
        super(context);
        listeners = new LinkedList<ProviderChangeListener<RuleTemplate>>();
        tpReg = bc.registerService(RuleTemplateProvider.class.getName(), this, null);
        this.templateRegistry = templateRegistry;
    }

    /**
     * This method differentiates what type of {@link Parser}s is tracked by the tracker.
     * For this concrete provider, this type is a {@link RuleTemplate} {@link Parser}.
     *
     * @see AbstractCommandProvider#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(@SuppressWarnings("rawtypes") ServiceReference reference) {
        if (reference.getProperty(Parser.PARSER_TYPE).equals(Parser.PARSER_TEMPLATE)) {
            return super.addingService(reference);
        }
        return null;
    }

    /**
     * This method is responsible for exporting a set of RuleTemplates in a specified file.
     *
     * @param parserType is relevant to the format that you need for conversion of the RuleTemplates in text.
     * @param set a set of RuleTemplates to export.
     * @param file a specified file for export.
     * @throws Exception when I/O operation has failed or has been interrupted or generating of the text fails
     *             for some reasons.
     * @see AutomationCommandsPluggable#exportTemplates(String, Set, File)
     */
    public String exportTemplates(String parserType, Set<RuleTemplate> set, File file) throws Exception {
        return super.exportData(parserType, set, file);
    }

    /**
     * This method is responsible for importing a set of RuleTemplates from a specified file or URL resource.
     *
     * @param parserType is relevant to the format that you need for conversion of the RuleTemplates in text.
     * @param url a specified URL for import.
     * @throws IOException when I/O operation has failed or has been interrupted.
     * @throws ParsingException when parsing of the text fails for some reasons.
     * @see AutomationCommandsPluggable#importTemplates(String, URL)
     */
    public Set<RuleTemplate> importTemplates(String parserType, URL url) throws IOException, ParsingException {
        Parser<RuleTemplate> parser = parsers.get(parserType);
        if (parser != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(new BufferedInputStream(url.openStream()));
            try {
                return importData(url, parser, inputStreamReader);
            } finally {
                inputStreamReader.close();
            }
        } else {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.TEMPLATE, null,
                    new Exception("Parser " + parserType + " not available")));
        }
    }

    @Override
    public RuleTemplate getTemplate(String UID, Locale locale) {
        synchronized (providerPortfolio) {
            return providedObjectsHolder.get(UID);
        }
    }

    @Override
    public Collection<RuleTemplate> getTemplates(Locale locale) {
        synchronized (providedObjectsHolder) {
            return providedObjectsHolder.values();
        }
    }

    /**
     * This method is responsible for removing a set of objects loaded from a specified file or URL resource.
     *
     * @param providerType specifies the provider responsible for removing the objects loaded from a specified file or
     *            URL resource.
     * @param url is a specified file or URL resource.
     * @return the string <b>SUCCESS</b>.
     */
    public String remove(URL url) {
        List<String> portfolio = null;
        synchronized (providerPortfolio) {
            portfolio = providerPortfolio.remove(url);
        }
        if (portfolio != null && !portfolio.isEmpty()) {
            synchronized (providedObjectsHolder) {
                for (String uid : portfolio) {
                    notifyListeners(providedObjectsHolder.remove(uid));
                }
            }
        }
        return AutomationCommand.SUCCESS;
    }

    @Override
    public void close() {
        if (tpReg != null) {
            tpReg.unregister();
            tpReg = null;
        }
        super.close();
    }

    @Override
    protected Set<RuleTemplate> importData(URL url, Parser<RuleTemplate> parser, InputStreamReader inputStreamReader)
            throws ParsingException {
        Set<RuleTemplate> providedObjects = parser.parse(inputStreamReader);
        if (providedObjects != null && !providedObjects.isEmpty()) {
            List<String> portfolio = new ArrayList<String>();
            synchronized (providerPortfolio) {
                providerPortfolio.put(url, portfolio);
            }
            List<ParsingNestedException> importDataExceptions = new ArrayList<ParsingNestedException>();
            for (RuleTemplate ruleT : providedObjects) {
                List<ParsingNestedException> exceptions = new ArrayList<ParsingNestedException>();
                String uid = ruleT.getUID();
                checkExistence(uid, exceptions);
                if (exceptions.isEmpty()) {
                    portfolio.add(uid);
                    synchronized (providedObjectsHolder) {
                        notifyListeners(providedObjectsHolder.put(uid, ruleT), ruleT);
                    }
                } else {
                    importDataExceptions.addAll(exceptions);
                }
            }
            if (!importDataExceptions.isEmpty()) {
                throw new ParsingException(importDataExceptions);
            }
        }
        return providedObjects;
    }

    /**
     * This method is responsible for checking the existence of {@link Template}s with the same
     * UIDs before these objects to be added in the system.
     *
     * @param uid UID of the newly created {@link Template}, which to be checked.
     * @param exceptions accumulates exceptions if {@link ModuleType} with the same UID exists.
     */
    protected void checkExistence(String uid, List<ParsingNestedException> exceptions) {
        if (templateRegistry == null) {
            exceptions.add(new ParsingNestedException(ParsingNestedException.TEMPLATE, uid,
                    new IllegalArgumentException("Failed to create Rule Template with UID \"" + uid
                            + "\"! Can't guarantee yet that other Rule Template with the same UID does not exist.")));
        }
        if (templateRegistry.get(uid) != null) {
            exceptions.add(new ParsingNestedException(ParsingNestedException.TEMPLATE, uid,
                    new IllegalArgumentException("Rule Template with UID \"" + uid
                            + "\" already exists! Failed to create a second with the same UID!")));
        }
    }

    @Override
    public Collection<RuleTemplate> getAll() {
        return new LinkedList<RuleTemplate>(providedObjectsHolder.values());
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    protected void notifyListeners(RuleTemplate oldElement, RuleTemplate newElement) {
        synchronized (listeners) {
            for (ProviderChangeListener<RuleTemplate> listener : listeners) {
                if (oldElement != null) {
                    listener.updated(this, oldElement, newElement);
                }
                listener.added(this, newElement);
            }
        }
    }

    protected void notifyListeners(RuleTemplate removedObject) {
        if (removedObject != null) {
            synchronized (listeners) {
                for (ProviderChangeListener<RuleTemplate> listener : listeners) {
                    listener.removed(this, removedObject);
                }
            }
        }
    }

}
