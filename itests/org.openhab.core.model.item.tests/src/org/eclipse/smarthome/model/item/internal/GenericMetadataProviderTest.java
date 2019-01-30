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
package org.eclipse.smarthome.model.item.internal;

import static org.junit.Assert.*;

import java.util.Collection;

import org.eclipse.smarthome.core.items.Metadata;
import org.junit.Test;

/**
 * @author Simon Kaufmann - initial contribution and API
 */
public class GenericMetadataProviderTest {

    @Test
    public void testGetAll_empty() {
        GenericMetadataProvider provider = new GenericMetadataProvider();
        Collection<Metadata> res = provider.getAll();
        assertNotNull(res);
        assertEquals(0, res.size());
    }

    @Test
    public void testAddMetadata() {
        GenericMetadataProvider provider = new GenericMetadataProvider();
        provider.addMetadata("binding", "item", "value", null);
        Collection<Metadata> res = provider.getAll();
        assertEquals(1, res.size());
        assertEquals("value", res.iterator().next().getValue());
    }

    @Test
    public void testRemoveMetadata_nonExistentItem() {
        GenericMetadataProvider provider = new GenericMetadataProvider();
        provider.removeMetadata("nonExistentItem");
    }

    @Test
    public void testRemoveMetadata() {
        GenericMetadataProvider provider = new GenericMetadataProvider();
        provider.addMetadata("other", "item", "value", null);
        provider.addMetadata("binding", "item", "value", null);
        provider.addMetadata("binding", "other", "value", null);
        assertEquals(3, provider.getAll().size());

        provider.removeMetadata("item");
        assertEquals(1, provider.getAll().size());
    }

}
