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
package org.eclipse.smarthome.config.core;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Test for Configuration class.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigurationTest {

    public final static class ConfigClass {
        public enum MyEnum {
            ON,
            OFF,
            UNKNOWN
        };

        public MyEnum enumField = MyEnum.UNKNOWN;
        public int intField;
        public boolean booleanField;
        public String stringField = "somedefault";
        public List<String> listField;
        @SuppressWarnings("unused")
        private static final String CONSTANT = "SOME_CONSTANT";
    }

    @Test
    public void assertGetConfigAsWorks() {
        Configuration configuration = new Configuration();
        configuration.put("intField", 1);
        configuration.put("booleanField", false);
        configuration.put("stringField", "test");
        configuration.put("enumField", "ON");
        configuration.put("listField", Arrays.asList("one", "two", "three"));
        configuration.put("notExisitingProperty", true);

        ConfigClass configClass = configuration.as(ConfigClass.class);

        assertThat(configClass.intField, is(equalTo(1)));
        assertThat(configClass.booleanField, is(false));
        assertThat(configClass.stringField, is("test"));
        assertThat(configClass.enumField, is(ConfigClass.MyEnum.ON));
        assertThat(configClass.listField, is(hasItems("one", "two", "three")));
    }

    @Test
    public void assertGetConfigAsEnumWrongValue() {
        Configuration configuration = new Configuration();
        configuration.put("enumField", "TEST");

        ConfigClass configClass = configuration.as(ConfigClass.class);

        assertThat(configClass.enumField, is(ConfigClass.MyEnum.UNKNOWN));
    }

    @Test
    public void assertConfigAllowsNullValues() {
        Configuration configuration = new Configuration();
        configuration.put("stringField", null);
        configuration.put("anotherField", null);

        // ensure conversions are null-tolerant and don't throw exceptions
        Map<String, Object> props = configuration.getProperties();
        Set<String> keys = configuration.keySet();
        List<Object> values = new ArrayList<>(configuration.values());

        // ensure copies, not views
        configuration.put("stringField", "someValue");
        configuration.put("additionalField", "");
        assertThat(props.get("stringField"), is(nullValue()));
        assertThat(values.get(0), is(nullValue()));
        assertThat(values.get(1), is(nullValue()));
        assertThat(values.size(), is(2));
        assertThat(keys.size(), is(2));
    }

    @Test
    public void assertPropertiesCanBeRemoved() {
        Map<String, Object> orgProperties = new HashMap<>();
        orgProperties.put("intField", 1);
        orgProperties.put("booleanField", false);
        orgProperties.put("stringField", "test");
        orgProperties.put("notExisitingProperty", true);

        Map<String, Object> newProperties = new HashMap<>();
        newProperties.put("booleanField", false);
        newProperties.put("stringField", "test");
        newProperties.put("notExisitingProperty", true);

        Configuration configuration = new Configuration(orgProperties);

        assertThat(configuration.get("intField"), is(equalTo(BigDecimal.ONE)));

        configuration.setProperties(newProperties);

        assertThat(configuration.get("intField"), is(nullValue()));
    }

    @Test
    public void assertToStringHandlesNullValuesGracefully() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("stringField", null);

        Configuration configuration = new Configuration(properties);
        String res = configuration.toString();
        assertThat(res.contains("type=?"), is(true));
    }

    @Test
    public void assertNormalizationInSetProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("intField", 1);

        Configuration configuration = new Configuration();
        configuration.setProperties(properties);
        assertThat(configuration.get("intField"), is(equalTo(BigDecimal.ONE)));
    }

    @Test
    public void assertNormalizationInPut() {
        Configuration configuration = new Configuration();
        configuration.put("intField", 1);
        assertThat(configuration.get("intField"), is(equalTo(BigDecimal.ONE)));
    }

    @Test
    public void assertSingleValueListMapping() {
        Configuration configuration = new Configuration();
        configuration.put("listField", "one");

        ConfigClass configClass = configuration.as(ConfigClass.class);

        assertThat(configClass.listField, is(hasItems("one")));
    }
}
