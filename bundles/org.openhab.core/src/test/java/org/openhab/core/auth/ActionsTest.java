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
 * Tests for {@link Actions}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class ActionsTest {

    @Test
    public void testParseItemActions() {
        assertEquals(ItemAction.READ, Actions.parse(ResourceType.ITEM, "READ"));
        assertEquals(ItemAction.COMMAND, Actions.parse(ResourceType.ITEM, "COMMAND"));
        assertEquals(ItemAction.EDIT, Actions.parse(ResourceType.ITEM, "EDIT"));
    }

    @Test
    public void testParsePageActions() {
        assertEquals(PageAction.READ, Actions.parse(ResourceType.PAGE, "READ"));
        assertEquals(PageAction.EDIT, Actions.parse(ResourceType.PAGE, "EDIT"));
        assertEquals(PageAction.MANAGE, Actions.parse(ResourceType.PAGE, "MANAGE"));
    }

    @Test
    public void testParseSitemapActions() {
        assertEquals(SitemapAction.READ, Actions.parse(ResourceType.SITEMAP, "READ"));
    }

    @Test
    public void testParseCaseInsensitive() {
        assertEquals(ItemAction.READ, Actions.parse(ResourceType.ITEM, "read"));
        assertEquals(PageAction.EDIT, Actions.parse(ResourceType.PAGE, "edit"));
    }

    @Test
    public void testParseInvalidActionThrows() {
        assertThrows(IllegalArgumentException.class, () -> Actions.parse(ResourceType.ITEM, "BOGUS"));
    }

    @Test
    public void testParseActionWrongResourceTypeThrows() {
        // COMMAND is valid for ITEM but not for PAGE
        assertThrows(IllegalArgumentException.class, () -> Actions.parse(ResourceType.PAGE, "COMMAND"));
    }

    @Test
    public void testActionResourceType() {
        assertEquals(ResourceType.ITEM, ItemAction.READ.resourceType());
        assertEquals(ResourceType.PAGE, PageAction.READ.resourceType());
        assertEquals(ResourceType.SITEMAP, SitemapAction.READ.resourceType());
    }

    @Test
    public void testActionName() {
        assertEquals("READ", ItemAction.READ.name());
        assertEquals("COMMAND", ItemAction.COMMAND.name());
        assertEquals("MANAGE", PageAction.MANAGE.name());
    }
}
