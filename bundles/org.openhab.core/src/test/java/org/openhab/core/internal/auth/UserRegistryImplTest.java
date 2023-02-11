/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import static org.mockito.ArgumentMatchers.*;
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
import org.openhab.core.auth.ManagedUser;
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
        assertEquals(user.getSessions().size(), 3);
        registry.removeUserSession(user, session3);
        assertEquals(user.getSessions().size(), 2);
        registry.clearSessions(user);
        assertEquals(user.getSessions().size(), 0);
    }

    @Test
    public void testApiTokens() throws Exception {
        ManagedUser user = (ManagedUser) registry.register("username", "password", Set.of("administrator"));
        registry.added(managedProviderMock, user);
        assertNotNull(user);
        String token1 = registry.addUserApiToken(user, "token1", "scope1");
        String token2 = registry.addUserApiToken(user, "token2", "scope2");
        String token3 = registry.addUserApiToken(user, "token3", "scope3");
        assertEquals(user.getApiTokens().size(), 3);
        registry.authenticate(new UserApiTokenCredentials(token1));
        registry.authenticate(new UserApiTokenCredentials(token2));
        registry.authenticate(new UserApiTokenCredentials(token3));
        registry.removeUserApiToken(user,
                user.getApiTokens().stream().filter(t -> t.getName().equals("token1")).findAny().get());
        registry.removeUserApiToken(user,
                user.getApiTokens().stream().filter(t -> t.getName().equals("token2")).findAny().get());
        registry.removeUserApiToken(user,
                user.getApiTokens().stream().filter(t -> t.getName().equals("token3")).findAny().get());
        assertEquals(user.getApiTokens().size(), 0);
    }
}
