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
package org.openhab.core.model.thing.testsupport.hue;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@Component
public class TestHueConfigDescriptionProvider implements ConfigDescriptionProvider {

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(Locale locale) {
        return emptyList();
    }

    @Override
    public ConfigDescription getConfigDescription(URI uri, Locale locale) {
        if (uri.equals(createURI("hue:LCT001:color"))) {
            ConfigDescriptionParameter paramDefault = ConfigDescriptionParameterBuilder
                    .create("defaultConfig", Type.TEXT).withDefault("defaultValue").build();
            ConfigDescriptionParameter paramCustom = ConfigDescriptionParameterBuilder.create("customConfig", Type.TEXT)
                    .withDefault("none").build();
            return ConfigDescriptionBuilder.create(uri)
                    .withParameters(Stream.of(paramDefault, paramCustom).collect(toList())).build();
        }
        return null;
    }

    private URI createURI(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to create URI: " + s, e);
        }
    }
}
