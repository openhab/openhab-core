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
package org.openhab.core.io.rest.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.io.rest.auth.internal.JwtHelper;
import org.openhab.core.io.rest.auth.internal.JwtSecurityContext;
import org.openhab.core.io.rest.auth.internal.UserSecurityContext;

/**
 * The {@link AuthFilterTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 * @author Gabor Bicskei - Added regression tests for auth behavior
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthFilterTest {

    @InjectMocks
    private @NonNullByDefault({}) AuthFilter authFilter;

    // These mocks are inject into authFilter during setup
    public @Mock @NonNullByDefault({}) JwtHelper jwtHelperMock;
    public @Mock @NonNullByDefault({}) UserRegistry userRegistryMock;

    private @Mock @NonNullByDefault({}) ContainerRequestContext containerRequestContext;
    private @Mock @NonNullByDefault({}) HttpServletRequest servletRequest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(servletRequest.getRemoteAddr()).thenReturn("192.168.0.100");
    }

    @Test
    public void implicitUserRoleAllowsAccess() throws IOException {
        authFilter.activate(Map.of()); // implicit user role is true by default
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext).setSecurityContext(any());
    }

    @Test
    public void noImplicitUserRoleDeniesAccess() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext, never()).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkAllowsAccessIfForwardedHeaderMatches() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.1.0/24"));
        when(servletRequest.getHeader("x-forwarded-for")).thenReturn("192.168.1.100");
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkDeniesAccessIfForwardedHeaderDoesNotMatch() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.1.0/24"));
        when(servletRequest.getHeader("x-forwarded-for")).thenReturn("192.168.2.100");
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext, never()).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkAllowsAccessIfRemoteAddressMatches() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.0.0/24"));
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkDeniesAccessIfRemoteAddressDoesNotMatch() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.1.0/24"));
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext, never()).setSecurityContext(any());
    }

    // --- New regression tests below use a manually constructed AuthFilter to avoid mock reference issues ---

    private AuthFilter createAuthFilter() {
        AuthFilter filter = new AuthFilter(jwtHelperMock, userRegistryMock);
        return filter;
    }

    // --- JWT Bearer Token Auth ---

    @Test
    public void jwtBearerTokenSetsJwtSecurityContext() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        when(jwtHelperMock.verifyAndParseJwtAccessToken("valid.jwt.token")).thenReturn(auth);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(JwtSecurityContext.class, sc);
    }

    @Test
    public void invalidJwtBearerTokenThrowsAuthenticationException() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        when(jwtHelperMock.verifyAndParseJwtAccessToken("bad.jwt.token"))
                .thenThrow(new AuthenticationException("Invalid token"));
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer bad.jwt.token");

        assertThrows(AuthenticationException.class, () -> filter.getSecurityContext(servletRequest, false));
    }

    // --- API Token (oh.*) Auth ---

    @Test
    public void apiTokenSetsUserSecurityContext() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.ADMIN));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer oh.mytoken.secretvalue");

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(UserSecurityContext.class, sc);
        assertEquals("ApiToken", sc.getAuthenticationScheme());
    }

    @Test
    public void apiTokenUserNotFoundThrowsAuthenticationException() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(null);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer oh.mytoken.secretvalue");

        assertThrows(AuthenticationException.class, () -> filter.getSecurityContext(servletRequest, false));
    }

    // --- X-OPENHAB-TOKEN Header ---

    @Test
    public void altAuthHeaderWithJwtToken() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.USER);
        when(jwtHelperMock.verifyAndParseJwtAccessToken("jwt.from.header")).thenReturn(auth);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn("jwt.from.header");

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(JwtSecurityContext.class, sc);
    }

    @Test
    public void altAuthHeaderWithApiToken() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.ADMIN));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn("oh.mytoken.secretvalue");

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(UserSecurityContext.class, sc);
        assertEquals("ApiToken", sc.getAuthenticationScheme());
    }

    @Test
    public void altAuthHeaderTakesPrecedenceOverAuthorizationHeader() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.USER);
        when(jwtHelperMock.verifyAndParseJwtAccessToken("alt.header.token")).thenReturn(auth);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn("alt.header.token");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer other.jwt.token");

        filter.getSecurityContext(servletRequest, false);

        verify(jwtHelperMock).verifyAndParseJwtAccessToken("alt.header.token");
        verify(jwtHelperMock, never()).verifyAndParseJwtAccessToken("other.jwt.token");
    }

    // --- Basic Auth ---

    @Test
    public void basicAuthWithUsernamePasswordWhenAllowed() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_ALLOW_BASIC_AUTH, true));
        String credentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.ADMIN));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Basic " + credentials);

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(UserSecurityContext.class, sc);
        assertEquals("Basic", sc.getAuthenticationScheme());
    }

    @Test
    public void basicAuthWithUsernamePasswordWhenDisabledThrowsAuthenticationException() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_ALLOW_BASIC_AUTH, false));
        String credentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Basic " + credentials);

        assertThrows(AuthenticationException.class, () -> filter.getSecurityContext(servletRequest, false));
    }

    @Test
    public void basicAuthSinglePartTreatedAsBearerToken() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        String singlePartToken = Base64.getEncoder().encodeToString("some-jwt-token".getBytes(StandardCharsets.UTF_8));
        Authentication auth = new Authentication("testuser", Role.USER);
        when(jwtHelperMock.verifyAndParseJwtAccessToken("some-jwt-token")).thenReturn(auth);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Basic " + singlePartToken);

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(JwtSecurityContext.class, sc);
    }

    @Test
    public void basicAuthMoreThanTwoPartsThrowsAuthenticationException() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_ALLOW_BASIC_AUTH, true));
        String credentials = Base64.getEncoder().encodeToString("user:pass:extra".getBytes(StandardCharsets.UTF_8));
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Basic " + credentials);

        assertThrows(AuthenticationException.class, () -> filter.getSecurityContext(servletRequest, false));
    }

    // --- Query Token (accessToken parameter) ---

    @Test
    public void queryTokenAllowedWithBearerToken() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        Authentication auth = new Authentication("testuser", Role.USER);
        when(jwtHelperMock.verifyAndParseJwtAccessToken("jwt.query.token")).thenReturn(auth);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);
        when(servletRequest.getParameterMap()).thenReturn(Map.of("accessToken", new String[] { "jwt.query.token" }));

        SecurityContext sc = filter.getSecurityContext(servletRequest, true);

        assertNotNull(sc);
        assertInstanceOf(JwtSecurityContext.class, sc);
    }

    @Test
    public void queryTokenFallsBackToBasicAuthWhenBearerFails() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_ALLOW_BASIC_AUTH, true));
        String base64Credentials = Base64.getEncoder()
                .encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));
        when(jwtHelperMock.verifyAndParseJwtAccessToken(base64Credentials))
                .thenThrow(new AuthenticationException("not a JWT"));
        Authentication auth = new Authentication("testuser", Role.USER);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.USER));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);
        when(servletRequest.getParameterMap()).thenReturn(Map.of("accessToken", new String[] { base64Credentials }));

        SecurityContext sc = filter.getSecurityContext(servletRequest, true);

        assertNotNull(sc);
        assertInstanceOf(UserSecurityContext.class, sc);
    }

    @Test
    public void queryTokenNotUsedWhenAllowQueryTokenIsFalse() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);
        when(servletRequest.getParameterMap()).thenReturn(Map.of("accessToken", new String[] { "some.token" }));

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNull(sc);
    }

    // --- Credential Checking Order ---

    @Test
    public void noCredentialsNoImplicitRoleReturnsNull() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNull(sc);
    }

    @Test
    public void noCredentialsWithImplicitRoleReturnsAnonymousContext() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, true));
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);

        SecurityContext sc = filter.getSecurityContext(servletRequest, false);

        assertNotNull(sc);
        assertInstanceOf(AnonymousUserSecurityContext.class, sc);
    }

    // --- Basic Auth Caching ---

    @Test
    public void basicAuthCachedOnSecondCall() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_ALLOW_BASIC_AUTH, true));
        String credentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.ADMIN));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Basic " + credentials);

        filter.getSecurityContext(servletRequest, false);
        filter.getSecurityContext(servletRequest, false);

        verify(userRegistryMock, times(1)).authenticate(any(Credentials.class));
    }

    @Test
    public void basicAuthCacheDisabledWithZeroExpiration() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_ALLOW_BASIC_AUTH, true,
                AuthFilter.CONFIG_CACHE_EXPIRATION, 0L));
        String credentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.ADMIN));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);
        when(servletRequest.getHeader("X-OPENHAB-TOKEN")).thenReturn(null);
        when(servletRequest.getHeader("Authorization")).thenReturn("Basic " + credentials);

        filter.getSecurityContext(servletRequest, false);
        filter.getSecurityContext(servletRequest, false);

        verify(userRegistryMock, times(2)).authenticate(any(Credentials.class));
    }

    // --- getSecurityContext(String bearerToken) overload ---

    @Test
    public void getSecurityContextWithNullBearerTokenReturnsNull() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of());

        SecurityContext sc = filter.getSecurityContext((String) null);

        assertNull(sc);
    }

    @Test
    public void getSecurityContextWithJwtBearerToken() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of());
        Authentication auth = new Authentication("testuser", Role.USER);
        when(jwtHelperMock.verifyAndParseJwtAccessToken("my.jwt.token")).thenReturn(auth);

        SecurityContext sc = filter.getSecurityContext("my.jwt.token");

        assertNotNull(sc);
        assertInstanceOf(JwtSecurityContext.class, sc);
    }

    @Test
    public void getSecurityContextWithApiTokenBearerToken() throws Exception {
        AuthFilter filter = createAuthFilter();
        filter.activate(Map.of());
        Authentication auth = new Authentication("testuser", Role.ADMIN);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(Set.of(Role.ADMIN));
        when(userRegistryMock.authenticate(any(Credentials.class))).thenReturn(auth);
        when(userRegistryMock.get("testuser")).thenReturn(user);

        SecurityContext sc = filter.getSecurityContext("oh.mytoken.secretvalue");

        assertNotNull(sc);
        assertInstanceOf(UserSecurityContext.class, sc);
    }
}
