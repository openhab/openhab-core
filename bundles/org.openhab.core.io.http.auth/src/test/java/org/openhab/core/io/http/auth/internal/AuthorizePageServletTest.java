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
package org.openhab.core.io.http.auth.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests the validation of the OAuth2 authorization request performed by the {@link AuthorizePageServlet}, which
 * decides where an authorization code may be delivered to.
 *
 * @author Holger Friedrich - Initial contribution
 * @author Dan Cunningham - Confirmation of redirect URIs on other origins
 */
@NonNullByDefault
public class AuthorizePageServletTest {

    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    /**
     * Builds a request as it would be served by openHAB reachable under the given origin.
     */
    private static HttpServletRequest request(String scheme, String serverName, int serverPort) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getScheme()).thenReturn(scheme);
        when(req.getServerName()).thenReturn(serverName);
        when(req.getServerPort()).thenReturn(serverPort);
        return req;
    }

    /**
     * The Main UI sends its own {@code window.location.origin} as redirect_uri, so the origin the request was served
     * on must be accepted - whatever host name, address or scheme openHAB happens to be reached under. A loopback
     * origin is nothing special here: it is accepted because it is the origin, not because it is loopback.
     */
    @ParameterizedTest
    @CsvSource({ //
            "http,  openhab.local, 8080, http://openhab.local:8080", //
            "http,  openhab,       8080, http://openhab:8080", //
            "http,  192.168.1.50,  8080, http://192.168.1.50:8080", //
            "https, openhab.local, 8443, https://openhab.local:8443", //
            "https, 192.168.1.50,  8443, https://192.168.1.50:8443", //
            // reaching openHAB on the machine it runs on, over either scheme
            "http,  localhost,     8080, http://localhost:8080", //
            "https, localhost,     8443, https://localhost:8443", //
            "http,  127.0.0.1,     8080, http://127.0.0.1:8080", //
            // URI.getHost() returns IPv6 literals enclosed in brackets, and so must the comparison cope with them
            "http,  [::1],         8080, http://[::1]:8080", //
            // an origin on a default port carries no port at all, but has to match the port of the request
            "http,  openhab.local, 80,   http://openhab.local", //
            "https, openhab.local, 443,  https://openhab.local", //
            // neither the scheme nor the host of an origin is case sensitive
            "http,  openhab.local, 8080, http://OPENHAB.LOCAL:8080", //
            "http,  localhost,     8080, HTTP://LOCALHOST:8080" })
    public void redirectUriOfServingOriginIsAccepted(String scheme, String serverName, int serverPort,
            String redirectUri) {
        assertTrue(AuthorizePageServlet.isSafeRedirectUri(request(scheme, serverName, serverPort), redirectUri));
    }

    /**
     * Anything which is not the serving origin is refused: this is what stops an authorization code from being
     * handed to a third party. Note that PKCE cannot prevent this, as an attacker luring the user into the
     * authorization request supplies the code challenge and knows the matching verifier.
     */
    @ParameterizedTest
    @CsvSource({ //
            // the attack described in GHSA-3523-gpwr-558p
            "http,  openhab.local, 8080, https://attacker.com", //
            "http,  openhab.local, 8080, http://attacker.com", //
            // a different port, host or scheme is a different origin
            "http,  openhab.local, 8080, http://openhab.local:8081", //
            "http,  openhab.local, 8080, https://openhab.local:8080", //
            "http,  openhab.local, 8080, http://evil.openhab.local:8080", //
            "https, openhab.local, 8443, http://openhab.local:8443", //
            // a default port must not match an origin served elsewhere
            "http,  openhab.local, 8080, http://openhab.local", //
            // credentials in the authority can disguise the real host, so they are refused outright
            "http,  openhab.local, 8080, http://openhab.local:8080@attacker.com", //
            "http,  openhab.local, 8080, http://user@openhab.local:8080" })
    public void foreignRedirectUriIsRejected(String scheme, String serverName, int serverPort, String redirectUri) {
        assertFalse(AuthorizePageServlet.isSafeRedirectUri(request(scheme, serverName, serverPort), redirectUri));
    }

    /**
     * Builds a request reaching openHAB on {@code http://localhost:8080} through a reverse proxy.
     */
    private static HttpServletRequest proxiedRequest(@Nullable String hostHeader, @Nullable String forwardedProto,
            @Nullable String forwardedHost) {
        HttpServletRequest req = request("http", "localhost", 8080);
        when(req.getHeader("Host")).thenReturn(hostHeader);
        when(req.getHeader("X-Forwarded-Proto")).thenReturn(forwardedProto);
        when(req.getHeader("X-Forwarded-Host")).thenReturn(forwardedHost);
        return req;
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = { //
            // browser's Host plus X-Forwarded-Proto, as the openHAB Cloud connector sends
            "myopenhab.org,        https, null,              https://myopenhab.org", //
            "connect.myopenhab.org:443, https, null,          https://connect.myopenhab.org", //
            "openhab.example.com:8443,  https, null,          https://openhab.example.com:8443", //
            // Host rewritten to the upstream, browser host in X-Forwarded-Host
            "localhost:8080,        https, openhab.example.com, https://openhab.example.com", //
            "localhost:8080,        null,  openhab.example.com, http://openhab.example.com", //
            // only the first value counts
            "localhost:8080,        'https, http', 'openhab.example.com, localhost:8080', https://openhab.example.com" })
    public void redirectUriOfForwardedOriginIsAccepted(@Nullable String hostHeader, @Nullable String forwardedProto,
            @Nullable String forwardedHost, String redirectUri) {
        assertTrue(AuthorizePageServlet.isSafeRedirectUri(proxiedRequest(hostHeader, forwardedProto, forwardedHost),
                redirectUri));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = { //
            "myopenhab.org,         https, null,              https://attacker.com", //
            "myopenhab.org,         https, null,              http://myopenhab.org", //
            "myopenhab.org,         https, null,              https://myopenhab.org:8443", //
            "localhost:8080,        https, openhab.example.com, https://localhost:8080", //
            "localhost:8080,        https, openhab.example.com, https://attacker.com", //
            "null,                 https, null,              https://myopenhab.org", //
            "myopenhab.org,        'ht tps', null,           https://myopenhab.org" })
    public void foreignRedirectUriBehindProxyIsRejected(@Nullable String hostHeader, @Nullable String forwardedProto,
            @Nullable String forwardedHost, String redirectUri) {
        assertFalse(AuthorizePageServlet.isSafeRedirectUri(proxiedRequest(hostHeader, forwardedProto, forwardedHost),
                redirectUri));
    }

    /**
     * A loopback target is not accepted just for being loopback. No openHAB client asks for one: they either use the
     * origin they were served on, or do not use this flow at all. Accepting it regardless of the serving origin would
     * let a crafted request deliver an authorization code to any port on the machine running the browser, so the
     * earlier loopback allowlist is deliberately not reinstated.
     */
    @ParameterizedTest
    @ValueSource(strings = { "http://localhost", "http://localhost:8080", "https://localhost:8443",
            "http://127.0.0.1:8080", "http://127.0.0.1:9000", "http://127.0.1.1:8080", "http://[::1]:8080",
            "http://[0:0:0:0:0:0:0:1]:8080" })
    public void loopbackRedirectUriFromAnotherOriginIsRejected(String redirectUri) {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        assertFalse(AuthorizePageServlet.isSafeRedirectUri(req, redirectUri));
    }

    /**
     * A fragment would be carried over into the redirect, where it could hide the query openHAB appends. The origin
     * matches here, so the fragment is the only reason for the rejection.
     */
    @Test
    public void redirectUriWithFragmentIsRejected() {
        assertFalse(AuthorizePageServlet.isSafeRedirectUri(request("http", "openhab.local", 8080),
                "http://openhab.local:8080#fragment"));
        assertFalse(AuthorizePageServlet.isSafeRedirectUri(request("http", "localhost", 8080),
                "http://localhost:8080#fragment"));
    }

    /**
     * Anything which is not an absolute URI with a host cannot be checked and is therefore refused.
     */
    @ParameterizedTest
    @ValueSource(strings = { "", " ", "/auth", "openhab.local:8080", "javascript:alert(1)", "http://",
            "http://openhab.local:8080 evil", "ht tp://openhab.local" })
    public void malformedRedirectUriIsRejected(String redirectUri) {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        assertFalse(AuthorizePageServlet.isSafeRedirectUri(req, redirectUri));
    }

    @Test
    public void validAuthorizationRequestIsAccepted() {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        String redirectUri = "http://openhab.local:8080";
        Map<String, String[]> params = params(redirectUri, redirectUri, "code", CHALLENGE, "S256");
        assertDoesNotThrow(() -> AuthorizePageServlet.validateAuthorizationRequest(params));
        assertTrue(AuthorizePageServlet.isSafeRedirectUri(req, redirectUri));
    }

    /**
     * PKCE with S256 is mandatory, so a pending token can never be created without a code challenge to verify.
     */
    @ParameterizedTest
    @CsvSource(nullValues = "null", value = { //
            "null,      S256", // no challenge at all
            "challenge, null", // no method, so nothing would ever be verified
            "challenge, plain", // a downgrade to the plain transformation
            "challenge, s256", // the method is case sensitive
            "challenge, S512" })
    public void authorizationRequestWithoutPkceIsRejected(@Nullable String codeChallenge,
            @Nullable String codeChallengeMethod) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AuthorizePageServlet.validateAuthorizationRequest(params("http://openhab.local:8080",
                        "http://openhab.local:8080", "code", codeChallenge, codeChallengeMethod)));
        assertEquals("invalid_request", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "token", "id_token", "CODE", "" })
    public void authorizationRequestWithUnsupportedResponseTypeIsRejected(String responseType) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AuthorizePageServlet.validateAuthorizationRequest(params("http://openhab.local:8080",
                        "http://openhab.local:8080", responseType, CHALLENGE, "S256")));
        assertEquals("unsupported_response_type", e.getMessage());
    }

    @Test
    public void authorizationRequestWithMismatchingClientIdIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AuthorizePageServlet.validateAuthorizationRequest(
                        params("http://openhab.local:8080", "http://other.local:8080", "code", CHALLENGE, "S256")));
        assertEquals("unauthorized_client", e.getMessage());
    }

    /**
     * The attack of GHSA-3523-gpwr-558p. It passes every other check - an attacker can set client_id to their own
     * redirect_uri and supply a code challenge of their own - so the user has to confirm the external host by typing
     * it before any code is issued.
     */
    @Test
    public void authorizationRequestRedirectingToAThirdPartyNeedsConfirmation() {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        String redirectUri = "https://attacker.com";
        Map<String, String[]> params = params(redirectUri, redirectUri, "code", CHALLENGE, "S256");
        assertDoesNotThrow(() -> AuthorizePageServlet.validateAuthorizationRequest(params));

        assertEquals("attacker.com", servlet(true).externalHost(req, redirectUri));
    }

    @Test
    public void sameOriginRedirectNeedsNoConfirmation() {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        assertNull(servlet(true).externalHost(req, "http://openhab.local:8080"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://attacker.com/cb", "ftp://example.com", "https://user@claude.ai/cb",
            "https://claude.ai/cb#frag", "javascript:alert(1)", "/relative", "", "ht tp://x",
            // non-ASCII hosts could hide look-alike characters
            "https://cläude.ai/cb", "https://сlaude.ai/cb" })
    public void unusableExternalRedirectIsRejected(String redirectUri) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servlet(true).externalHost(request("http", "openhab.local", 8080), redirectUri));
        assertEquals("unauthorized_client", e.getMessage());
    }

    @Test
    public void overlongExternalRedirectIsRejected() {
        String redirectUri = "https://claude.ai/" + "a".repeat(AuthorizePageServlet.MAX_REDIRECT_URI_LENGTH);
        assertNull(AuthorizePageServlet.allowedExternalHost(redirectUri));
    }

    @ParameterizedTest
    @CsvSource({ "http://localhost:33418/callback, localhost", "http://127.0.0.1:6274/oauth/callback, 127.0.0.1",
            "http://[::1]:8080/cb, [::1]", "https://Claude.AI/api/mcp/auth_callback, claude.ai" })
    public void externalHostIsLowerCasedAndAllowsPlainHttpOnLoopbackOnly(String redirectUri, String host) {
        assertEquals(host, AuthorizePageServlet.allowedExternalHost(redirectUri));
    }

    @Test
    public void externalSitesCanBeDisabled() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servlet(false).externalHost(request("https", "myopenhab.org", 443), CLAUDE));
        assertEquals("unauthorized_client", e.getMessage());
    }

    @Test
    public void externalSitesAreAllowedWhenNotConfigured() {
        AuthorizePageServlet servlet = servlet(true);
        servlet.modified(null);
        assertEquals("claude.ai", servlet.externalHost(request("https", "myopenhab.org", 443), CLAUDE));
    }

    @Test
    public void externalRedirectPageShowsTheHostAndAnEmptyConfirmationField() throws Exception {
        HttpServletRequest req = request("https", "myopenhab.org", 443);
        Map<String, String[]> params = params(CLAUDE, CLAUDE, "code", CHALLENGE, "S256");
        // a crafted link can't prefill the confirmation
        params.put(AuthorizePageServlet.CONFIRM_HOST_PARAM, new String[] { "claude.ai" });
        when(req.getParameterMap()).thenReturn(params);
        StringWriter body = new StringWriter();
        HttpServletResponse resp = response(body);

        servlet(true).doGet(req, resp);

        String page = body.toString();
        assertTrue(page.contains("<div class=\"site\">claude.ai</div>"));
        assertTrue(page.contains(CLAUDE));
        assertTrue(page.contains("name=\"confirm_host\""));
        assertFalse(page.contains("value=\"claude.ai\""));
        verify(resp).setHeader("X-Frame-Options", "DENY");
        verify(resp).setHeader("Content-Security-Policy", "frame-ancestors 'none'");
    }

    @Test
    public void sameOriginPageHasNoConfirmationFieldEvenIfRequested() throws Exception {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        Map<String, String[]> params = params("http://openhab.local:8080", "http://openhab.local:8080", "code",
                CHALLENGE, "S256");
        params.put("external_host", new String[] { "evil" });
        when(req.getParameterMap()).thenReturn(params);
        StringWriter body = new StringWriter();

        servlet(true).doGet(req, response(body));

        assertFalse(body.toString().contains("confirm_host"));
        assertFalse(body.toString().contains("evil"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "claude.ai", " Claude.AI " })
    public void typingTheHostIssuesACode(String typedHost) throws Exception {
        givenUser("bob", Role.USER);
        HttpServletRequest req = loginRequest("bob", typedHost);
        req.getParameterMap().put("state", new String[] { "xyz" });
        StringWriter body = new StringWriter();
        HttpServletResponse resp = response(body);

        AuthorizePageServlet servlet = servlet(true);
        servlet.pageTemplate = "<head></head>{message}|{form_fields}";

        servlet.doPost(req, resp);

        // sent on from a page: a redirect after the form post would be blocked by the page's form-action
        verify(resp, never()).addHeader(eq("Location"), any());
        verify(resp).setHeader("Cache-Control", "no-store");
        String page = body.toString();
        assertTrue(page.contains("<meta http-equiv=\"refresh\" content=\"0;url=" + CLAUDE + "?code="));
        assertTrue(page.contains("<a href=\"" + CLAUDE + "?code="));
        assertTrue(page.contains("&amp;state=xyz"));
    }

    @Test
    public void wrongHostUsesUpTheCsrfToken() throws Exception {
        AuthorizePageServlet servlet = servlet(true);

        servlet.doPost(loginRequest("bob", "claude-ai.com"), response(new StringWriter()));

        assertFalse(servlet.csrfTokens.containsKey("csrf"));
    }

    @Test
    public void missingCsrfTokenIsAFailedLoginThatStillAsksForTheSite() throws Exception {
        AuthorizePageServlet servlet = servlet(true);
        servlet.csrfTokens.clear();
        StringWriter body = new StringWriter();

        servlet.doPost(loginRequest("bob", "claude-ai.com"), response(body));

        assertTrue(body.toString().contains("Please try again."));
        assertFalse(body.toString().contains("doesn't match"));
        assertTrue(body.toString().contains("<div class=\"site\">claude.ai</div>"));
        assertTrue(body.toString().contains("name=\"confirm_host\""));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "claude", "claude-ai.com", "claude.ai.example.com", "api.claude.ai" })
    public void wrongOrMissingHostIssuesNoCodeAndSkipsTheLogin(@Nullable String typedHost) throws Exception {
        givenUser("bob", Role.USER);
        StringWriter body = new StringWriter();
        HttpServletResponse resp = response(body);

        servlet(true).doPost(loginRequest("bob", typedHost), resp);

        verify(authProvider, never()).authenticate(any());
        verify(resp, never()).addHeader(eq("Location"), any());
        assertTrue(body.toString().contains("doesn't match"));
        assertTrue(body.toString().contains("name=\"confirm_host\""));
    }

    @Test
    public void injectedExternalHostDoesNotReplaceTheRealOne() throws Exception {
        givenUser("bob", Role.USER);
        HttpServletRequest req = loginRequest("bob", "evil.com");
        req.getParameterMap().put("external_host", new String[] { "evil.com" });
        HttpServletResponse resp = response(new StringWriter());

        servlet(true).doPost(req, resp);

        verify(resp, never()).addHeader(eq("Location"), any());
    }

    @Test
    public void disabledExternalSitesIssueNoCode() throws Exception {
        givenUser("bob", Role.USER);
        StringWriter body = new StringWriter();
        HttpServletResponse resp = response(body);

        servlet(false).doPost(loginRequest("bob", "claude.ai"), resp);

        verify(authProvider, never()).authenticate(any());
        verify(resp, never()).addHeader(eq("Location"), any());
        assertEquals("unauthorized_client", body.toString());
    }

    @Test
    public void externalSiteCannotOfferToCreateTheFirstAccount() throws Exception {
        AuthorizePageServlet servlet = servlet(true);
        when(userRegistry.getAll()).thenReturn(List.of());
        HttpServletRequest req = request("https", "myopenhab.org", 443);
        when(req.getParameterMap()).thenReturn(params(CLAUDE, CLAUDE, "code", CHALLENGE, "S256"));
        StringWriter body = new StringWriter();

        servlet.doGet(req, response(body));

        assertTrue(body.toString().contains("no user account yet"));
        assertFalse(body.toString().contains("confirm_host"));
    }

    @Test
    public void externalSiteCannotCreateTheFirstAccount() throws Exception {
        AuthorizePageServlet servlet = servlet(true);
        when(userRegistry.getAll()).thenReturn(List.of());
        HttpServletRequest req = loginRequest("admin", "claude.ai");
        req.getParameterMap().put("password_repeat", new String[] { "secret" });
        StringWriter body = new StringWriter();
        HttpServletResponse resp = response(body);

        servlet.doPost(req, resp);

        verify(userRegistry, never()).register(any(), any(), any());
        verify(resp, never()).addHeader(eq("Location"), any());
        assertEquals("access_denied", body.toString());
    }

    @Test
    public void failedLoginStillShowsTheExternalSite() throws Exception {
        when(authProvider.authenticate(any())).thenThrow(new AuthenticationException("wrong password"));
        StringWriter body = new StringWriter();

        servlet(true).doPost(loginRequest("bob", "claude.ai"), response(body));

        assertTrue(body.toString().contains("Please try again."));
        assertTrue(body.toString().contains("<div class=\"site\">claude.ai</div>"));
    }

    @Test
    public void reflectedValuesCannotInjectPagePlaceholders() throws Exception {
        HttpServletRequest req = request("https", "myopenhab.org", 443);
        Map<String, String[]> params = params(CLAUDE, CLAUDE, "code", CHALLENGE, "S256");
        params.put("state", new String[] { "{message}" });
        when(req.getParameterMap()).thenReturn(params);
        StringWriter body = new StringWriter();

        servlet(true).doGet(req, response(body));

        assertTrue(body.toString().contains("name=\"state\" value=\"&#123;message&#125;\""));
    }

    private static final String CLAUDE = "https://claude.ai/api/mcp/auth_callback";

    private final UserRegistry userRegistry = mock(UserRegistry.class);
    private final AuthenticationProvider authProvider = mock(AuthenticationProvider.class);

    private AuthorizePageServlet servlet(boolean allowExternalSites) {
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle()).thenReturn(mock(Bundle.class));
        LocaleProvider localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);
        when(userRegistry.getAll()).thenReturn(List.<User> of(new ManagedUser("someone", "salt", "hash")));
        AuthorizePageServlet servlet = new AuthorizePageServlet(bundleContext, userRegistry, authProvider,
                localeProvider,
                Map.<String, Object> of(AuthorizePageServlet.CONFIG_ALLOW_EXTERNAL_SITES, allowExternalSites));
        servlet.csrfTokens.put("csrf", Instant.now());
        // the real template isn't loaded in tests, so render just the message and the form fields
        servlet.pageTemplate = "{message}|{form_fields}";
        return servlet;
    }

    private void givenUser(String name, String role) throws Exception {
        ManagedUser user = new ManagedUser(name, "salt", "hash");
        user.setRoles(Set.of(role));
        when(userRegistry.get(name)).thenReturn(user);
        when(authProvider.authenticate(any())).thenReturn(new Authentication(name, role));
    }

    private static HttpServletRequest loginRequest(String username, @Nullable String typedHost) {
        HttpServletRequest req = request("https", "myopenhab.org", 443);
        Map<String, String[]> params = params(CLAUDE, CLAUDE, "code", CHALLENGE, "S256");
        params.put("username", new String[] { username });
        params.put("password", new String[] { "secret" });
        params.put("csrf_token", new String[] { "csrf" });
        if (typedHost != null) {
            params.put(AuthorizePageServlet.CONFIRM_HOST_PARAM, new String[] { typedHost });
        }
        when(req.getParameterMap()).thenReturn(params);
        when(req.getRemoteAddr()).thenReturn("192.0.2.1");
        return req;
    }

    private static HttpServletResponse response(StringWriter body) throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(body));
        return resp;
    }

    @ParameterizedTest
    @CsvSource({ "redirect_uri, invalid_request", "response_type, unsupported_response_type",
            "client_id, unauthorized_client", "scope, invalid_scope" })
    public void authorizationRequestWithMissingParameterIsRejected(String missingParam, String expectedMessage) {
        Map<String, String[]> params = params("http://openhab.local:8080", "http://openhab.local:8080", "code",
                CHALLENGE, "S256");
        params.remove(missingParam);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AuthorizePageServlet.validateAuthorizationRequest(params));
        assertEquals(expectedMessage, e.getMessage());
    }

    private static Map<String, String[]> params(String redirectUri, String clientId, String responseType,
            @Nullable String codeChallenge, @Nullable String codeChallengeMethod) {
        Map<String, String[]> params = new HashMap<>();
        params.put("redirect_uri", new String[] { redirectUri });
        params.put("client_id", new String[] { clientId });
        params.put("response_type", new String[] { responseType });
        params.put("scope", new String[] { "admin" });
        if (codeChallenge != null) {
            params.put("code_challenge", new String[] { codeChallenge });
        }
        if (codeChallengeMethod != null) {
            params.put("code_challenge_method", new String[] { codeChallengeMethod });
        }
        return params;
    }
}
