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
package org.eclipse.smarthome.core.thing.internal;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.config.core.metadata.MetadataConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;

/**
 * Provider of the config description for the auto update policy metadata.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@NonNullByDefault
@Component
public class AutoUpdateConfigDescriptionProvider implements MetadataConfigDescriptionProvider {

    @Override
    public String getNamespace() {
        return "autoupdate";
    }

    @Override
    public @Nullable String getDescription(@Nullable Locale locale) {
        return "Auto Update";
    }

    @Override
    public @Nullable List<ParameterOption> getParameterOptions(@Nullable Locale locale) {
        return Stream.of( //
                new ParameterOption("true", "Enforce an auto update"), //
                new ParameterOption("false", "Veto an auto update") //
        ).collect(toList());
    }

    @Override
    public @Nullable List<ConfigDescriptionParameter> getParameters(String value, @Nullable Locale locale) {
        return null;
    }

}
