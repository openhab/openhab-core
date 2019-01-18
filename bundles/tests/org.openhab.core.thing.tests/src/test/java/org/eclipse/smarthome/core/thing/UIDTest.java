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
package org.eclipse.smarthome.core.thing;

import org.junit.Test;

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
