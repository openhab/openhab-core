/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.util.ConditionBuilder;

/**
 * The {@link YamlConditionDTOTest} contains tests for the {@link YamlConditionDTO} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class YamlConditionDTOTest {

    @Test
    public void testConstructor() {
        Condition c = ConditionBuilder.create().withId("condition1").withTypeUID("type1").build();
        YamlConditionDTO condition = new YamlConditionDTO(c);
        assertThat(condition, notNullValue());
        assertThat(condition.id, is("condition1"));
        assertThat(condition.type, is("type1"));
    }

    @Test
    public void testEquals() throws IOException {
        Condition c = ConditionBuilder.create().withId("condition1").withTypeUID("type1").build();
        YamlConditionDTO condition1 = new YamlConditionDTO(c);
        YamlConditionDTO condition2 = new YamlConditionDTO();
        assertNotEquals(condition1, condition2);
        assertEquals(condition1, condition1);
        assertEquals(condition1, new YamlConditionDTO(c));
        assertFalse(condition2.equals(new YamlModuleDTO()));
    }

    @Test
    public void testToString() {
        Condition c = ConditionBuilder.create().withId("condition1").withTypeUID("type1").build();
        YamlConditionDTO condition1 = new YamlConditionDTO(c);
        YamlConditionDTO condition2 = new YamlConditionDTO();
        assertEquals("YamlConditionDTO [inputs={}, id=condition1, type=type1, config={}]", condition1.toString());
        assertEquals("YamlConditionDTO []", condition2.toString());
        condition1.label = "Label1";
        condition1.description = "Description1";
        assertEquals(
                "YamlConditionDTO [inputs={}, id=condition1, type=type1, label=Label1, description=Description1, config={}]",
                condition1.toString());
    }
}
