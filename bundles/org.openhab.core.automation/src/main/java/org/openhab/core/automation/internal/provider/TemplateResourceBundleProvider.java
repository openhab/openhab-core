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
package org.openhab.core.automation.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.provider.i18n.ModuleI18nUtil;
import org.openhab.core.automation.internal.provider.i18n.RuleTemplateI18nUtil;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.template.TemplateProvider;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.i18n.ConfigI18nLocalizationService;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class is implementation of {@link TemplateProvider}. It serves for providing {@link RuleTemplates}s by loading
 * bundle resources. It extends functionality of {@link AbstractResourceBundleProvider} by specifying:
 * <ul>
 * <li>the path to resources, corresponding to the {@link RuleTemplates}s - root directory
 * {@link AbstractResourceBundleProvider#ROOT_DIRECTORY} with sub-directory "templates".
 * <li>type of the {@link Parser}s, corresponding to the {@link RuleTemplates}s - {@link Parser#PARSER_TEMPLATE}
 * <li>specific functionality for loading the {@link RuleTemplates}s
 * <li>tracking the managing service of the {@link ModuleType}s.
 * <li>tracking the managing of the {@link RuleTemplates}s.
 * </ul>
 *
 * @author Ana Dimova - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 * @author Yordan Mihaylov - updates related to api changes
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleTemplateProvider.class,
        Provider.class }, property = "provider.type=bundle")
public class TemplateResourceBundleProvider extends AbstractResourceBundleProvider<RuleTemplate>
        implements RuleTemplateProvider {

    private final RuleTemplateI18nUtil ruleTemplateI18nUtil;
    private final ModuleI18nUtil moduleI18nUtil;

    /**
     * This constructor is responsible for initializing the path to resources and tracking the managing service of the
     * {@link ModuleType}s and the managing service of the {@link RuleTemplates}s.
     *
     * @param context is the {@code BundleContext}, used for creating a tracker for {@link Parser} services.
     */
    @Activate
    public TemplateResourceBundleProvider(final @Reference ConfigI18nLocalizationService configI18nService,
            final @Reference TranslationProvider i18nProvider) {
        super(ROOT_DIRECTORY + "/templates/");
        this.configI18nService = configI18nService;
        this.ruleTemplateI18nUtil = new RuleTemplateI18nUtil(i18nProvider);
        this.moduleI18nUtil = new ModuleI18nUtil(i18nProvider);
    }

    @Override
    @Activate
    protected void activate(@Nullable BundleContext bundleContext) {
        super.activate(bundleContext);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(parser.type=parser.template)")
    protected void addParser(Parser<RuleTemplate> parser, Map<String, String> properties) {
        super.addParser(parser, properties);
    }

    @Override
    protected void removeParser(Parser<RuleTemplate> parser, Map<String, String> properties) {
        super.removeParser(parser, properties);
    }

    @Override
    public Collection<RuleTemplate> getAll() {
        return providedObjectsHolder.values();
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

    /**
     * @see TemplateProvider#getTemplate(java.lang.String, java.util.Locale)
     */
    @Override
    public @Nullable RuleTemplate getTemplate(String UID, @Nullable Locale locale) {
        return getPerLocale(providedObjectsHolder.get(UID), locale);
    }

    /**
     * @see TemplateProvider#getTemplates(Locale)
     */
    @Override
    public Collection<RuleTemplate> getTemplates(@Nullable Locale locale) {
        List<RuleTemplate> templatesList = new ArrayList<>();
        for (RuleTemplate t : providedObjectsHolder.values()) {
            RuleTemplate rtPerLocale = getPerLocale(t, locale);
            if (rtPerLocale != null) {
                templatesList.add(rtPerLocale);
            }
        }
        return templatesList;
    }

    /**
     * This method is used to localize the {@link Template}s.
     *
     * @param element is the {@link Template} that must be localized.
     * @param locale represents a specific geographical, political, or cultural region.
     * @return the localized {@link Template}.
     */
    private @Nullable RuleTemplate getPerLocale(@Nullable RuleTemplate defTemplate, @Nullable Locale locale) {
        if (locale == null || defTemplate == null) {
            return defTemplate;
        }
        String uid = defTemplate.getUID();
        Bundle bundle = getBundle(uid);

        if (bundle != null && defTemplate instanceof RuleTemplate) {
            String llabel = ruleTemplateI18nUtil.getLocalizedRuleTemplateLabel(bundle, uid, defTemplate.getLabel(),
                    locale);
            String ldescription = ruleTemplateI18nUtil.getLocalizedRuleTemplateDescription(bundle, uid,
                    defTemplate.getDescription(), locale);
            List<ConfigDescriptionParameter> lconfigDescriptions = getLocalizedConfigurationDescription(
                    defTemplate.getConfigurationDescriptions(), bundle, uid, RuleTemplateI18nUtil.RULE_TEMPLATE,
                    locale);
            List<Action> lactions = moduleI18nUtil.getLocalizedModules(defTemplate.getActions(), bundle, uid,
                    RuleTemplateI18nUtil.RULE_TEMPLATE, locale);
            List<Condition> lconditions = moduleI18nUtil.getLocalizedModules(defTemplate.getConditions(), bundle, uid,
                    RuleTemplateI18nUtil.RULE_TEMPLATE, locale);
            List<Trigger> ltriggers = moduleI18nUtil.getLocalizedModules(defTemplate.getTriggers(), bundle, uid,
                    RuleTemplateI18nUtil.RULE_TEMPLATE, locale);
            return new RuleTemplate(uid, llabel, ldescription, defTemplate.getTags(), ltriggers, lconditions, lactions,
                    lconfigDescriptions, defTemplate.getVisibility());
        }
        return null;
    }

    @Override
    protected String getUID(RuleTemplate parsedObject) {
        return parsedObject.getUID();
    }
}
