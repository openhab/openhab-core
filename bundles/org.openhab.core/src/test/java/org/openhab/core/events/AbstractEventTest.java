/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.events;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * {@link AbstractEventTest} tests the utility methods in {@link org.openhab.core.events.AbstractEvent}.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class AbstractEventTest {
    @Test
    public void testBuildSource() throws Exception {
        assertEquals(AbstractEvent.buildSource("org.openhab.core.thing", null), "org.openhab.core.thing");
        assertEquals(AbstractEvent.buildSource("org.openhab.core.thing", "actor"), "org.openhab.core.thing$actor");
    }

    @Test
    public void testBuildDelegatedSource() throws Exception {
        assertEquals(AbstractEvent.buildDelegatedSource(null, "org.openhab.core.thing"), "org.openhab.core.thing");
        assertEquals(AbstractEvent.buildDelegatedSource("org.openhab.binding.matter", "org.openhab.core.thing"),
                "org.openhab.binding.matter=>org.openhab.core.thing");
        assertEquals(AbstractEvent.buildDelegatedSource(null, "org.openhab.core.thing", "actor"),
                "org.openhab.core.thing$actor");
        assertEquals(
                AbstractEvent.buildDelegatedSource("org.openhab.binding.matter", "org.openhab.core.thing", "actor"),
                "org.openhab.binding.matter=>org.openhab.core.thing$actor");
        assertEquals(AbstractEvent.buildDelegatedSource("org.openhab.binding.matter$originalActor",
                "org.openhab.core.thing", "actor"),
                "org.openhab.binding.matter$originalActor=>org.openhab.core.thing$actor");
    }
}
