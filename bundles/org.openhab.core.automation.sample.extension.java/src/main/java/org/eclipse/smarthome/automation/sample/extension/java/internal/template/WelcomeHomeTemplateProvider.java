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
package org.eclipse.smarthome.automation.sample.extension.java.internal.template;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.RuleTemplateProvider;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The purpose of this class is to illustrate how to provide Rule Templates and how to use them for creation of the
 * {@link Rule}s. Of course, the templates are not mandatory RuleTemplateProvider the creation of rules, the rules also
 * can be created
 * directly.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class WelcomeHomeTemplateProvider implements RuleTemplateProvider {

    private Map<String, RuleTemplate> providedRuleTemplates;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration providerReg;

    public WelcomeHomeTemplateProvider() {
        providedRuleTemplates = new HashMap<String, RuleTemplate>();
        providedRuleTemplates.put(AirConditionerRuleTemplate.UID, AirConditionerRuleTemplate.initialize());
    }

    /**
     * To provide the {@link Template}s should register the WelcomeHomeTemplateProvider as {@link TemplateProvider}
     * service.
     *
     * @param bc
     *            is a bundle's execution context within the Framework.
     */
    public void register(BundleContext bc) {
        providerReg = bc.registerService(RuleTemplateProvider.class.getName(), this, null);
    }

    /**
     * This method unregisters the WelcomeHomeTemplateProvider as {@link TemplateProvider}
     * service.
     */
    public void unregister() {
        providerReg.unregister();
        providerReg = null;
        providedRuleTemplates = null;
    }

    @Override
    public RuleTemplate getTemplate(String UID, Locale locale) {
        return providedRuleTemplates.get(UID);
    }

    @Override
    public Collection<RuleTemplate> getTemplates(Locale locale) {
        return providedRuleTemplates.values();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
        // does nothing because this provider does not change
    }

    @Override
    public Collection<RuleTemplate> getAll() {
        return Collections.unmodifiableCollection(providedRuleTemplates.values());
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
        // does nothing because this provider does not change
    }

}
