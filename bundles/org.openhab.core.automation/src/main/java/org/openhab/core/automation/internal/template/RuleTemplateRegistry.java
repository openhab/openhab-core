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
package org.openhab.core.automation.internal.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.automation.template.TemplateProvider;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.osgi.service.component.annotations.Component;

/**
 * The implementation of {@link TemplateRegistry} that is registered as a service.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - TemplateRegistry extends AbstractRegistry
 */
@NonNullByDefault
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
    public @Nullable RuleTemplate get(String templateUID) {
        return get(templateUID, null);
    }

    @Override
    public @Nullable RuleTemplate get(String templateUID, @Nullable Locale locale) {
        Entry<Provider<RuleTemplate>, RuleTemplate> prt = getValueAndProvider(templateUID);
        if (prt == null) {
            return null;
        } else {
            RuleTemplate t = locale == null ? prt.getValue()
                    : ((RuleTemplateProvider) prt.getKey()).getTemplate(templateUID, locale);
            return t != null ? createCopy(t) : null;
        }
    }

    private RuleTemplate createCopy(RuleTemplate template) {
        return new RuleTemplate(template.getUID(), template.getLabel(), template.getDescription(),
                new HashSet<>(template.getTags()), new ArrayList<>(template.getTriggers()),
                new ArrayList<>(template.getConditions()), new ArrayList<>(template.getActions()),
                new LinkedList<>(template.getConfigurationDescriptions()), template.getVisibility());
    }

    @Override
    public Collection<RuleTemplate> getByTag(@Nullable String tag) {
        return getByTag(tag, null);
    }

    @Override
    public Collection<RuleTemplate> getByTag(@Nullable String tag, @Nullable Locale locale) {
        Collection<RuleTemplate> result = new ArrayList<>();
        forEach((provider, resultTemplate) -> {
            Collection<String> tags = resultTemplate.getTags();
            RuleTemplate t = locale == null ? resultTemplate
                    : ((RuleTemplateProvider) provider).getTemplate(resultTemplate.getUID(), locale);
            if (t != null && (tag == null || tags.contains(tag))) {
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
    public Collection<RuleTemplate> getByTags(@Nullable Locale locale, String... tags) {
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        Collection<RuleTemplate> result = new ArrayList<>();
        forEach((provider, resultTemplate) -> {
            Collection<String> tTags = resultTemplate.getTags();
            RuleTemplate t = locale == null ? resultTemplate
                    : ((RuleTemplateProvider) provider).getTemplate(resultTemplate.getUID(), locale);
            if (t != null && tTags.containsAll(tagSet)) {
                result.add(t);
            }
        });
        return result;
    }

    @Override
    public Collection<RuleTemplate> getAll(@Nullable Locale locale) {
        return getByTag(null, locale);
    }
}
