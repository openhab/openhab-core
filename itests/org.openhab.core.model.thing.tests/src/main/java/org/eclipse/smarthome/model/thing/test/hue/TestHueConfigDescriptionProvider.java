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
package org.eclipse.smarthome.model.thing.test.hue;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
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
            ConfigDescriptionParameter paramDefault = new ConfigDescriptionParameter("defaultConfig", Type.TEXT) {
                @Override
                public String getDefault() {
                    return "defaultValue";
                };
            };
            ConfigDescriptionParameter paramCustom = new ConfigDescriptionParameter("customConfig", Type.TEXT) {
                @Override
                public String getDefault() {
                    return "none";
                };
            };
            return new ConfigDescription(uri, Stream.of(paramDefault, paramCustom).collect(toList()));
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
