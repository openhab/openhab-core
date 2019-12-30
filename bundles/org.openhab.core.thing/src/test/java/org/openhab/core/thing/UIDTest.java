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
package org.openhab.core.thing;

import org.junit.Test;

/**
 * @author Alex Tugarev - Initial contribution
 */
public class UIDTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCharacters() {
        new ThingUID("binding:type:id_with_invalidchar#");
    }

    @Test
    public void testValidUIDs() {
        new ThingUID("binding:type:id-1");
        new ThingUID("binding:type:id_1");
        new ThingUID("binding:type:ID");
        new ThingUID("00:type:ID");
    }

}
