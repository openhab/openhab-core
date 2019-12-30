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
package org.openhab.core.automation.internal;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ana Dimova - Initial contribution
 */
public class ConnectionValidatorTest {

    @Test
    public void testValidConnections() {
        Assert.assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "${name}"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "moduleId.outputName"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list[1].name.values"));
        Assert.assertTrue(
                Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module1.map[\"na[m}.\"e\"][1].values_1-2"));
    }

    @Test
    public void testInvalidConnections() {
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "{name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "${name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "${name.values}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "$name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "moduleId.outputName."));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "[1].name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "list.[1]name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, ".module.array[1].name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module1.map\"na[m}.\"e\"]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.map[\"na[m}.\"e\""));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list[1.name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list1].name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.list[1].name."));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module[\"name\"]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.[name]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module[\"name]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONNECTION_PATTERN, "module.[name\"]"));
    }

    @Test
    public void testInvalidConfigReference() {
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, ""));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "{name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "${name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "${name.values}"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "[1].name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "${name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.CONFIG_REFERENCE_PATTERN, "$name.values}"));
    }

    @Test
    public void testValidOutputReference() {
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "[1]"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".phones[1]"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".phones[1].number"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "[\"test\"]"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "[\"test\"].name"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".map[\"test\"]"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".map[\"test\"].name"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.values"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.array[1]"));
        Assert.assertTrue(Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.map[\"na[m}.\"e\"]"));
        Assert.assertTrue(
                Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".bean.map[\"na[m}.\"e\"].values"));
    }

    @Test
    public void testInvalidOutputReference() {
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones."));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones["));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "]phones"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones[].name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "phones[\"\"].name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "list.[1]name.values"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, ".map\"na[m}.\"e\"]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.map[\"na[m}.\"e\""));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.list[1.name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.list1].name"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.list[1].name."));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module[\"name\"]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.[name]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module[\"name]"));
        Assert.assertTrue(!Pattern.matches(ConnectionValidator.OUTPUT_REFERENCE_PATTERN, "module.[name\"]"));
    }
}
