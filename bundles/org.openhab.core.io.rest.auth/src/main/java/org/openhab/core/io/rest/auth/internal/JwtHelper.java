/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.sasl.AuthenticationException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKey.OutputControlLevel;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.User;
import org.openhab.core.config.core.ConfigConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class helps with JWT tokens' building, signing, verifying and parsing.
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = JwtHelper.class)
public class JwtHelper {
    private final Logger logger = LoggerFactory.getLogger(JwtHelper.class);

    private static final String KEY_FILE_PATH = ConfigConstants.getUserDataFolder() + File.separator + "secrets"
            + File.separator + "rsa_json_web_key.json";

    private static final String ISSUER_NAME = "openhab";
    private static final String AUDIENCE = "openhab";

    private RsaJsonWebKey jwtWebKey;

    public JwtHelper() {
        try {
            jwtWebKey = loadOrGenerateKey();
        } catch (Exception e) {
            logger.error("Error while initializing the JWT helper", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private RsaJsonWebKey generateNewKey() throws JoseException, FileNotFoundException, IOException {
        RsaJsonWebKey newKey = RsaJwkGenerator.generateJwk(2048);

        File file = new File(KEY_FILE_PATH);
        file.getParentFile().mkdirs();

        String keyJson = newKey.toJson(OutputControlLevel.INCLUDE_PRIVATE);

        Files.writeString(file.toPath(), keyJson, StandardCharsets.UTF_8);
        return newKey;
    }

    private RsaJsonWebKey loadOrGenerateKey() throws FileNotFoundException, JoseException, IOException {
        try (final BufferedReader reader = Files.newBufferedReader(Paths.get(KEY_FILE_PATH))) {
            return (RsaJsonWebKey) JsonWebKey.Factory.newJwk(reader.readLine());
        } catch (IOException | JoseException e) {
            RsaJsonWebKey key = generateNewKey();
            logger.debug("Created JWT signature key in {}", KEY_FILE_PATH);
            return key;
        }
    }

    /**
     * Builds a new access token.
     *
     * @param user the user (subject) to build the token, it will also add the roles as claims
     * @param clientId the client ID the token is for
     * @param scope the scope the token is valid for
     * @param tokenLifetime the lifetime of the token in minutes before it expires
     *
     * @return a base64-encoded signed JWT token to be passed as a bearer token in API requests
     */
    public String getJwtAccessToken(User user, String clientId, String scope, int tokenLifetime) {
        try {
            JwtClaims jwtClaims = new JwtClaims();
            jwtClaims.setIssuer(ISSUER_NAME);
            jwtClaims.setAudience(AUDIENCE);
            jwtClaims.setExpirationTimeMinutesInTheFuture(tokenLifetime);
            jwtClaims.setGeneratedJwtId();
            jwtClaims.setIssuedAtToNow();
            jwtClaims.setNotBeforeMinutesInThePast(2);
            jwtClaims.setSubject(user.getName());
            jwtClaims.setClaim("client_id", clientId);
            jwtClaims.setClaim("scope", scope);
            jwtClaims.setStringListClaim("role",
                    new ArrayList<>(user.getRoles() != null ? user.getRoles() : Collections.emptySet()));

            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(jwtClaims.toJson());
            jws.setKey(jwtWebKey.getPrivateKey());
            jws.setKeyIdHeaderValue(jwtWebKey.getKeyId());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            String jwt = jws.getCompactSerialization();

            return jwt;
        } catch (Exception e) {
            logger.error("Error while writing JWT token", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Performs verifications on a JWT token, then parses it into a {@link AuthenticationException} instance
     *
     * @param jwt the base64-encoded JWT token from the request
     * @return the {@link Authentication} derived from the information in the token
     * @throws AuthenticationException
     */
    public Authentication verifyAndParseJwtAccessToken(String jwt) throws AuthenticationException {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder().setRequireExpirationTime().setAllowedClockSkewInSeconds(30)
                .setRequireSubject().setExpectedIssuer(ISSUER_NAME).setExpectedAudience(AUDIENCE)
                .setVerificationKey(jwtWebKey.getKey())
                .setJwsAlgorithmConstraints(ConstraintType.WHITELIST, AlgorithmIdentifiers.RSA_USING_SHA256).build();

        try {
            JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
            String username = jwtClaims.getSubject();
            List<String> roles = jwtClaims.getStringListClaimValue("role");
            Authentication auth = new Authentication(username, roles.toArray(new String[roles.size()]));
            return auth;
        } catch (Exception e) {
            logger.error("Error while processing JWT token", e);
            throw new AuthenticationException(e.getMessage());
        }
    }
}
