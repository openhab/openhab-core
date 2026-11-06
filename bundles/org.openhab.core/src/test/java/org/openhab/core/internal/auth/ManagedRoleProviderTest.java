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
package org.openhab.core.internal.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.AllSelector;
import org.openhab.core.auth.ByGroupSelector;
import org.openhab.core.auth.ByIdSelector;
import org.openhab.core.auth.ByLocationSelector;
import org.openhab.core.auth.ByTagSelector;
import org.openhab.core.auth.ItemAction;
import org.openhab.core.auth.PageAction;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.SitemapAction;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;

/**
 * Tests for {@link ManagedRoleProvider} DTO round-trip (toElement / toPersistableElement).
 *
 * @author Gabor Bicskei - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ManagedRoleProviderTest {

    private @Mock @NonNullByDefault({}) StorageService storageServiceMock;
    private @Mock @NonNullByDefault({}) Storage<Object> storageMock;
    private @NonNullByDefault({}) ManagedRoleProvider provider;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setup() {
        when(storageServiceMock.getStorage(eq("roles"), any())).thenReturn(storageMock);
        provider = new ManagedRoleProvider(storageServiceMock);
    }

    @Test
    public void testRoundTripWithAllSelectorAndItemAction() {
        RoleDefinition original = new RoleDefinition("viewer", "Read-only viewer",
                Set.of(new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ)), false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        RoleDefinition restored = provider.toElement("viewer", persisted);

        assertNotNull(restored);
        assertEquals("viewer", restored.getUID());
        assertEquals("Read-only viewer", restored.getDescription());
        assertFalse(restored.isBuiltIn());
        assertEquals(1, restored.getPermissions().size());
        assertTrue(restored.getPermissions()
                .contains(new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.READ)));
    }

    @Test
    public void testRoundTripWithByIdSelector() {
        RoleDefinition original = new RoleDefinition("custom", "Custom role",
                Set.of(new Permission(ResourceType.ITEM, new ByIdSelector("LivingRoom_Light"), ItemAction.COMMAND)),
                false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        RoleDefinition restored = provider.toElement("custom", persisted);

        assertNotNull(restored);
        assertEquals(1, restored.getPermissions().size());
        Permission perm = restored.getPermissions().iterator().next();
        assertInstanceOf(ByIdSelector.class, perm.selector());
        assertEquals("LivingRoom_Light", ((ByIdSelector) perm.selector()).id());
        assertEquals(ItemAction.COMMAND, perm.action());
    }

    @Test
    public void testRoundTripWithByGroupSelector() {
        RoleDefinition original = new RoleDefinition("custom", "Custom role",
                Set.of(new Permission(ResourceType.ITEM, new ByGroupSelector("gLights"), ItemAction.READ)), false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        RoleDefinition restored = provider.toElement("custom", persisted);

        assertNotNull(restored);
        Permission perm = restored.getPermissions().iterator().next();
        assertInstanceOf(ByGroupSelector.class, perm.selector());
        assertEquals("gLights", ((ByGroupSelector) perm.selector()).group());
    }

    @Test
    public void testRoundTripWithByTagSelector() {
        RoleDefinition original = new RoleDefinition("custom", "Custom role",
                Set.of(new Permission(ResourceType.ITEM, new ByTagSelector("Lighting"), ItemAction.READ)), false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        RoleDefinition restored = provider.toElement("custom", persisted);

        assertNotNull(restored);
        Permission perm = restored.getPermissions().iterator().next();
        assertInstanceOf(ByTagSelector.class, perm.selector());
        assertEquals("Lighting", ((ByTagSelector) perm.selector()).tag());
    }

    @Test
    public void testRoundTripWithByLocationSelector() {
        RoleDefinition original = new RoleDefinition("custom", "Custom role",
                Set.of(new Permission(ResourceType.ITEM, new ByLocationSelector("LivingRoom"), ItemAction.READ)),
                false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        RoleDefinition restored = provider.toElement("custom", persisted);

        assertNotNull(restored);
        Permission perm = restored.getPermissions().iterator().next();
        assertInstanceOf(ByLocationSelector.class, perm.selector());
        assertEquals("LivingRoom", ((ByLocationSelector) perm.selector()).location());
    }

    @Test
    public void testRoundTripWithMultiplePermissions() {
        RoleDefinition original = new RoleDefinition("user", "Standard user",
                Set.of(new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.COMMAND),
                        new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.READ),
                        new Permission(ResourceType.SITEMAP, AllSelector.INSTANCE, SitemapAction.READ)),
                true);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        assertEquals(3, persisted.permissions.size());

        RoleDefinition restored = provider.toElement("user", persisted);

        assertNotNull(restored);
        assertEquals("user", restored.getUID());
        assertTrue(restored.isBuiltIn());
        assertEquals(3, restored.getPermissions().size());
        assertTrue(restored.getPermissions()
                .contains(new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.COMMAND)));
        assertTrue(restored.getPermissions()
                .contains(new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.READ)));
        assertTrue(restored.getPermissions()
                .contains(new Permission(ResourceType.SITEMAP, AllSelector.INSTANCE, SitemapAction.READ)));
    }

    @Test
    public void testRoundTripWithEmptyPermissions() {
        RoleDefinition original = new RoleDefinition("empty", "No permissions", Set.of(), false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(original);
        assertEquals(0, persisted.permissions.size());

        RoleDefinition restored = provider.toElement("empty", persisted);

        assertNotNull(restored);
        assertTrue(restored.getPermissions().isEmpty());
    }

    @Test
    public void testToElementWithCorruptResourceTypeReturnsNull() {
        PersistedRoleDefinition persisted = new PersistedRoleDefinition("bad", "Bad role",
                java.util.List.of(new PersistedPermission("INVALID_TYPE", "*", "READ")), false);

        RoleDefinition result = provider.toElement("bad", persisted);

        assertNull(result);
    }

    @Test
    public void testToElementWithCorruptActionReturnsNull() {
        PersistedRoleDefinition persisted = new PersistedRoleDefinition("bad", "Bad role",
                java.util.List.of(new PersistedPermission("ITEM", "*", "BOGUS_ACTION")), false);

        RoleDefinition result = provider.toElement("bad", persisted);

        assertNull(result);
    }

    @Test
    public void testToElementWithCorruptSelectorReturnsNull() {
        PersistedRoleDefinition persisted = new PersistedRoleDefinition("bad", "Bad role",
                java.util.List.of(new PersistedPermission("ITEM", "invalid_no_colon", "READ")), false);

        RoleDefinition result = provider.toElement("bad", persisted);

        assertNull(result);
    }

    @Test
    public void testPersistedPermissionStringValues() {
        Permission permission = new Permission(ResourceType.ITEM, new ByGroupSelector("gLights"), ItemAction.COMMAND);
        RoleDefinition role = new RoleDefinition("test", "Test", Set.of(permission), false);

        PersistedRoleDefinition persisted = provider.toPersistableElement(role);

        assertEquals(1, persisted.permissions.size());
        PersistedPermission pp = persisted.permissions.get(0);
        assertEquals("ITEM", pp.resourceType);
        assertEquals("group:gLights", pp.selector);
        assertEquals("COMMAND", pp.action);
    }

    @Test
    public void testStorageName() {
        // Verify via the add method that it uses the correct storage
        verify(storageServiceMock).getStorage(eq("roles"), any());
    }
}
