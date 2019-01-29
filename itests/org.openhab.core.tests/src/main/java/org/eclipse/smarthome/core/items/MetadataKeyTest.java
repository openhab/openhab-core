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
package org.eclipse.smarthome.core.items;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Simon Kaufmann - initial contribution and API
 */
public class MetadataKeyTest {

    @Test
    public void testGetNamespace() {
        assertEquals("namespace", new MetadataKey("namespace", "itemName").getNamespace());
    }

    @Test
    public void testGetItemName() {
        assertEquals("itemName", new MetadataKey("namespace", "itemName").getItemName());
    }

}
