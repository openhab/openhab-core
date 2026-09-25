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
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiTokenCredentials;
import org.openhab.core.auth.UserSession;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Yannick Schaus - Initial contribution
 * @author Gabor Bicskei - Added regression tests for role and auth behavior
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class UserRegistryImplTest {

    @SuppressWarnings("rawtypes")
    private @Mock @NonNullByDefault({}) ServiceReference managedProviderRefMock;
    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) ManagedUserProvider managedProviderMock;

    private @NonNullByDefault({}) UserRegistryImpl registry;
    private @NonNullByDefault({}) ServiceListener providerTracker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        when(bundleContextMock.getService(same(managedProviderRefMock))).thenReturn(managedProviderMock);

        registry = new UserRegistryImpl(bundleContextMock, Map.of());
        registry.setManagedProvider(managedProviderMock);
        registry.waitForCompletedAsyncActivationTasks();

        ArgumentCaptor<ServiceListener> captor = ArgumentCaptor.forClass(ServiceListener.class);
        verify(bundleContextMock).addServiceListener(captor.capture(), any());
        providerTracker = captor.getValue();
        providerTracker.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, managedProviderRefMock));
    }

    @Test
    public void testGetEmpty() throws Exception {
        User res = registry.get("none");
        assertNull(res);
    }

    @Test
    public void testUserManagement() throws Exception {
        User user = registry.register("username", "password", Set.of("administrator"));
        registry.added(managedProviderMock, user);
        assertNotNull(user);
        registry.authenticate(new UsernamePasswordCredentials("username", "password"));
        registry.changePassword(user, "password2");
        registry.authenticate(new UsernamePasswordCredentials("username", "password2"));
        registry.remove(user.getName());
        registry.removed(managedProviderMock, user);
        user = registry.get("username");
        assertNull(user);
    }

    @Test
    public void testSessions() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("username", "password", Set.of("administrator"));
        registry.added(managedProviderMock, user);
        assertNotNull(user);
        UserSession session1 = new UserSession(UUID.randomUUID().toString(), "s1", "urn:test", "urn:test", "scope");
        UserSession session2 = new UserSession(UUID.randomUUID().toString(), "s2", "urn:test", "urn:test", "scope2");
        UserSession session3 = new UserSession(UUID.randomUUID().toString(), "s3", "urn:test", "urn:test", "scope3");
        registry.addUserSession(user, session1);
        registry.addUserSession(user, session2);
        registry.addUserSession(user, session3);
        assertEquals(3, user.getSessions().size());
        registry.removeUserSession(user, session3);
        assertEquals(2, user.getSessions().size());
        registry.clearSessions(user);
        assertEquals(0, user.getSessions().size());
    }

    @Test
    public void testApiTokens() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("username", "password", Set.of("administrator"));
        registry.added(managedProviderMock, user);
        assertNotNull(user);
        String token1 = registry.addUserApiToken(user, "token1", "scope1");
        String token2 = registry.addUserApiToken(user, "token2", "scope2");
        String token3 = registry.addUserApiToken(user, "token3", "scope3");
        assertEquals(3, user.getApiTokens().size());
        registry.authenticate(new UserApiTokenCredentials(token1));
        registry.authenticate(new UserApiTokenCredentials(token2));
        registry.authenticate(new UserApiTokenCredentials(token3));
        registry.removeUserApiToken(user,
                user.getApiTokens().stream().filter(t -> "token1".equals(t.getName())).findAny().get());
        registry.removeUserApiToken(user,
                user.getApiTokens().stream().filter(t -> "token2".equals(t.getName())).findAny().get());
        registry.removeUserApiToken(user,
                user.getApiTokens().stream().filter(t -> "token3".equals(t.getName())).findAny().get());
        assertEquals(0, user.getApiTokens().size());
    }

    // --- Custom Role Strings ---

    @Test
    public void customRoleStringsAccepted() throws Exception {
        // The registry does not validate role strings — any string is accepted
        User user = registry.register("roleuser", "password", Set.of("custom_role", "another_role"));
        registry.added(managedProviderMock, user);

        assertNotNull(user);
        assertTrue(user.getRoles().contains("custom_role"));
        assertTrue(user.getRoles().contains("another_role"));
    }

    @Test
    public void standardRolesAccepted() throws Exception {
        User user = registry.register("stduser", "password", Set.of(Role.ADMIN, Role.USER));
        registry.added(managedProviderMock, user);

        assertTrue(user.getRoles().contains(Role.ADMIN));
        assertTrue(user.getRoles().contains(Role.USER));
    }

    @Test
    public void rolesPreservedAfterAuthentication() throws Exception {
        Set<String> roles = Set.of(Role.ADMIN, "custom_role");
        User user = registry.register("roleuser", "password", roles);
        registry.added(managedProviderMock, user);

        Authentication auth = registry.authenticate(new UsernamePasswordCredentials("roleuser", "password"));

        assertEquals("roleuser", auth.getUsername());
        assertTrue(auth.getRoles().contains(Role.ADMIN));
        assertTrue(auth.getRoles().contains("custom_role"));
    }

    @Test
    public void rolesSurviveRoundTripViaSetRoles() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("roleuser", "password", Set.of(Role.ADMIN, Role.USER));
        registry.added(managedProviderMock, user);

        // Verify initial roles
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(Role.ADMIN));
        assertTrue(user.getRoles().contains(Role.USER));

        // Modify roles directly on ManagedUser (simulating addRole)
        user.getRoles().add("custom_role");
        assertEquals(3, user.getRoles().size());

        // Authenticate and verify all roles come back
        Authentication auth = registry.authenticate(new UsernamePasswordCredentials("roleuser", "password"));
        assertTrue(auth.getRoles().contains(Role.ADMIN));
        assertTrue(auth.getRoles().contains(Role.USER));
        assertTrue(auth.getRoles().contains("custom_role"));
    }

    @Test
    public void removeRoleFromUser() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("roleuser", "password",
                Set.of(Role.ADMIN, Role.USER, "extra_role"));
        registry.added(managedProviderMock, user);

        assertEquals(3, user.getRoles().size());

        // Remove a role
        user.getRoles().remove("extra_role");
        assertEquals(2, user.getRoles().size());
        assertFalse(user.getRoles().contains("extra_role"));

        // Verify removed role does not appear in authentication
        Authentication auth = registry.authenticate(new UsernamePasswordCredentials("roleuser", "password"));
        assertFalse(auth.getRoles().contains("extra_role"));
        assertTrue(auth.getRoles().contains(Role.ADMIN));
        assertTrue(auth.getRoles().contains(Role.USER));
    }

    @Test
    public void emptyRolesAccepted() throws Exception {
        User user = registry.register("noroleuser", "password", Set.of());
        registry.added(managedProviderMock, user);

        assertTrue(user.getRoles().isEmpty());

        Authentication auth = registry.authenticate(new UsernamePasswordCredentials("noroleuser", "password"));
        assertTrue(auth.getRoles().isEmpty());
    }

    // --- Authentication Error Cases ---

    @Test
    public void authenticateUnknownUserThrowsException() {
        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UsernamePasswordCredentials("nonexistent", "password")));
    }

    @Test
    public void authenticateWrongPasswordThrowsException() throws Exception {
        User user = registry.register("testuser", "correct_password", Set.of(Role.USER));
        registry.added(managedProviderMock, user);

        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UsernamePasswordCredentials("testuser", "wrong_password")));
    }

    @Test
    public void oldPasswordFailsAfterChangePassword() throws Exception {
        User user = registry.register("testuser", "oldpass", Set.of(Role.USER));
        registry.added(managedProviderMock, user);

        registry.authenticate(new UsernamePasswordCredentials("testuser", "oldpass"));
        registry.changePassword(user, "newpass");

        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UsernamePasswordCredentials("testuser", "oldpass")));

        // New password works
        registry.authenticate(new UsernamePasswordCredentials("testuser", "newpass"));
    }

    // --- API Token Format and Validation ---

    @Test
    public void apiTokenHasCorrectFormat() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("testuser", "password", Set.of(Role.USER));
        registry.added(managedProviderMock, user);

        String token = registry.addUserApiToken(user, "mytoken", "scope");

        // Format: oh.{name}.{random}
        assertTrue(token.startsWith("oh.mytoken."));
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        assertEquals("oh", parts[0]);
        assertEquals("mytoken", parts[1]);
        assertFalse(parts[2].isEmpty());
    }

    @Test
    public void apiTokenNonAlphanumericNameRejected() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("testuser", "password", Set.of(Role.USER));
        registry.added(managedProviderMock, user);

        assertThrows(IllegalArgumentException.class, () -> registry.addUserApiToken(user, "invalid-name", "scope"));
        assertThrows(IllegalArgumentException.class, () -> registry.addUserApiToken(user, "invalid_name", "scope"));
        assertThrows(IllegalArgumentException.class, () -> registry.addUserApiToken(user, "invalid name", "scope"));
    }

    @Test
    public void apiTokenInvalidFormatThrowsException() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("testuser", "password", Set.of(Role.USER));
        registry.added(managedProviderMock, user);

        // Wrong prefix
        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UserApiTokenCredentials("bad.token.value")));
        // Too few parts
        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UserApiTokenCredentials("oh.onlytwoparts")));
        // Too many parts
        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UserApiTokenCredentials("oh.token.value.extra")));
    }

    @Test
    public void apiTokenWithUnknownNameThrowsException() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("testuser", "password", Set.of(Role.USER));
        registry.added(managedProviderMock, user);
        registry.addUserApiToken(user, "realtoken", "scope");

        assertThrows(AuthenticationException.class,
                () -> registry.authenticate(new UserApiTokenCredentials("oh.faketoken.randomvalue")));
    }

    @Test
    public void apiTokenAuthenticationReturnsCorrectScope() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("testuser", "password", Set.of(Role.ADMIN));
        registry.added(managedProviderMock, user);

        String token = registry.addUserApiToken(user, "scopedtoken", "items:read");

        Authentication auth = registry.authenticate(new UserApiTokenCredentials(token));
        assertEquals("items:read", auth.getScope());
        assertEquals("testuser", auth.getUsername());
    }

    // --- Session Management Edge Cases ---

    @Test
    public void addSessionToNonAuthenticatedUserThrows() {
        // GenericUser does not implement AuthenticatedUser, so this should fail
        // We test with a mock User that is not AuthenticatedUser
        org.openhab.core.auth.GenericUser genericUser = new org.openhab.core.auth.GenericUser("testuser",
                Set.of(Role.USER));
        // We can't add genericUser to the registry easily, but we can test the method directly
        // by first adding it as a known user
        // Instead, test that the method validates the type
        UserSession session = new UserSession(UUID.randomUUID().toString(), "rt", "cid", "uri", "scope");
        assertThrows(IllegalArgumentException.class, () -> registry.addUserSession(genericUser, session));
    }

    @Test
    public void changePasswordOnNonManagedUserThrows() {
        org.openhab.core.auth.GenericUser genericUser = new org.openhab.core.auth.GenericUser("testuser",
                Set.of(Role.USER));
        assertThrows(IllegalArgumentException.class, () -> registry.changePassword(genericUser, "newpass"));
    }
}
