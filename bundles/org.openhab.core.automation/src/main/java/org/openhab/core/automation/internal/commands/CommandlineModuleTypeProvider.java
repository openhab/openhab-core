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
package org.openhab.core.automation.internal.commands;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.parser.ParsingNestedException;
import org.openhab.core.automation.template.TemplateProvider;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * This class is implementation of {@link ModuleTypeProvider}. It extends functionality of
 * {@link AbstractCommandProvider}.
 * <p>
 * It is responsible for execution of {@link AutomationCommandsPluggable}, corresponding to the {@link ModuleType}s:
 * <ul>
 * <li>imports the {@link ModuleType}s from local files or from URL resources
 * <li>provides functionality for persistence of the {@link ModuleType}s
 * <li>removes the {@link ModuleType}s and their persistence
 * <li>lists the {@link ModuleType}s and their details
 * </ul>
 * <p>
 * accordingly to the used command.
 *
 * @author Ana Dimova - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 */
@NonNullByDefault
public class CommandlineModuleTypeProvider extends AbstractCommandProvider<ModuleType> implements ModuleTypeProvider {

    /**
     * This field holds a reference to the {@link TemplateProvider} service registration.
     */
    @SuppressWarnings("rawtypes")
    protected @Nullable ServiceRegistration mtpReg;
    private final ModuleTypeRegistry moduleTypeRegistry;

    /**
     * This constructor creates instances of this particular implementation of {@link ModuleTypeProvider}. It does not
     * add any new functionality to the constructors of the providers. Only provides consistency by invoking the
     * parent's constructor.
     *
     * @param context is the {@code BundleContext}, used for creating a tracker for {@link Parser} services.
     * @param moduleTypeRegistry a ModuleTypeRegistry service
     */
    public CommandlineModuleTypeProvider(BundleContext bundleContext, ModuleTypeRegistry moduleTypeRegistry) {
        super(bundleContext);
        mtpReg = bundleContext.registerService(ModuleTypeProvider.class.getName(), this, null);
        this.moduleTypeRegistry = moduleTypeRegistry;
    }

    /**
     * This method differentiates what type of {@link Parser}s is tracked by the tracker.
     * For this concrete provider, this type is a {@link ModuleType} {@link Parser}.
     *
     * @see AbstractCommandProvider#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public @Nullable Object addingService(@SuppressWarnings("rawtypes") @Nullable ServiceReference reference) {
        if (reference != null && Parser.PARSER_MODULE_TYPE.equals(reference.getProperty(Parser.PARSER_TYPE))) {
            return super.addingService(reference);
        }
        return null;
    }

    /**
     * This method is responsible for exporting a set of ModuleTypes in a specified file.
     *
     * @param parserType is relevant to the format that you need for conversion of the ModuleTypes in text.
     * @param set a set of ModuleTypes to export.
     * @param file a specified file for export.
     * @throws Exception when I/O operation has failed or has been interrupted or generating of the text fails
     *             for some reasons.
     * @see AutomationCommandsPluggable#exportModuleTypes(String, Set, File)
     */
    public String exportModuleTypes(String parserType, Set<ModuleType> set, File file) throws Exception {
        return super.exportData(parserType, set, file);
    }

    /**
     * This method is responsible for importing a set of ModuleTypes from a specified file or URL resource.
     *
     * @param parserType is relevant to the format that you need for conversion of the ModuleTypes in text.
     * @param url a specified URL for import.
     * @throws IOException when I/O operation has failed or has been interrupted.
     * @throws ParsingException when parsing of the text fails for some reasons.
     * @see AutomationCommandsPluggable#importModuleTypes(String, URL)
     */
    public Set<ModuleType> importModuleTypes(String parserType, URL url) throws IOException, ParsingException {
        Parser<ModuleType> parser = parsers.get(parserType);
        if (parser != null) {
            InputStream is = url.openStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            InputStreamReader inputStreamReader = new InputStreamReader(bis);
            try {
                return importData(url, parser, inputStreamReader);
            } finally {
                inputStreamReader.close();
            }
        } else {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.MODULE_TYPE, null,
                    new Exception("Parser " + parserType + " not available")));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable ModuleType getModuleType(String UID, @Nullable Locale locale) {
        synchronized (providedObjectsHolder) {
            return providedObjectsHolder.get(UID);
        }
    }

    @Override
    public Collection<ModuleType> getModuleTypes(@Nullable Locale locale) {
        synchronized (providedObjectsHolder) {
            return !providedObjectsHolder.isEmpty() ? providedObjectsHolder.values()
                    : Collections.<ModuleType> emptyList();
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
        if (mtpReg != null) {
            mtpReg.unregister();
            mtpReg = null;
        }
        super.close();
    }

    @Override
    protected Set<ModuleType> importData(URL url, Parser<ModuleType> parser, InputStreamReader inputStreamReader)
            throws ParsingException {
        Set<ModuleType> providedObjects = parser.parse(inputStreamReader);
        if (providedObjects != null && !providedObjects.isEmpty()) {
            String uid = null;
            List<String> portfolio = new ArrayList<>();
            synchronized (providerPortfolio) {
                providerPortfolio.put(url, portfolio);
            }
            List<ParsingNestedException> importDataExceptions = new ArrayList<>();
            for (ModuleType providedObject : providedObjects) {
                List<ParsingNestedException> exceptions = new ArrayList<>();
                uid = providedObject.getUID();
                checkExistence(uid, exceptions);
                if (exceptions.isEmpty()) {
                    portfolio.add(uid);
                    synchronized (providedObjectsHolder) {
                        notifyListeners(providedObjectsHolder.put(uid, providedObject), providedObject);
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
     * This method is responsible for checking the existence of {@link ModuleType}s with the same
     * UIDs before these objects to be added in the system.
     *
     * @param uid UID of the newly created {@link ModuleType}, which to be checked.
     * @param exceptions accumulates exceptions if {@link ModuleType} with the same UID exists.
     */
    protected void checkExistence(String uid, List<ParsingNestedException> exceptions) {
        if (this.moduleTypeRegistry == null) {
            exceptions.add(new ParsingNestedException(ParsingNestedException.MODULE_TYPE, uid,
                    new IllegalArgumentException("Failed to create Module Type with UID \"" + uid
                            + "\"! Can't guarantee yet that other Module Type with the same UID does not exist.")));
        }
        if (moduleTypeRegistry.get(uid) != null) {
            exceptions.add(new ParsingNestedException(ParsingNestedException.MODULE_TYPE, uid,
                    new IllegalArgumentException("Module Type with UID \"" + uid
                            + "\" already exists! Failed to create a second with the same UID!")));
        }
    }

    @Override
    public Collection<ModuleType> getAll() {
        return new LinkedList<>(providedObjectsHolder.values());
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    protected void notifyListeners(@Nullable ModuleType oldElement, ModuleType newElement) {
        synchronized (listeners) {
            for (ProviderChangeListener<ModuleType> listener : listeners) {
                if (oldElement != null) {
                    listener.updated(this, oldElement, newElement);
                }
                listener.added(this, newElement);
            }
        }
    }

    protected void notifyListeners(@Nullable ModuleType removedObject) {
        if (removedObject != null) {
            synchronized (listeners) {
                for (ProviderChangeListener<ModuleType> listener : listeners) {
                    listener.removed(this, removedObject);
                }
            }
        }
    }
}
