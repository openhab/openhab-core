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
package org.openhab.core.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Permission} record, including compact constructor validation.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class PermissionTest {

    @Test
    public void testValidPermissionCreation() {
        Permission p = new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ);
        assertEquals(ResourceType.ITEM, p.resourceType());
        assertSame(AllSelector.INSTANCE, p.selector());
        assertEquals(ItemAction.READ, p.action());
    }

    @Test
    public void testMismatchedActionResourceTypeThrows() {
        // PageAction.READ is not valid for ResourceType.ITEM
        assertThrows(IllegalArgumentException.class,
                () -> new Permission(ResourceType.ITEM, AllSelector.INSTANCE, PageAction.READ));
    }

    @Test
    public void testMismatchedSitemapActionOnItemThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Permission(ResourceType.ITEM, AllSelector.INSTANCE, SitemapAction.READ));
    }

    @Test
    public void testMismatchedItemActionOnPageThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Permission(ResourceType.PAGE, AllSelector.INSTANCE, ItemAction.COMMAND));
    }

    @Test
    public void testRecordEquality() {
        Permission p1 = new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ);
        Permission p2 = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testRecordInequalityDifferentAction() {
        Permission p1 = new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ);
        Permission p2 = new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.COMMAND);
        assertNotEquals(p1, p2);
    }

    @Test
    public void testRecordInequalityDifferentSelector() {
        Permission p1 = new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ);
        Permission p2 = new Permission(ResourceType.ITEM, new ByIdSelector("item1"), ItemAction.READ);
        assertNotEquals(p1, p2);
    }

    @Test
    public void testRecordInequalityDifferentResourceType() {
        Permission p1 = new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ);
        Permission p2 = new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.READ);
        assertNotEquals(p1, p2);
    }

    @Test
    public void testAllResourceTypeActionCombinations() {
        // Verify all valid combinations don't throw
        assertDoesNotThrow(() -> new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ));
        assertDoesNotThrow(() -> new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.COMMAND));
        assertDoesNotThrow(() -> new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.EDIT));
        assertDoesNotThrow(() -> new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.READ));
        assertDoesNotThrow(() -> new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.EDIT));
        assertDoesNotThrow(() -> new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.MANAGE));
        assertDoesNotThrow(() -> new Permission(ResourceType.SITEMAP, AllSelector.INSTANCE, SitemapAction.READ));
    }

    @Test
    public void testPermissionWithByIdSelector() {
        Permission p = new Permission(ResourceType.ITEM, new ByIdSelector("Light1"), ItemAction.COMMAND);
        assertEquals("id:Light1", p.selector().expression());
        assertTrue(p.selector().matches("Light1"));
        assertFalse(p.selector().matches("Light2"));
    }

    @Test
    public void testPermissionWithEntityAwareSelector() {
        Permission p = new Permission(ResourceType.ITEM, new ByGroupSelector("gLights"), ItemAction.READ);
        assertEquals("group:gLights", p.selector().expression());
        // Entity-aware selectors return false from simple matches()
        assertFalse(p.selector().matches("anyItem"));
    }
}
