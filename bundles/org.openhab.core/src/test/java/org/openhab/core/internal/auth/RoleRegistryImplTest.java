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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.AllSelector;
import org.openhab.core.auth.ItemAction;
import org.openhab.core.auth.PageAction;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.SitemapAction;
import org.openhab.core.service.ReadyService;

/**
 * Tests for {@link RoleRegistryImpl}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class RoleRegistryImplTest {

    private @Mock @NonNullByDefault({}) ManagedRoleProvider managedRoleProviderMock;
    private @Mock @NonNullByDefault({}) ReadyService readyServiceMock;
    private @NonNullByDefault({}) RoleRegistryImpl registry;

    @BeforeEach
    public void setup() {
        when(managedRoleProviderMock.getAll()).thenReturn(List.of());
        registry = new RoleRegistryImpl(new DefaultRoleProvider(), managedRoleProviderMock, readyServiceMock);
    }

    @Test
    public void testBuiltInRolesPresentOnStartup() {
        assertNotNull(registry.get("administrator"));
        assertNotNull(registry.get("user"));
        assertEquals(2, registry.getAll().size());
    }

    @Test
    public void testAdministratorRoleHasNoPermissions() {
        RoleDefinition admin = Objects.requireNonNull(registry.get("administrator"));
        assertEquals("administrator", admin.getUID());
        assertEquals("Full system access", admin.getDescription());
        assertTrue(admin.isBuiltIn());
        assertTrue(admin.getPermissions().isEmpty());
    }

    @Test
    public void testUserRoleHasExpectedPermissions() {
        RoleDefinition user = Objects.requireNonNull(registry.get("user"));
        assertEquals("user", user.getUID());
        assertTrue(user.isBuiltIn());
        Set<Permission> permissions = user.getPermissions();
        assertEquals(3, permissions.size());
        assertTrue(permissions.contains(new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.COMMAND)));
        assertTrue(permissions.contains(new Permission(ResourceType.PAGE, new AllSelector(), PageAction.READ)));
        assertTrue(permissions.contains(new Permission(ResourceType.SITEMAP, new AllSelector(), SitemapAction.READ)));
    }

    @Test
    public void testBuiltInRolesCannotBeRemoved() {
        assertThrows(IllegalArgumentException.class, () -> registry.remove("administrator"));
        assertThrows(IllegalArgumentException.class, () -> registry.remove("user"));
    }

    @Test
    public void testRemoveNonExistentKeyReturnsNull() {
        assertNull(registry.remove("nonexistent"));
    }

    @Test
    public void testIsDefaultForBuiltInRoles() {
        RoleDefinition admin = Objects.requireNonNull(registry.get("administrator"));
        RoleDefinition user = Objects.requireNonNull(registry.get("user"));
        assertTrue(registry.isDefault(admin));
        assertTrue(registry.isDefault(user));
    }

    @Test
    public void testIsDefaultForCustomRole() {
        RoleDefinition customRole = new RoleDefinition("custom", "Custom role", Set.of(), false);
        assertFalse(registry.isDefault(customRole));
    }

    @Test
    public void testIsEditableForBuiltInRoles() {
        when(managedRoleProviderMock.get(eq("administrator"))).thenReturn(null);
        when(managedRoleProviderMock.get(eq("user"))).thenReturn(null);

        RoleDefinition admin = Objects.requireNonNull(registry.get("administrator"));
        RoleDefinition user = Objects.requireNonNull(registry.get("user"));
        assertFalse(registry.isEditable(admin));
        assertFalse(registry.isEditable(user));
    }

    @Test
    public void testIsEditableForManagedRole() {
        RoleDefinition customRole = new RoleDefinition("custom", "Custom role", Set.of(), false);
        when(managedRoleProviderMock.get(eq("custom"))).thenReturn(customRole);

        assertTrue(registry.isEditable(customRole));
    }

    @Test
    public void testPermissionRecordBasics() {
        Permission permission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.COMMAND);
        assertEquals(ResourceType.ITEM, permission.resourceType());
        assertEquals("*", permission.selector().expression());
        assertEquals("COMMAND", permission.action().name());
    }

    @Test
    public void testPermissionRecordEquality() {
        Permission p1 = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.COMMAND);
        Permission p2 = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.COMMAND);
        Permission p3 = new Permission(ResourceType.PAGE, new AllSelector(), PageAction.READ);
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
    }

    @Test
    public void testPermissionValidatesActionResourceType() {
        assertThrows(IllegalArgumentException.class,
                () -> new Permission(ResourceType.ITEM, new AllSelector(), PageAction.READ));
    }

    @Test
    public void testResourceTypeValues() {
        assertEquals(3, ResourceType.values().length);
        assertNotNull(ResourceType.valueOf("ITEM"));
        assertNotNull(ResourceType.valueOf("PAGE"));
        assertNotNull(ResourceType.valueOf("SITEMAP"));
    }

    @Test
    public void testRoleDefinitionToString() {
        RoleDefinition builtIn = Objects.requireNonNull(registry.get("administrator"));
        assertEquals("administrator (built-in)", builtIn.toString());

        RoleDefinition custom = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        assertEquals("viewer (custom)", custom.toString());
    }

    @Test
    public void testAddCustomRoleDelegatesToManagedProvider() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only viewer", Set.of(), false);
        registry.add(customRole);
        verify(managedRoleProviderMock).add(customRole);
    }

    @Test
    public void testUpdateCustomRoleDelegatesToManagedProvider() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only viewer", Set.of(), false);
        registry.update(customRole);
        verify(managedRoleProviderMock).update(customRole);
    }

    @Test
    public void testAddedCustomRoleIsRetrievable() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only viewer",
                Set.of(new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ)), false);

        registry.added(managedRoleProviderMock, customRole);

        RoleDefinition retrieved = registry.get("viewer");
        assertNotNull(retrieved);
        assertEquals("viewer", retrieved.getUID());
        assertEquals("Read-only viewer", retrieved.getDescription());
        assertFalse(retrieved.isBuiltIn());
        assertEquals(1, retrieved.getPermissions().size());
    }

    @Test
    public void testRemovedCustomRoleIsNoLongerRetrievable() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only viewer", Set.of(), false);

        registry.added(managedRoleProviderMock, customRole);
        assertNotNull(registry.get("viewer"));

        registry.removed(managedRoleProviderMock, customRole);
        assertNull(registry.get("viewer"));
    }
}
