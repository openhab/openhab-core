/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

package org.openhab.core.automation.internal.profiles;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ConfigOptionProvider.class)
@NonNullByDefault
public class ScriptProfileConfigOptionProvider implements ConfigOptionProvider {
    private final String CONFIG_URI = "profile:system:script";

    private final RuleRegistry ruleRegistry;

    @Activate
    public ScriptProfileConfigOptionProvider(@Reference RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (!uri.toString().equals(CONFIG_URI)) {
            return null;
        }

        // all params get the same options
        var rules = ruleRegistry.getByTag(ScriptProfile.SCRIPT_TAG);

        return rules.stream().sorted(comparing(Rule::getName)).map(r -> new ParameterOption(r.getUID(), r.getName()))
                .collect(toList());
    }
}
