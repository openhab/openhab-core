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
package org.openhab.core.io.rest.auth.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.security.MessageDigest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jose4j.base64url.Base64Url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.PendingToken;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UserSession;

/**
 * Tests for {@link TokenResource}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TokenResourceTest {

    private @NonNullByDefault({}) TokenResource tokenResource;

    private @Mock @NonNullByDefault({}) UserRegistry userRegistryMock;
    private @Mock @NonNullByDefault({}) JwtHelper jwtHelperMock;
    private @Mock @NonNullByDefault({}) SecurityContext securityContextMock;

    @BeforeEach
    public void setup() {
        tokenResource = new TokenResource(userRegistryMock, jwtHelperMock);
        when(jwtHelperMock.getJwtAccessToken(any(), any(), any(), eq(TokenResource.TOKEN_LIFETIME)))
                .thenReturn("mock.jwt.token");
    }

    // --- Helper methods ---

    /**
     * Calls TokenResource.getToken with proper null handling for parameters that are nullable at runtime
     * but declared as @NonNull due to class-level @NonNullByDefault on TokenResource.
     */
    @NonNullByDefault({})
    private Response callGetToken(String grantType, String code, String redirectUri, String clientId,
            String refreshToken, String codeVerifier, boolean useCookie, Cookie sessionCookie) {
        return tokenResource.getToken(grantType, code, redirectUri, clientId, refreshToken, codeVerifier, useCookie,
                sessionCookie);
    }

    /**
     * Calls TokenResource.deleteSession with proper null handling.
     */
    @NonNullByDefault({})
    private Response callDeleteSession(String refreshToken, String id, Cookie sessionCookie,
            SecurityContext securityContext) {
        return tokenResource.deleteSession(refreshToken, id, sessionCookie, securityContext);
    }

    private ManagedUser createUserWithPendingToken(String username, String authCode, String clientId,
            String redirectUri, String scope) {
        return createUserWithPendingToken(username, authCode, clientId, redirectUri, scope, null, null);
    }

    @NonNullByDefault({})
    private ManagedUser createUserWithPendingToken(String username, String authCode, String clientId,
            String redirectUri, String scope, String codeChallenge, String codeChallengeMethod) {
        ManagedUser user = new ManagedUser(username, "salt", "hash");
        user.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        PendingToken pendingToken = new PendingToken(authCode, clientId, redirectUri, scope, codeChallenge,
                codeChallengeMethod);
        user.setPendingToken(pendingToken);
        return user;
    }

    private ManagedUser createUserWithSession(String username, String refreshToken, String sessionId, String clientId,
            String redirectUri, String scope) {
        ManagedUser user = new ManagedUser(username, "salt", "hash");
        user.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        UserSession session = new UserSession(sessionId, refreshToken, clientId, redirectUri, scope);
        user.setSessions(new ArrayList<>(List.of(session)));
        return user;
    }

    // --- Authorization Code Grant ---

    @Test
    public void authorizationCodeGrantHappyPath() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, null, false, null);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(TokenResponseDTO.class, response.getEntity());
        TokenResponseDTO dto = (TokenResponseDTO) response.getEntity();
        assertEquals("mock.jwt.token", dto.access_token);
        assertEquals("bearer", dto.token_type);
        assertEquals(TokenResource.TOKEN_LIFETIME * 60, dto.expires_in);
        assertNotNull(dto.refresh_token);
        assertEquals("admin", dto.scope);

        // Pending token should be cleared
        assertNull(user.getPendingToken());
        // Session should be added
        assertEquals(1, user.getSessions().size());
    }

    @Test
    public void authorizationCodeGrantWrongClientId() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "correct-client",
                "http://localhost/callback", "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "wrong-client",
                null, null, false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void authorizationCodeGrantWrongRedirectUri() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/wrong", "test-client", null,
                null, false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void authorizationCodeGrantInvalidCode() {
        when(userRegistryMock.getAll()).thenReturn(List.of());

        Response response = callGetToken("authorization_code", "invalid-code", "http://localhost/callback",
                "test-client", null, null, false, null);

        assertEquals(400, response.getStatus());
    }

    // --- PKCE Validation ---

    @Test
    public void pkceS256ValidationSucceeds() throws Exception {
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String codeChallenge = Base64Url.encode(sha256.digest(codeVerifier.getBytes()));

        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin", codeChallenge, "S256");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, codeVerifier, false, null);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void pkceS256ValidationFailsWithWrongVerifier() throws Exception {
        String correctVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String codeChallenge = Base64Url.encode(sha256.digest(correctVerifier.getBytes()));

        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin", codeChallenge, "S256");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, "wrong-verifier", false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void pkcePlainValidationSucceeds() {
        String codeVerifier = "plain-code-challenge";

        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin", codeVerifier, "plain");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, codeVerifier, false, null);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void pkcePlainValidationFailsWithWrongVerifier() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin", "correct-challenge", "plain");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, "wrong-verifier", false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void pkceMissingVerifierWhenChallengePresent() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin", "some-challenge", "S256");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        // codeVerifier is null
        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, null, false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void noPkceSkipsValidation() {
        String authCode = UUID.randomUUID().toString();
        // No code challenge in pending token (both null)
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/callback",
                "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/callback", "test-client",
                null, null, false, null);

        assertEquals(200, response.getStatus());
    }

    // --- Refresh Token Grant ---

    @Test
    public void refreshTokenGrantHappyPath() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("refresh_token", null, null, "test-client", refreshToken, null, false, null);

        assertEquals(200, response.getStatus());
        assertInstanceOf(TokenResponseDTO.class, response.getEntity());
        TokenResponseDTO dto = (TokenResponseDTO) response.getEntity();
        assertEquals("mock.jwt.token", dto.access_token);
        assertEquals(refreshToken, dto.refresh_token);
    }

    @Test
    public void refreshTokenGrantMissingRefreshToken() {
        Response response = callGetToken("refresh_token", null, null, "test-client", null, null, false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void refreshTokenGrantInvalidRefreshToken() {
        when(userRegistryMock.getAll()).thenReturn(List.of());

        Response response = callGetToken("refresh_token", null, null, "test-client", "invalid-refresh-token", null,
                false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void refreshTokenGrantWithSessionCookieValidation() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        // Mark session as requiring cookie
        user.getSessions().get(0).setSessionCookie(true);
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        // Provide matching cookie
        Cookie sessionCookie = new Cookie(TokenResource.SESSIONID_COOKIE_NAME, sessionId);

        Response response = callGetToken("refresh_token", null, null, "test-client", refreshToken, null, false,
                sessionCookie);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void refreshTokenGrantWithMissingSessionCookieFails() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        // Mark session as requiring cookie
        user.getSessions().get(0).setSessionCookie(true);
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        // No cookie provided
        Response response = callGetToken("refresh_token", null, null, "test-client", refreshToken, null, false, null);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void refreshTokenGrantWithWrongSessionCookieFails() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        user.getSessions().get(0).setSessionCookie(true);
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        // Wrong cookie value
        Cookie wrongCookie = new Cookie(TokenResource.SESSIONID_COOKIE_NAME, "wrong-session-id");

        Response response = callGetToken("refresh_token", null, null, "test-client", refreshToken, null, false,
                wrongCookie);

        assertEquals(400, response.getStatus());
    }

    // --- Unsupported Grant Type ---

    @Test
    public void unsupportedGrantTypeReturns400() {
        Response response = callGetToken("client_credentials", null, null, "test-client", null, null, false, null);

        assertEquals(400, response.getStatus());
    }

    // --- Sessions Endpoint ---

    @Test
    public void getSessionsUnauthenticatedReturns401() {
        when(securityContextMock.getUserPrincipal()).thenReturn(null);

        Response response = tokenResource.getSessions(securityContextMock);

        assertEquals(401, response.getStatus());
    }

    @Test
    public void getSessionsUserNotFoundReturns404() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("nonexistent");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        when(userRegistryMock.get("nonexistent")).thenReturn(null);

        Response response = tokenResource.getSessions(securityContextMock);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void getSessionsForAuthenticatedUserReturns200() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        UserSession session = new UserSession(UUID.randomUUID().toString(), "rt", "cid", "http://localhost/callback",
                "admin");
        user.setSessions(new ArrayList<>(List.of(session)));
        when(userRegistryMock.get("testuser")).thenReturn(user);

        Response response = tokenResource.getSessions(securityContextMock);

        assertEquals(200, response.getStatus());
    }

    // --- API Tokens Endpoint ---

    @Test
    public void getApiTokensUnauthenticatedReturns401() {
        when(securityContextMock.getUserPrincipal()).thenReturn(null);

        Response response = tokenResource.getApiTokens(securityContextMock);

        assertEquals(401, response.getStatus());
    }

    @Test
    public void getApiTokensUserNotFoundReturns404() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("nonexistent");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        when(userRegistryMock.get("nonexistent")).thenReturn(null);

        Response response = tokenResource.getApiTokens(securityContextMock);

        assertEquals(404, response.getStatus());
    }

    // --- Remove API Token Endpoint ---

    @Test
    public void removeApiTokenUnauthenticatedReturns401() {
        when(securityContextMock.getUserPrincipal()).thenReturn(null);

        Response response = tokenResource.removeApiToken(securityContextMock, "tokenname");

        assertEquals(401, response.getStatus());
    }

    @Test
    public void removeApiTokenNotFoundReturns404() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        when(userRegistryMock.get("testuser")).thenReturn(user);

        Response response = tokenResource.removeApiToken(securityContextMock, "nonexistent");

        assertEquals(404, response.getStatus());
    }

    // --- Logout Endpoint ---

    @Test
    public void logoutByRefreshTokenSucceeds() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        when(userRegistryMock.get("testuser")).thenReturn(user);

        Response response = callDeleteSession(refreshToken, null, null, securityContextMock);

        assertEquals(200, response.getStatus());
        verify(userRegistryMock).removeUserSession(eq(user), any(UserSession.class));
    }

    @Test
    public void logoutBySessionIdSucceeds() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        when(userRegistryMock.get("testuser")).thenReturn(user);

        // The id parameter matches the prefix of the session ID (before first '-')
        String sessionPrefix = sessionId.split("-")[0];
        Response response = callDeleteSession(null, sessionPrefix, null, securityContextMock);

        assertEquals(200, response.getStatus());
        verify(userRegistryMock).removeUserSession(eq(user), any(UserSession.class));
    }

    @Test
    public void logoutUnauthenticatedReturns401() {
        when(securityContextMock.getUserPrincipal()).thenReturn(null);

        Response response = callDeleteSession("some-token", null, null, securityContextMock);

        assertEquals(401, response.getStatus());
    }

    @Test
    public void logoutSessionNotFoundReturns404() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        ManagedUser user = new ManagedUser("testuser", "salt", "hash");
        user.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        when(userRegistryMock.get("testuser")).thenReturn(user);

        Response response = callDeleteSession("nonexistent-token", null, null, securityContextMock);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void logoutWithMatchingSessionCookieReplacesIt() {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String sessionId = UUID.randomUUID().toString();
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(securityContextMock.getUserPrincipal()).thenReturn(principal);
        ManagedUser user = createUserWithSession("testuser", refreshToken, sessionId, "test-client",
                "http://localhost/callback", "admin");
        when(userRegistryMock.get("testuser")).thenReturn(user);

        Cookie sessionCookie = new Cookie(TokenResource.SESSIONID_COOKIE_NAME, sessionId);

        Response response = callDeleteSession(refreshToken, null, sessionCookie, securityContextMock);

        assertEquals(200, response.getStatus());
        // The cookie should be replaced with a new random value
        assertNotNull(response.getHeaders().get("Set-Cookie"));
    }

    // --- Session Cookie for Root Redirect URIs ---

    @Test
    public void authCodeGrantWithCookieForRootUri() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/",
                "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/", "test-client", null, null,
                true, null);

        assertEquals(200, response.getStatus());
        // Should have Set-Cookie header for root URI
        assertNotNull(response.getHeaders().get("Set-Cookie"));
        // Session should be marked with cookie flag
        assertTrue(user.getSessions().get(0).hasSessionCookie());
    }

    @Test
    public void authCodeGrantWithCookieForNonRootUriReturns400() {
        String authCode = UUID.randomUUID().toString();
        ManagedUser user = createUserWithPendingToken("testuser", authCode, "test-client", "http://localhost/some/path",
                "admin");
        when(userRegistryMock.getAll()).thenReturn(List.of(user));

        Response response = callGetToken("authorization_code", authCode, "http://localhost/some/path", "test-client",
                null, null, true, null);

        // Non-root redirect URI with useCookie should fail
        assertEquals(400, response.getStatus());
    }
}
