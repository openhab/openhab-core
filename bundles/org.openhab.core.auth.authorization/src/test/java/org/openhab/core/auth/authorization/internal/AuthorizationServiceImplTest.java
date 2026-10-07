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
package org.openhab.core.auth.authorization.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.AllSelector;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.ItemAction;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.PermissionEvaluator;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleRegistry;

/**
 * Tests for {@link AuthorizationServiceImpl}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class AuthorizationServiceImplTest {

    private @Mock @NonNullByDefault({}) RoleRegistry roleRegistryMock;
    private @Mock @NonNullByDefault({}) PermissionEvaluator itemEvaluatorMock;

    private @NonNullByDefault({}) AuthorizationServiceImpl service;

    private static final Map<String, Object> ENABLED_CONFIG = Map.of("enabled", true);
    private static final Map<String, Object> DISABLED_CONFIG = Map.of("enabled", false);

    @BeforeEach
    public void setup() {
        service = new AuthorizationServiceImpl(roleRegistryMock);
    }

    private void addItemEvaluator() {
        when(itemEvaluatorMock.getResourceType()).thenReturn(ResourceType.ITEM);
        service.addPermissionEvaluator(itemEvaluatorMock);
    }

    // --- Disabled state ---

    @Test
    public void testDisabledByDefault() {
        service.activate(DISABLED_CONFIG);
        assertFalse(service.isEnabled());
    }

    @Test
    public void testDisabledHasPermissionReturnsTrue() {
        service.activate(DISABLED_CONFIG);
        Authentication auth = new Authentication("alice", Role.USER);
        assertTrue(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));
    }

    @Test
    public void testDisabledFilterAuthorizedReturnsSameInstance() {
        service.activate(DISABLED_CONFIG);
        Authentication auth = new Authentication("alice", Role.USER);
        List<String> items = List.of("item1", "item2", "item3");
        Collection<String> result = service.filterAuthorized(auth, items, ResourceType.ITEM, ItemAction.READ, s -> s);
        assertSame(items, result);
    }

    // --- Enabled state ---

    @Test
    public void testEnabled() {
        service.activate(ENABLED_CONFIG);
        assertTrue(service.isEnabled());
    }

    @Test
    public void testAdminShortCircuitsToAllowAll() {
        service.activate(ENABLED_CONFIG);
        Authentication adminAuth = new Authentication("admin", Role.ADMIN);
        assertTrue(service.hasPermission(adminAuth, ResourceType.ITEM, "AnyItem", ItemAction.READ));
    }

    @Test
    public void testAdminFilterAuthorizedReturnsSameInstance() {
        service.activate(ENABLED_CONFIG);
        Authentication adminAuth = new Authentication("admin", Role.ADMIN);
        List<String> items = List.of("item1", "item2");
        Collection<String> result = service.filterAuthorized(adminAuth, items, ResourceType.ITEM, ItemAction.READ,
                s -> s);
        assertSame(items, result);
    }

    @Test
    public void testUserWithMatchingPermissionIsAllowed() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);
        when(itemEvaluatorMock.evaluate(eq(itemPermission), eq("LivingRoom_Light"), eq(ItemAction.READ)))
                .thenReturn(true);

        Authentication auth = new Authentication("alice", Role.USER);
        assertTrue(service.hasPermission(auth, ResourceType.ITEM, "LivingRoom_Light", ItemAction.READ));
    }

    @Test
    public void testUserWithoutMatchingPermissionIsDenied() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);
        when(itemEvaluatorMock.evaluate(eq(itemPermission), eq("LivingRoom_Light"), eq(ItemAction.COMMAND)))
                .thenReturn(false);

        Authentication auth = new Authentication("alice", Role.USER);
        assertFalse(service.hasPermission(auth, ResourceType.ITEM, "LivingRoom_Light", ItemAction.COMMAND));
    }

    @Test
    public void testUnknownRoleIsDenied() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        when(roleRegistryMock.get(eq("unknown_role"))).thenReturn(null);

        Authentication auth = new Authentication("alice", "unknown_role");
        assertFalse(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));
    }

    @Test
    public void testNoEvaluatorRegisteredIsDenied() {
        service.activate(ENABLED_CONFIG);

        Authentication auth = new Authentication("alice", Role.USER);
        assertFalse(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));
    }

    @Test
    public void testFilterAuthorizedReturnsOnlyPermittedResources() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);
        when(itemEvaluatorMock.evaluate(eq(itemPermission), eq("allowed"), eq(ItemAction.READ))).thenReturn(true);
        when(itemEvaluatorMock.evaluate(eq(itemPermission), eq("denied"), eq(ItemAction.READ))).thenReturn(false);

        Authentication auth = new Authentication("alice", Role.USER);
        List<String> items = List.of("allowed", "denied");
        Collection<String> result = service.filterAuthorized(auth, items, ResourceType.ITEM, ItemAction.READ, s -> s);
        assertEquals(List.of("allowed"), List.copyOf(result));
    }

    // --- Cache behavior ---

    @Test
    public void testCacheAvoidsRepeatedEvaluation() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);
        when(itemEvaluatorMock.evaluate(any(), any(), any())).thenReturn(true);

        Authentication auth = new Authentication("alice", Role.USER);
        service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ);
        service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ);

        verify(itemEvaluatorMock, times(1)).evaluate(any(), eq("MyItem"), eq(ItemAction.READ));
    }

    @Test
    public void testModifiedClearsCache() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);
        when(itemEvaluatorMock.evaluate(any(), any(), any())).thenReturn(true);

        Authentication auth = new Authentication("alice", Role.USER);
        service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ);

        service.modified(ENABLED_CONFIG);

        service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ);

        verify(itemEvaluatorMock, times(2)).evaluate(any(), eq("MyItem"), eq(ItemAction.READ));
    }

    // --- Evaluator wiring ---

    @Test
    public void testAddEvaluatorClearsCache() {
        service.activate(ENABLED_CONFIG);

        PermissionEvaluator firstEvaluator = mock(PermissionEvaluator.class);
        when(firstEvaluator.getResourceType()).thenReturn(ResourceType.ITEM);
        when(firstEvaluator.evaluate(any(), any(), any())).thenReturn(true);
        service.addPermissionEvaluator(firstEvaluator);

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);

        Authentication auth = new Authentication("alice", Role.USER);
        assertTrue(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));

        PermissionEvaluator secondEvaluator = mock(PermissionEvaluator.class);
        when(secondEvaluator.getResourceType()).thenReturn(ResourceType.ITEM);
        when(secondEvaluator.evaluate(any(), any(), any())).thenReturn(false);
        service.addPermissionEvaluator(secondEvaluator);

        assertFalse(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));
    }

    @Test
    public void testRemoveEvaluatorClearsCache() {
        service.activate(ENABLED_CONFIG);
        addItemEvaluator();

        Permission itemPermission = new Permission(ResourceType.ITEM, new AllSelector(), ItemAction.READ);
        RoleDefinition userRole = new RoleDefinition("user", "Standard user", Set.of(itemPermission), true);
        when(roleRegistryMock.get(eq("user"))).thenReturn(userRole);
        when(itemEvaluatorMock.evaluate(any(), any(), any())).thenReturn(true);

        Authentication auth = new Authentication("alice", Role.USER);
        assertTrue(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));

        service.removePermissionEvaluator(itemEvaluatorMock);

        assertFalse(service.hasPermission(auth, ResourceType.ITEM, "MyItem", ItemAction.READ));
    }

    // --- Lifecycle ---

    @Test
    public void testActivateRegistersRegistryChangeListener() {
        service.activate(DISABLED_CONFIG);
        verify(roleRegistryMock).addRegistryChangeListener(any());
    }

    @Test
    public void testDeactivateUnregistersRegistryChangeListener() {
        service.activate(DISABLED_CONFIG);
        service.deactivate();
        verify(roleRegistryMock).removeRegistryChangeListener(any());
    }
}
