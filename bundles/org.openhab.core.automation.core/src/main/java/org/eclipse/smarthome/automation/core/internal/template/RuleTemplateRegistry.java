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
package org.eclipse.smarthome.automation.core.internal.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.RuleTemplateProvider;
import org.eclipse.smarthome.automation.template.TemplateProvider;
import org.eclipse.smarthome.automation.template.TemplateRegistry;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.core.common.registry.AbstractRegistry;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.osgi.service.component.annotations.Component;

/**
 * The implementation of {@link TemplateRegistry} that is registered as a service.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - TemplateRegistry extends AbstractRegistry
 */
@Component(service = { TemplateRegistry.class, RuleTemplateRegistry.class }, immediate = true)
public class RuleTemplateRegistry extends AbstractRegistry<RuleTemplate, String, RuleTemplateProvider>
        implements TemplateRegistry<RuleTemplate> {

    public RuleTemplateRegistry() {
        super(RuleTemplateProvider.class);
    }

    @Override
    protected void addProvider(Provider<RuleTemplate> provider) {
        if (provider instanceof TemplateProvider) {
            super.addProvider(provider);
        }
    }

    @Override
    public RuleTemplate get(String templateUID) {
        return get(templateUID, null);
    }

    @Override
    public RuleTemplate get(String templateUID, Locale locale) {
        Entry<Provider<RuleTemplate>, RuleTemplate> prt = getValueAndProvider(templateUID);
        if (prt == null) {
            return null;
        } else {
            RuleTemplate t = locale == null ? prt.getValue()
                    : ((RuleTemplateProvider) prt.getKey()).getTemplate(templateUID, locale);
            return createCopy(t);
        }
    }

    private RuleTemplate createCopy(RuleTemplate template) {
        return new RuleTemplate(template.getUID(), template.getLabel(), template.getDescription(),
                new HashSet<String>(template.getTags()), new ArrayList<>(template.getTriggers()),
                new ArrayList<>(template.getConditions()), new ArrayList<>(template.getActions()),
                new LinkedList<ConfigDescriptionParameter>(template.getConfigurationDescriptions()),
                template.getVisibility());
    }

    @Override
    public Collection<RuleTemplate> getByTag(String tag) {
        return getByTag(tag, null);
    }

    @Override
    public Collection<RuleTemplate> getByTag(String tag, Locale locale) {
        Collection<RuleTemplate> result = new ArrayList<>();
        forEach((provider, resultTemplate) -> {
            Collection<String> tags = resultTemplate.getTags();
            RuleTemplate t = locale == null ? resultTemplate
                    : ((RuleTemplateProvider) provider).getTemplate(resultTemplate.getUID(), locale);
            if (tag == null || tags.contains(tag)) {
                result.add(t);
            }
        });
        return result;
    }

    @Override
    public Collection<RuleTemplate> getByTags(String... tags) {
        return getByTags(null, tags);
    }

    @Override
    public Collection<RuleTemplate> getByTags(Locale locale, String... tags) {
        Set<String> tagSet = tags != null ? new HashSet<>(Arrays.asList(tags)) : null;
        Collection<RuleTemplate> result = new ArrayList<>();
        forEach((provider, resultTemplate) -> {
            Collection<String> tTags = resultTemplate.getTags();
            RuleTemplate t = locale == null ? resultTemplate
                    : ((RuleTemplateProvider) provider).getTemplate(resultTemplate.getUID(), locale);
            if (tTags.containsAll(tagSet)) {
                result.add(t);
            }
        });
        return result;
    }

    @Override
    public Collection<RuleTemplate> getAll(Locale locale) {
        return getByTag(null, locale);
    }

}
