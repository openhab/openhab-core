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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleRegistry;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.io.console.Console;

/**
 * Tests for the addRole/removeRole subcommands in {@link UserConsoleCommandExtension}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class UserConsoleCommandExtensionTest {

    private @Mock @NonNullByDefault({}) UserRegistry userRegistryMock;
    private @Mock @NonNullByDefault({}) RoleRegistry roleRegistryMock;
    private @Mock @NonNullByDefault({}) Console consoleMock;
    private @NonNullByDefault({}) UserConsoleCommandExtension command;

    @BeforeEach
    public void setup() {
        command = new UserConsoleCommandExtension(userRegistryMock, roleRegistryMock);
    }

    // --- addRole ---

    @Test
    public void testAddRoleToUser() {
        ManagedUser user = new ManagedUser("alice", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of("user")));
        when(userRegistryMock.get(eq("alice"))).thenReturn(user);
        when(roleRegistryMock.get(eq("viewer"))).thenReturn(new RoleDefinition("viewer", "", Set.of(), false));

        command.execute(new String[] { "addRole", "alice", "viewer" }, consoleMock);

        verify(userRegistryMock).update(user);
        verify(consoleMock).println(contains("added to user"));
    }

    @Test
    public void testAddRoleWithUndefinedRolePrintsWarningButAdds() {
        ManagedUser user = new ManagedUser("alice", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of("user")));
        when(userRegistryMock.get(eq("alice"))).thenReturn(user);
        when(roleRegistryMock.get(eq("undefined_role"))).thenReturn(null);

        command.execute(new String[] { "addRole", "alice", "undefined_role" }, consoleMock);

        verify(consoleMock).println(contains("Warning"));
        verify(userRegistryMock).update(user);
        verify(consoleMock).println(contains("added to user"));
    }

    @Test
    public void testAddRoleUserAlreadyHasRolePrintsMessage() {
        ManagedUser user = new ManagedUser("alice", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of("user", "viewer")));
        when(userRegistryMock.get(eq("alice"))).thenReturn(user);

        command.execute(new String[] { "addRole", "alice", "viewer" }, consoleMock);

        verify(userRegistryMock, never()).update(user);
        verify(consoleMock).println(contains("already has role"));
    }

    @Test
    public void testAddRoleUserNotFoundPrintsError() {
        when(userRegistryMock.get(eq("nonexistent"))).thenReturn(null);

        command.execute(new String[] { "addRole", "nonexistent", "viewer" }, consoleMock);

        verify(userRegistryMock, never()).update(any());
        verify(consoleMock).println("User not found.");
    }

    @Test
    public void testAddRoleMissingArgsPrintsUsage() {
        command.execute(new String[] { "addRole", "alice" }, consoleMock);

        verify(consoleMock).printUsage(anyString());
    }

    // --- removeRole ---

    @Test
    public void testRemoveRoleFromUser() {
        ManagedUser user = new ManagedUser("alice", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of("user", "viewer")));
        when(userRegistryMock.get(eq("alice"))).thenReturn(user);

        command.execute(new String[] { "removeRole", "alice", "viewer" }, consoleMock);

        verify(userRegistryMock).update(user);
        verify(consoleMock).println(contains("removed from user"));
    }

    @Test
    public void testRemoveRoleUserDoesNotHaveRolePrintsError() {
        ManagedUser user = new ManagedUser("alice", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of("user")));
        when(userRegistryMock.get(eq("alice"))).thenReturn(user);

        command.execute(new String[] { "removeRole", "alice", "viewer" }, consoleMock);

        verify(userRegistryMock, never()).update(user);
        verify(consoleMock).println(contains("does not have role"));
    }

    @Test
    public void testRemoveRoleUserNotFoundPrintsError() {
        when(userRegistryMock.get(eq("nonexistent"))).thenReturn(null);

        command.execute(new String[] { "removeRole", "nonexistent", "viewer" }, consoleMock);

        verify(userRegistryMock, never()).update(any());
        verify(consoleMock).println("User not found.");
    }

    @Test
    public void testRemoveRoleMissingArgsPrintsUsage() {
        command.execute(new String[] { "removeRole" }, consoleMock);

        verify(consoleMock).printUsage(anyString());
    }
}
