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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.automation.module.core.provider.i18n.ModuleTypeI18nService;
import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class is implementation of {@link ModuleTypeProvider}. It serves for providing {@link ModuleType}s by loading
 * bundle resources. It extends functionality of {@link AbstractResourceBundleProvider} by specifying:
 * <ul>
 * <li>the path to resources, corresponding to the {@link ModuleType}s - root directory
 * {@link AbstractResourceBundleProvider#ROOT_DIRECTORY} with sub-directory "moduletypes".
 * <li>type of the {@link Parser}s, corresponding to the {@link ModuleType}s - {@link Parser#PARSER_MODULE_TYPE}
 * <li>specific functionality for loading the {@link ModuleType}s
 * <li>tracking the managing service of the {@link ModuleType}s.
 * </ul>
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 * @author Yordan Mihaylov - updates related to api changes
 */
@Component(immediate = true, service = { ModuleTypeProvider.class, Provider.class }, property = "provider.type=bundle")
public class ModuleTypeResourceBundleProvider extends AbstractResourceBundleProvider<ModuleType>
        implements ModuleTypeProvider {

    private ModuleTypeI18nService moduleTypeI18nService;

    /**
     * This constructor is responsible for initializing the path to resources and tracking the
     * {@link ModuleTypeRegistry}.
     *
     * @param context is the {@code BundleContext}, used for creating a tracker for {@link Parser} services.
     */
    public ModuleTypeResourceBundleProvider() {
        listeners = new LinkedList<ProviderChangeListener<ModuleType>>();
        path = ROOT_DIRECTORY + "/moduletypes/";
    }

    @Override
    @Activate
    protected void activate(BundleContext bc) {
        super.activate(bc);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(parser.type=parser.module.type)")
    protected void addParser(Parser<ModuleType> parser, Map<String, String> properties) {
        super.addParser(parser, properties);
    }

    @Override
    protected void removeParser(Parser<ModuleType> parser, Map<String, String> properties) {
        super.removeParser(parser, properties);
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        super.setTranslationProvider(i18nProvider);
    }

    @Override
    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        super.unsetTranslationProvider(i18nProvider);
    }

    @Override
    public Collection<ModuleType> getAll() {
        return providedObjectsHolder.values();
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

    /**
     * @see ModuleTypeProvider#getModuleType(java.lang.String, java.util.Locale)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> T getModuleType(String UID, Locale locale) {
        return (T) getPerLocale(providedObjectsHolder.get(UID), locale);
    }

    /**
     * @see ModuleTypeProvider#getModuleTypes(java.util.Locale)
     */
    @Override
    public Collection<ModuleType> getModuleTypes(Locale locale) {
        List<ModuleType> moduleTypesList = new ArrayList<ModuleType>();
        for (ModuleType mt : providedObjectsHolder.values()) {
            moduleTypesList.add(getPerLocale(mt, locale));
        }
        return moduleTypesList;
    }

    @Override
    protected String getUID(ModuleType parsedObject) {
        return parsedObject.getUID();
    }

    /**
     * This method is used to localize the {@link ModuleType}s.
     *
     * @param element is the {@link ModuleType} that must be localized.
     * @param locale represents a specific geographical, political, or cultural region.
     * @return the localized {@link ModuleType}.
     */
    private ModuleType getPerLocale(ModuleType defModuleType, Locale locale) {
        String uid = defModuleType.getUID();
        Bundle bundle = getBundle(uid);

        return moduleTypeI18nService.getModuleTypePerLocale(defModuleType, locale, bundle);
    }

    @Reference
    protected void setModuleTypeI18nService(ModuleTypeI18nService moduleTypeI18nService) {
        this.moduleTypeI18nService = moduleTypeI18nService;
    }

    protected void unsetModuleTypeI18nService(ModuleTypeI18nService moduleTypeI18nService) {
        this.moduleTypeI18nService = null;
    }
}
