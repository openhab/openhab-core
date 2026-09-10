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
package org.openhab.core.io.console.internal.extension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.AllSelector;
import org.openhab.core.auth.ItemAction;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleRegistry;
import org.openhab.core.io.console.Console;

/**
 * Tests for {@link RoleConsoleCommandExtension}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class RoleConsoleCommandExtensionTest {

    private @Mock @NonNullByDefault({}) RoleRegistry roleRegistryMock;
    private @Mock @NonNullByDefault({}) Console consoleMock;
    private @NonNullByDefault({}) RoleConsoleCommandExtension command;

    @BeforeEach
    public void setup() {
        command = new RoleConsoleCommandExtension(roleRegistryMock);
    }

    // --- list ---

    @Test
    public void testListPrintsAllRoles() {
        RoleDefinition admin = new RoleDefinition("administrator", "Full system access", Set.of(), true);
        RoleDefinition user = new RoleDefinition("user", "Standard user",
                Set.of(new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.COMMAND)), true);
        when(roleRegistryMock.getAll()).thenReturn(List.of(admin, user));

        command.execute(new String[] { "list" }, consoleMock);

        verify(consoleMock).println(contains("administrator"));
        verify(consoleMock).println(contains("user (built-in)"));
        verify(consoleMock).println(contains("ITEM * COMMAND"));
    }

    // --- add ---

    @Test
    public void testAddCreatesCustomRole() {
        command.execute(new String[] { "add", "viewer", "Read-only viewer" }, consoleMock);

        verify(roleRegistryMock).add(argThat(role -> "viewer".equals(role.getUID())
                && "Read-only viewer".equals(role.getDescription()) && !role.isBuiltIn()));
        verify(consoleMock).println(contains("created"));
    }

    @Test
    public void testAddWithoutDescriptionUsesEmpty() {
        command.execute(new String[] { "add", "viewer" }, consoleMock);

        verify(roleRegistryMock)
                .add(argThat(role -> "viewer".equals(role.getUID()) && "".equals(role.getDescription())));
        verify(consoleMock).println(contains("created"));
    }

    @Test
    public void testAddWithMissingArgsPrintsUsage() {
        command.execute(new String[] { "add" }, consoleMock);

        verify(roleRegistryMock, never()).add(any());
        verify(consoleMock).printUsage(anyString());
    }

    @Test
    public void testAddWithExistingNamePrintsError() {
        doThrow(new IllegalArgumentException("exists")).when(roleRegistryMock).add(any());

        command.execute(new String[] { "add", "administrator" }, consoleMock);

        verify(consoleMock).println(contains("already exists"));
    }

    // --- remove ---

    @Test
    public void testRemoveCustomRole() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isDefault(customRole)).thenReturn(false);

        command.execute(new String[] { "remove", "viewer" }, consoleMock);

        verify(roleRegistryMock).remove(eq("viewer"));
        verify(consoleMock).println(contains("removed"));
    }

    @Test
    public void testRemoveBuiltInRolePrintsError() {
        RoleDefinition admin = new RoleDefinition("administrator", "Full system access", Set.of(), true);
        when(roleRegistryMock.get(eq("administrator"))).thenReturn(admin);
        when(roleRegistryMock.isDefault(admin)).thenReturn(true);

        command.execute(new String[] { "remove", "administrator" }, consoleMock);

        verify(roleRegistryMock, never()).remove(anyString());
        verify(consoleMock).println(contains("Cannot remove built-in"));
    }

    @Test
    public void testRemoveNonexistentRolePrintsError() {
        when(roleRegistryMock.get(eq("nonexistent"))).thenReturn(null);

        command.execute(new String[] { "remove", "nonexistent" }, consoleMock);

        verify(roleRegistryMock, never()).remove(anyString());
        verify(consoleMock).println("Role not found.");
    }

    // --- addPermission ---

    @Test
    public void testAddPermissionToEditableRole() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "addPermission", "viewer", "ITEM", "*", "READ" }, consoleMock);

        verify(roleRegistryMock).update(argThat(role -> role.getPermissions().size() == 1 && role.getPermissions()
                .contains(new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ))));
        verify(consoleMock).println(contains("Permission added"));
    }

    @Test
    public void testAddPermissionToBuiltInRolePrintsError() {
        RoleDefinition admin = new RoleDefinition("administrator", "Full system access", Set.of(), true);
        when(roleRegistryMock.get(eq("administrator"))).thenReturn(admin);
        when(roleRegistryMock.isEditable(admin)).thenReturn(false);

        command.execute(new String[] { "addPermission", "administrator", "ITEM", "*", "READ" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println(contains("Cannot modify built-in"));
    }

    @Test
    public void testAddPermissionWithInvalidResourceTypePrintsError() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "addPermission", "viewer", "INVALID", "*", "READ" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println(contains("Unknown resource type"));
    }

    @Test
    public void testAddPermissionResourceTypeCaseInsensitive() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "addPermission", "viewer", "item", "*", "read" }, consoleMock);

        verify(roleRegistryMock).update(argThat(role -> role.getPermissions()
                .contains(new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ))));
    }

    @Test
    public void testAddPermissionWithInvalidActionPrintsError() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "addPermission", "viewer", "ITEM", "*", "BOGUS" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println(contains("Invalid action"));
    }

    @Test
    public void testAddPermissionWithInvalidSelectorPrintsError() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "addPermission", "viewer", "ITEM", "invalid_no_colon", "READ" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println(contains("Invalid selector"));
    }

    @Test
    public void testAddPermissionToNonexistentRolePrintsError() {
        when(roleRegistryMock.get(eq("nonexistent"))).thenReturn(null);

        command.execute(new String[] { "addPermission", "nonexistent", "ITEM", "*", "READ" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println("Role not found.");
    }

    // --- removePermission ---

    @Test
    public void testRemovePermissionFromRole() {
        Permission existing = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(existing), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "removePermission", "viewer", "ITEM", "*", "READ" }, consoleMock);

        verify(roleRegistryMock).update(argThat(role -> role.getPermissions().isEmpty()));
        verify(consoleMock).println(contains("Permission removed"));
    }

    @Test
    public void testRemovePermissionFromBuiltInRolePrintsError() {
        RoleDefinition admin = new RoleDefinition("administrator", "Full system access", Set.of(), true);
        when(roleRegistryMock.get(eq("administrator"))).thenReturn(admin);
        when(roleRegistryMock.isEditable(admin)).thenReturn(false);

        command.execute(new String[] { "removePermission", "administrator", "ITEM", "*", "READ" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println(contains("Cannot modify built-in"));
    }

    @Test
    public void testRemoveNonexistentPermissionPrintsError() {
        RoleDefinition customRole = new RoleDefinition("viewer", "Read-only", Set.of(), false);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(customRole);
        when(roleRegistryMock.isEditable(customRole)).thenReturn(true);

        command.execute(new String[] { "removePermission", "viewer", "ITEM", "*", "READ" }, consoleMock);

        verify(roleRegistryMock, never()).update(any());
        verify(consoleMock).println(contains("Permission not found"));
    }

    // --- show ---

    @Test
    public void testShowRole() {
        Permission perm = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.COMMAND);
        RoleDefinition user = new RoleDefinition("user", "Standard user", Set.of(perm), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(user);

        command.execute(new String[] { "show", "user" }, consoleMock);

        verify(consoleMock).println(contains("user (built-in)"));
        verify(consoleMock).println(contains("ITEM * COMMAND"));
    }

    @Test
    public void testShowNonexistentRolePrintsError() {
        when(roleRegistryMock.get(eq("nonexistent"))).thenReturn(null);

        command.execute(new String[] { "show", "nonexistent" }, consoleMock);

        verify(consoleMock).println("Role not found.");
    }

    // --- edge cases ---

    @Test
    public void testNoArgsPrintsUsage() {
        command.execute(new String[] {}, consoleMock);

        verify(consoleMock, atLeastOnce()).printUsage(anyString());
    }

    @Test
    public void testUnknownSubcommandPrintsError() {
        command.execute(new String[] { "bogus" }, consoleMock);

        verify(consoleMock).println(contains("Unknown command"));
        verify(consoleMock, atLeastOnce()).printUsage(anyString());
    }
}
