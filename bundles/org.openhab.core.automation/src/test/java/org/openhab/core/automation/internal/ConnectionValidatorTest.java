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
package org.openhab.core.automation.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public class ConnectionValidatorTest {

    @Test
    public void testValidConnections() {
        assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name"));
        assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "${name}"));
        assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "moduleId.outputName"));
        assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list[1].name.values"));
        assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module1.map[\"na[m}.\"e\"][1].values_1-2"));
    }

    @Test
    public void testInvalidConnections() {
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "{name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "${name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "${name.values}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "moduleId.outputName."));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "[1].name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "list.[1]name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, ".module.array[1].name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module1.map\"na[m}.\"e\"]"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.map[\"na[m}.\"e\""));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list[1.name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list1].name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list[1].name."));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module[\"name\"]"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.[name]"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module[\"name]"));
        assertFalse(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.[name\"]"));
    }

    @Test
    public void testInvalidConfigReference() {
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, ""));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "{name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "${name"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "${name.values}"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "[1].name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "${name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name.values}"));
    }

    @Test
    public void testValidOutputReference() {
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "[1]"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".phones[1]"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".phones[1].number"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "[\"test\"]"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "[\"test\"].name"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".map[\"test\"]"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".map[\"test\"].name"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.values"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.array[1]"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.map[\"na[m}.\"e\"]"));
        assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.map[\"na[m}.\"e\"].values"));
    }

    @Test
    public void testInvalidOutputReference() {
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones."));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones["));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "]phones"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones[].name"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones[\"\"].name"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "list.[1]name.values"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".map\"na[m}.\"e\"]"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.map[\"na[m}.\"e\""));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.list[1.name"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.list1].name"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.list[1].name."));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module[\"name\"]"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.[name]"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module[\"name]"));
        assertFalse(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.[name\"]"));
    }
}
