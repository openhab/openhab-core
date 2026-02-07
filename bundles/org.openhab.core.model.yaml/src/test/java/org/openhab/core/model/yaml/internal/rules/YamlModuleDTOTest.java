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
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.util.TriggerBuilder;

/**
 * The {@link YamlModuleDTOTest} contains tests for the {@link YamlModuleDTO} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class YamlModuleDTOTest {

    @Test
    public void testConstructor() {
        Trigger t = TriggerBuilder.create().withId("trigger1").withTypeUID("type1").build();
        YamlModuleDTO trigger = new YamlModuleDTO(t);
        assertThat(trigger, notNullValue());
        assertThat(trigger.id, is("trigger1"));
        assertThat(trigger.type, is("type1"));
    }

    @Test
    public void testEquals() throws IOException {
        Trigger t = TriggerBuilder.create().withId("trigger1").withTypeUID("type1").build();
        YamlModuleDTO trigger1 = new YamlModuleDTO(t);
        YamlModuleDTO trigger2 = new YamlModuleDTO();
        assertNotEquals(trigger1, trigger2);
        assertEquals(trigger1, trigger1);
        assertEquals(trigger1, new YamlModuleDTO(t));
        assertFalse(trigger2.equals(new Object()));
    }

    @Test
    public void testToString() {
        Trigger t = TriggerBuilder.create().withId("trigger1").withTypeUID("type1").build();
        YamlModuleDTO trigger1 = new YamlModuleDTO(t);
        YamlModuleDTO trigger2 = new YamlModuleDTO();
        assertEquals("YamlModuleDTO [id=trigger1, type=type1, config={}]", trigger1.toString());
        assertEquals("YamlModuleDTO []", trigger2.toString());
        trigger1.label = "Label1";
        trigger1.description = "Description1";
        assertEquals("YamlModuleDTO [id=trigger1, type=type1, label=Label1, description=Description1, config={}]",
                trigger1.toString());
    }
}
