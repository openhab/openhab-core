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
package org.openhab.core.io.websocket.log;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.Role;
import org.osgi.service.log.LogReaderService;

import jakarta.ws.rs.core.SecurityContext;

/**
 * The {@link LogWebSocketAdapterTest} contains tests for the {@link LogWebSocketAdapter}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class LogWebSocketAdapterTest {

    private @Mock @NonNullByDefault({}) LogReaderService logReaderService;
    private @Mock @NonNullByDefault({}) JettyServerUpgradeRequest request;
    private @Mock @NonNullByDefault({}) JettyServerUpgradeResponse response;

    private @NonNullByDefault({}) LogWebSocketAdapter logWsAdapter;

    @BeforeEach
    public void setup() {
        logWsAdapter = new LogWebSocketAdapter(logReaderService);
    }

    @Test
    public void createWebSocketBlockedForUserRole() {
        SecurityContext userContext = mock(SecurityContext.class);
        when(userContext.isUserInRole(Role.ADMIN)).thenReturn(false);

        Object result = logWsAdapter.createWebSocket(request, response, userContext);
        assertNull(result);
    }

    @Test
    public void createWebSocketAllowedForAdminRole() {
        SecurityContext adminContext = mock(SecurityContext.class);
        when(adminContext.isUserInRole(Role.ADMIN)).thenReturn(true);

        Object result = logWsAdapter.createWebSocket(request, response, adminContext);
        assertNotNull(result);
    }
}
