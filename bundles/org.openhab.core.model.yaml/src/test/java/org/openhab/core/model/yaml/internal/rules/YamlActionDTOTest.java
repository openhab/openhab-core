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
import org.openhab.core.automation.Action;
import org.openhab.core.automation.util.ActionBuilder;

/**
 * The {@link YamlActionDTOTest} contains tests for the {@link YamlActionDTO} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class YamlActionDTOTest {

    @Test
    public void testConstructor() {
        Action a = ActionBuilder.create().withId("action1").withTypeUID("type1").build();
        YamlActionDTO action = new YamlActionDTO(a);
        assertThat(action, notNullValue());
        assertThat(action.id, is("action1"));
        assertThat(action.type, is("type1"));
    }

    @Test
    public void testEquals() throws IOException {
        Action a = ActionBuilder.create().withId("action1").withTypeUID("type1").build();
        YamlActionDTO action1 = new YamlActionDTO(a);
        YamlActionDTO action2 = new YamlActionDTO();
        assertNotEquals(action1, action2);
        assertEquals(action1, action1);
        assertEquals(action1, new YamlActionDTO(a));
        assertFalse(action2.equals(new YamlModuleDTO()));
    }

    @Test
    public void testToString() {
        Action a = ActionBuilder.create().withId("action1").withTypeUID("type1").build();
        YamlActionDTO action1 = new YamlActionDTO(a);
        YamlActionDTO action2 = new YamlActionDTO();
        assertEquals("YamlActionDTO [inputs={}, id=action1, type=type1, config={}]", action1.toString());
        assertEquals("YamlActionDTO []", action2.toString());
        action1.label = "Label1";
        action1.description = "Description1";
        assertEquals(
                "YamlActionDTO [inputs={}, id=action1, type=type1, label=Label1, description=Description1, config={}]",
                action1.toString());
    }
}
