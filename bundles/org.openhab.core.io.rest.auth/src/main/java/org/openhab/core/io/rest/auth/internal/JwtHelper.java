package org.openhab.core.io.rest.auth.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
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

@NonNullByDefault
@Component(immediate = true, service = JwtHelper.class)
public class JwtHelper {
    private final Logger logger = LoggerFactory.getLogger(TokenResource.class);

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

        IOUtils.write(keyJson, new FileOutputStream(file));
        return newKey;
    }

    private RsaJsonWebKey loadOrGenerateKey() throws FileNotFoundException, JoseException, IOException {
        try {
            List<String> lines = IOUtils.readLines(new FileInputStream(KEY_FILE_PATH));
            return (RsaJsonWebKey) JsonWebKey.Factory.newJwk(lines.get(0));
        } catch (IOException | JoseException e) {
            logger.info("Creating new JWT signature key");
            return generateNewKey();
        }
    }

    public String getJwtAccessToken(User user, String clientId) {
        try {
            JwtClaims jwtClaims = new JwtClaims();
            jwtClaims.setIssuer(ISSUER_NAME);
            jwtClaims.setAudience(AUDIENCE);
            jwtClaims.setExpirationTimeMinutesInTheFuture(24 * 60);
            jwtClaims.setGeneratedJwtId();
            jwtClaims.setIssuedAtToNow();
            jwtClaims.setNotBeforeMinutesInThePast(2);
            jwtClaims.setSubject(user.getName());
            jwtClaims.setStringListClaim("role",
                    new ArrayList<>((user.getRoles() != null) ? user.getRoles() : Collections.emptySet()));

            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(jwtClaims.toJson());
            jws.setKey(jwtWebKey.getPrivateKey());
            jws.setKeyIdHeaderValue(jwtWebKey.getKeyId());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA512);
            String jwt = jws.getCompactSerialization();

            return jwt;
        } catch (Exception e) {
            logger.error("Error while writing JWT token", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public Authentication verifyAndParseJwtAccessToken(String jwt) {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder().setRequireExpirationTime().setAllowedClockSkewInSeconds(30)
                .setRequireSubject().setExpectedIssuer(ISSUER_NAME).setExpectedAudience(AUDIENCE)
                .setVerificationKey(jwtWebKey.getKey())
                .setJwsAlgorithmConstraints(ConstraintType.WHITELIST, AlgorithmIdentifiers.RSA_USING_SHA512).build();

        try {
            JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
            String username = jwtClaims.getSubject();
            List<String> roles = jwtClaims.getStringListClaimValue("role");
            Authentication auth = new Authentication(username, roles.toArray(new String[roles.size()]));
            return auth;
        } catch (Exception e) {
            logger.error("Error while processing JWT token", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
