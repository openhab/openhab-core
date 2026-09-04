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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openhab.core.OpenHAB;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.GenericUser;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;

/**
 * Tests for {@link JwtHelper}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class JwtHelperTest {

    private static final String ISSUER_NAME = "openhab";
    private static final String AUDIENCE = "openhab";

    private static @Nullable String previousUserDataProperty;

    private @NonNullByDefault({}) JwtHelper jwtHelper;

    /**
     * Isolate the userdata folder so {@link JwtHelper} writes its RSA key into the JUnit-managed temporary directory
     * instead of the working directory. This must run before {@link JwtHelper} is first referenced, because
     * {@code JwtHelper.KEY_FILE_PATH} is a {@code static final} field that resolves {@link OpenHAB#getUserDataFolder()}
     * exactly once at class-initialization time. The temp dir is injected as a parameter (rather than a static field)
     * so JUnit creates and cleans it up automatically.
     */
    @BeforeAll
    public static void setupClass(@TempDir Path tempUserData) {
        previousUserDataProperty = System.getProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT);
        System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, tempUserData.toString());
    }

    @AfterAll
    public static void teardownClass() {
        // Restore the previous property value, or clear it if it was not set before.
        String previous = previousUserDataProperty;
        if (previous != null) {
            System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, previous);
        } else {
            System.clearProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT);
        }
    }

    @BeforeEach
    public void setup() {
        // JwtHelper reads/writes its key from OpenHAB.getUserDataFolder()/secrets/rsa_json_web_key.json, which now
        // points into the isolated temporary directory set up in setupClass().
        jwtHelper = new JwtHelper();
    }

    /**
     * Returns the RSA key that the {@link JwtHelper} instance under test uses for signing and verification. The key is
     * read directly from the instance's private {@code jwtWebKey} field via reflection rather than from disk.
     * <p>
     * Reading from the instance (instead of re-reading the persisted key file) makes the test independent of where
     * {@code JwtHelper} persists its key: {@code JwtHelper.KEY_FILE_PATH} is a {@code static final} field resolved once
     * at class-initialization time, so it may not point at the isolated temp userdata folder if the {@code JwtHelper}
     * class was loaded before the {@code openhab.userdata} property was set. Using the live instance key avoids that
     * ordering dependency entirely while still guaranteeing the same key material is used — so crafted tokens are
     * rejected only for the reason under test (expiration or issuer), never for a signature mismatch.
     */
    private RsaJsonWebKey getJwtHelperKey() throws Exception {
        Field keyField = JwtHelper.class.getDeclaredField("jwtWebKey");
        keyField.setAccessible(true);
        return (RsaJsonWebKey) keyField.get(jwtHelper);
    }

    // --- Token Creation ---

    @Test
    public void tokenCreationReturnsNonEmptyString() {
        User user = new GenericUser("testuser", Set.of(Role.ADMIN));
        String token = jwtHelper.getJwtAccessToken(user, "test-client", "admin", 60);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void tokenHasThreeParts() {
        User user = new GenericUser("testuser", Set.of(Role.USER));
        String token = jwtHelper.getJwtAccessToken(user, "test-client", "user", 60);

        // JWT compact serialization has 3 dot-separated parts: header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    public void tokenClaimsContainCorrectSubject() throws Exception {
        User user = new GenericUser("admin_user", Set.of(Role.ADMIN));
        String token = jwtHelper.getJwtAccessToken(user, "my-client", "admin", 60);

        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);
        assertEquals("admin_user", auth.getUsername());
    }

    @Test
    public void tokenClaimsContainCorrectRoles() throws Exception {
        User user = new GenericUser("admin_user", Set.of(Role.ADMIN, Role.USER));
        String token = jwtHelper.getJwtAccessToken(user, "my-client", "admin", 60);

        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);
        assertTrue(auth.getRoles().contains(Role.ADMIN));
        assertTrue(auth.getRoles().contains(Role.USER));
    }

    @Test
    public void tokenClaimsContainCorrectScope() throws Exception {
        User user = new GenericUser("testuser", Set.of(Role.USER));
        String token = jwtHelper.getJwtAccessToken(user, "my-client", "user", 60);

        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);
        assertEquals("user", auth.getScope());
    }

    @Test
    public void tokenWithNoRoles() throws Exception {
        User user = new GenericUser("testuser", Set.of());
        String token = jwtHelper.getJwtAccessToken(user, "my-client", "admin", 60);

        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);
        assertTrue(auth.getRoles().isEmpty());
    }

    // --- Token Verification ---

    @Test
    public void validTokenVerifiesSuccessfully() throws Exception {
        User user = new GenericUser("testuser", Set.of(Role.ADMIN));
        String token = jwtHelper.getJwtAccessToken(user, "test-client", "admin", 60);

        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);

        assertNotNull(auth);
        assertEquals("testuser", auth.getUsername());
    }

    @Test
    public void expiredTokenThrowsAuthenticationException() throws Exception {
        // Build a genuinely expired token signed with the SAME key JwtHelper uses, so the only reason verification
        // fails is the expired 'exp' claim (and not a malformed token or a signature/issuer mismatch). All other
        // required claims (issuer, audience, subject, valid signature) are present and correct. The expiration is set
        // well beyond JwtHelper's 30 second allowed clock skew.
        RsaJsonWebKey key = getJwtHelperKey();

        NumericDate expiredAt = NumericDate.now();
        expiredAt.addSeconds(-600); // 10 minutes in the past

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER_NAME);
        claims.setAudience(AUDIENCE);
        claims.setExpirationTime(expiredAt);
        claims.setGeneratedJwtId();
        claims.setIssuedAt(expiredAt);
        claims.setNotBefore(expiredAt);
        claims.setSubject("testuser");
        claims.setStringListClaim("role", List.of(Role.ADMIN));
        claims.setClaim("scope", "admin");

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(key.getPrivateKey());
        jws.setKeyIdHeaderValue(key.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String expiredToken = jws.getCompactSerialization();

        assertThrows(AuthenticationException.class, () -> {
            jwtHelper.verifyAndParseJwtAccessToken(expiredToken);
        });
    }

    @Test
    public void malformedTokenThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class, () -> {
            jwtHelper.verifyAndParseJwtAccessToken("not-a-jwt");
        });
    }

    @Test
    public void emptyTokenThrowsAuthenticationException() {
        assertThrows(AuthenticationException.class, () -> {
            jwtHelper.verifyAndParseJwtAccessToken("");
        });
    }

    @Test
    public void wrongSignatureTokenThrowsAuthenticationException() throws Exception {
        // Create a token signed with a DIFFERENT RSA key
        RsaJsonWebKey differentKey = RsaJwkGenerator.generateJwk(2048);

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER_NAME);
        claims.setAudience(AUDIENCE);
        claims.setExpirationTimeMinutesInTheFuture(60);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject("testuser");
        claims.setStringListClaim("role", List.of(Role.ADMIN));
        claims.setClaim("scope", "admin");

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(differentKey.getPrivateKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String tokenWithWrongKey = jws.getCompactSerialization();

        assertThrows(AuthenticationException.class, () -> {
            jwtHelper.verifyAndParseJwtAccessToken(tokenWithWrongKey);
        });
    }

    @Test
    public void tokenWithWrongIssuerThrowsAuthenticationException() throws Exception {
        // Sign with the SAME key JwtHelper uses but set a wrong 'iss' claim, so the ONLY reason verification fails is
        // the issuer mismatch (the signature is valid and all other required claims are present and correct). This
        // isolates the test to actual issuer enforcement rather than a signature verification failure.
        RsaJsonWebKey key = getJwtHelperKey();

        JwtClaims claims = new JwtClaims();
        claims.setIssuer("wrong-issuer");
        claims.setAudience(AUDIENCE);
        claims.setExpirationTimeMinutesInTheFuture(60);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject("testuser");
        claims.setStringListClaim("role", List.of(Role.ADMIN));
        claims.setClaim("scope", "admin");

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(key.getPrivateKey());
        jws.setKeyIdHeaderValue(key.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String wrongIssuerToken = jws.getCompactSerialization();

        assertThrows(AuthenticationException.class, () -> {
            jwtHelper.verifyAndParseJwtAccessToken(wrongIssuerToken);
        });
    }

    // --- Round Trip ---

    @Test
    public void roundTripPreservesAllFields() throws Exception {
        User user = new GenericUser("myuser", Set.of(Role.ADMIN, Role.USER));
        String token = jwtHelper.getJwtAccessToken(user, "test-client-id", "admin user", 60);

        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);

        assertEquals("myuser", auth.getUsername());
        assertEquals(Set.of(Role.ADMIN, Role.USER), auth.getRoles());
        assertEquals("admin user", auth.getScope());
    }

    @Test
    public void multipleTokensForSameUserAreDistinct() {
        User user = new GenericUser("testuser", Set.of(Role.ADMIN));
        String token1 = jwtHelper.getJwtAccessToken(user, "client1", "admin", 60);
        String token2 = jwtHelper.getJwtAccessToken(user, "client2", "admin", 60);

        // Different JTI and potentially different iat means different tokens
        assertNotEquals(token1, token2);
    }

    // --- Key Persistence ---

    @Test
    public void twoJwtHelperInstancesShareSameKey() throws Exception {
        // Both instances should load the same key from disk
        JwtHelper helper2 = new JwtHelper();

        User user = new GenericUser("testuser", Set.of(Role.ADMIN));
        String token = jwtHelper.getJwtAccessToken(user, "client", "admin", 60);

        // The second instance should be able to verify tokens created by the first
        Authentication auth = helper2.verifyAndParseJwtAccessToken(token);
        assertEquals("testuser", auth.getUsername());
    }
}
