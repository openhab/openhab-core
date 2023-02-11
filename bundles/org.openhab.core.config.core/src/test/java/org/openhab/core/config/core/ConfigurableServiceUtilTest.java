/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.openhab.core.config.core.ConfigurableServiceUtil.*;

import java.util.Properties;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ConfigurableServiceUtil}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class ConfigurableServiceUtilTest {

    @Test
    public void asConfigurableServiceDefinedProperties() {
        String category = "system";
        String descriptionURI = "system:inbox";
        boolean factory = true;
        String label = "Inbox";

        Properties properties = new Properties();
        properties.put(SERVICE_PROPERTY_CATEGORY, category);
        properties.put(SERVICE_PROPERTY_DESCRIPTION_URI, descriptionURI);
        properties.put(SERVICE_PROPERTY_FACTORY_SERVICE, factory);
        properties.put(SERVICE_PROPERTY_LABEL, label);

        ConfigurableService configurableService = ConfigurableServiceUtil
                .asConfigurableService((key) -> properties.get(key));

        assertThat(configurableService.annotationType(), is(ConfigurableService.class));
        assertThat(configurableService.category(), is(category));
        assertThat(configurableService.description_uri(), is(descriptionURI));
        assertThat(configurableService.factory(), is(factory));
        assertThat(configurableService.label(), is(label));
    }

    @Test
    public void asConfigurableServiceUndefinedProperties() {
        Properties properties = new Properties();

        ConfigurableService configurableService = ConfigurableServiceUtil
                .asConfigurableService((key) -> properties.get(key));

        assertThat(configurableService.annotationType(), is(ConfigurableService.class));
        assertThat(configurableService.category(), is(emptyString()));
        assertThat(configurableService.description_uri(), is(emptyString()));
        assertThat(configurableService.factory(), is(false));
        assertThat(configurableService.label(), is(emptyString()));
    }
}
