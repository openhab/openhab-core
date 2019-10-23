/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.provider.i18n;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.i18n.ConfigI18nLocalizationService;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.provider.i18n.ModuleTypeI18nService;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.type.TriggerType;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a service that provides i18n functionality for automation modules
 *
 * @author Stefan Triller - Initial contribution
 */
@Component
@NonNullByDefault
public class ModuleTypeI18nServiceImpl implements ModuleTypeI18nService {

    private final Logger logger = LoggerFactory.getLogger(ModuleTypeI18nServiceImpl.class);

    /**
     * This field holds a reference to the service instance for internationalization support within the platform.
     */
    private final TranslationProvider i18nProvider;
    private final ConfigI18nLocalizationService localizationService;

    @Activate
    public ModuleTypeI18nServiceImpl(final @Reference TranslationProvider i18nProvider,
            final @Reference ConfigI18nLocalizationService localizationService) {
        this.i18nProvider = i18nProvider;
        this.localizationService = localizationService;
    }

    /**
     * This method is used to localize the {@link ModuleType}s.
     *
     * @param element is the {@link ModuleType} that must be localized.
     * @param locale represents a specific geographical, political, or cultural region.
     * @return the localized {@link ModuleType}.
     */
    @Override
    public @Nullable ModuleType getModuleTypePerLocale(@Nullable ModuleType defModuleType, @Nullable Locale locale,
            Bundle bundle) {
        if (defModuleType == null || locale == null) {
            return defModuleType;
        }
        String uid = defModuleType.getUID();
        String llabel = ModuleTypeI18nUtil.getLocalizedModuleTypeLabel(i18nProvider, bundle, uid,
                defModuleType.getLabel(), locale);
        String ldescription = ModuleTypeI18nUtil.getLocalizedModuleTypeDescription(i18nProvider, bundle, uid,
                defModuleType.getDescription(), locale);

        List<ConfigDescriptionParameter> lconfigDescriptionParameters = getLocalizedConfigDescriptionParameters(
                defModuleType.getConfigurationDescriptions(), ModuleTypeI18nUtil.MODULE_TYPE, uid, bundle, locale);
        if (defModuleType instanceof ActionType) {
            return createLocalizedActionType((ActionType) defModuleType, bundle, uid, locale,
                    lconfigDescriptionParameters, llabel, ldescription);
        }
        if (defModuleType instanceof ConditionType) {
            return createLocalizedConditionType((ConditionType) defModuleType, bundle, uid, locale,
                    lconfigDescriptionParameters, llabel, ldescription);
        }
        if (defModuleType instanceof TriggerType) {
            return createLocalizedTriggerType((TriggerType) defModuleType, bundle, uid, locale,
                    lconfigDescriptionParameters, llabel, ldescription);
        }
        return null;
    }

    private @Nullable List<ConfigDescriptionParameter> getLocalizedConfigDescriptionParameters(
            List<ConfigDescriptionParameter> parameters, String prefix, String uid, Bundle bundle,
            @Nullable Locale locale) {
        try {
            return localizationService
                    .getLocalizedConfigDescription(bundle,
                            new ConfigDescription(new URI(prefix + ":" + uid + ".name"), parameters), locale)
                    .getParameters();
        } catch (URISyntaxException e) {
            logger.error("Constructed invalid uri '{}:{}.name'", prefix, uid, e);
            return null;
        }
    }

    /**
     * Utility method for localization of ActionTypes.
     *
     * @param at is an ActionType for localization.
     * @param bundle the bundle providing localization resources.
     * @param moduleTypeUID is an ActionType uid.
     * @param locale represents a specific geographical, political, or cultural region.
     * @param lconfigDescriptions are ActionType localized config descriptions.
     * @param llabel is an ActionType localized label.
     * @param ldescription is an ActionType localized description.
     * @return localized ActionType.
     */
    private @Nullable ActionType createLocalizedActionType(ActionType at, Bundle bundle, String moduleTypeUID,
            @Nullable Locale locale, @Nullable List<ConfigDescriptionParameter> lconfigDescriptions,
            @Nullable String llabel, @Nullable String ldescription) {
        List<Input> inputs = ModuleTypeI18nUtil.getLocalizedInputs(i18nProvider, at.getInputs(), bundle, moduleTypeUID,
                locale);
        List<Output> outputs = ModuleTypeI18nUtil.getLocalizedOutputs(i18nProvider, at.getOutputs(), bundle,
                moduleTypeUID, locale);
        ActionType lat = null;
        if (at instanceof CompositeActionType) {
            List<Action> modules = ModuleI18nUtil.getLocalizedModules(i18nProvider,
                    ((CompositeActionType) at).getChildren(), bundle, moduleTypeUID, ModuleTypeI18nUtil.MODULE_TYPE,
                    locale);
            lat = new CompositeActionType(moduleTypeUID, lconfigDescriptions, llabel, ldescription, at.getTags(),
                    at.getVisibility(), inputs, outputs, modules);
        } else {
            lat = new ActionType(moduleTypeUID, lconfigDescriptions, llabel, ldescription, at.getTags(),
                    at.getVisibility(), inputs, outputs);
        }
        return lat;
    }

    /**
     * Utility method for localization of ConditionTypes.
     *
     * @param ct is a ConditionType for localization.
     * @param bundle the bundle providing localization resources.
     * @param moduleTypeUID is a ConditionType uid.
     * @param locale represents a specific geographical, political, or cultural region.
     * @param lconfigDescriptions are ConditionType localized config descriptions.
     * @param llabel is a ConditionType localized label.
     * @param ldescription is a ConditionType localized description.
     * @return localized ConditionType.
     */
    private @Nullable ConditionType createLocalizedConditionType(ConditionType ct, Bundle bundle, String moduleTypeUID,
            @Nullable Locale locale, @Nullable List<ConfigDescriptionParameter> lconfigDescriptions,
            @Nullable String llabel, @Nullable String ldescription) {
        List<Input> inputs = ModuleTypeI18nUtil.getLocalizedInputs(i18nProvider, ct.getInputs(), bundle, moduleTypeUID,
                locale);
        ConditionType lct = null;
        if (ct instanceof CompositeConditionType) {
            List<Condition> modules = ModuleI18nUtil.getLocalizedModules(i18nProvider,
                    ((CompositeConditionType) ct).getChildren(), bundle, moduleTypeUID, ModuleTypeI18nUtil.MODULE_TYPE,
                    locale);
            lct = new CompositeConditionType(moduleTypeUID, lconfigDescriptions, llabel, ldescription, ct.getTags(),
                    ct.getVisibility(), inputs, modules);
        } else {
            lct = new ConditionType(moduleTypeUID, lconfigDescriptions, llabel, ldescription, ct.getTags(),
                    ct.getVisibility(), inputs);
        }
        return lct;
    }

    /**
     * Utility method for localization of TriggerTypes.
     *
     * @param ct is a TriggerType for localization.
     * @param bundle the bundle providing localization resources.
     * @param moduleTypeUID is a TriggerType uid.
     * @param locale represents a specific geographical, political, or cultural region.
     * @param lconfigDescriptions are TriggerType localized config descriptions.
     * @param llabel is a TriggerType localized label.
     * @param ldescription is a TriggerType localized description.
     * @return localized TriggerType.
     */
    private @Nullable TriggerType createLocalizedTriggerType(TriggerType tt, Bundle bundle, String moduleTypeUID,
            @Nullable Locale locale, @Nullable List<ConfigDescriptionParameter> lconfigDescriptions,
            @Nullable String llabel, @Nullable String ldescription) {
        List<Output> outputs = ModuleTypeI18nUtil.getLocalizedOutputs(i18nProvider, tt.getOutputs(), bundle,
                moduleTypeUID, locale);
        TriggerType ltt = null;
        if (tt instanceof CompositeTriggerType) {
            List<Trigger> modules = ModuleI18nUtil.getLocalizedModules(i18nProvider,
                    ((CompositeTriggerType) tt).getChildren(), bundle, moduleTypeUID, ModuleTypeI18nUtil.MODULE_TYPE,
                    locale);
            ltt = new CompositeTriggerType(moduleTypeUID, lconfigDescriptions, llabel, ldescription, tt.getTags(),
                    tt.getVisibility(), outputs, modules);
        } else {
            ltt = new TriggerType(moduleTypeUID, lconfigDescriptions, llabel, ldescription, tt.getTags(),
                    tt.getVisibility(), outputs);
        }
        return ltt;
    }

}
