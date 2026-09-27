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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the validation of the OAuth2 authorization request performed by the {@link AuthorizePageServlet}, which
 * decides where an authorization code may be delivered to.
 *
 * @author Holger Friedrich - Initial contribution
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
        assertDoesNotThrow(() -> {
            AuthorizePageServlet.validateAuthorizationRequest(params);
            AuthorizePageServlet.validateRedirectUriOrigin(req, redirectUri);
        });
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
     * redirect_uri and supply a code challenge of their own - so only the origin check refuses it.
     */
    @Test
    public void authorizationRequestRedirectingToAThirdPartyIsRejected() {
        HttpServletRequest req = request("http", "openhab.local", 8080);
        String redirectUri = "https://attacker.com";
        Map<String, String[]> params = params(redirectUri, redirectUri, "code", CHALLENGE, "S256");
        assertDoesNotThrow(() -> AuthorizePageServlet.validateAuthorizationRequest(params));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AuthorizePageServlet.validateRedirectUriOrigin(req, redirectUri));
        assertEquals("unauthorized_client", e.getMessage());
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
