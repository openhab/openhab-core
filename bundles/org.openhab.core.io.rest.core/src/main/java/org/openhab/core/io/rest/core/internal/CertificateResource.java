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
package org.openhab.core.io.rest.core.internal;

import static org.openhab.core.io.jetty.certificate.CertificateGenerator.KEYSTORE_ENTRY_ALIAS;
import static org.openhab.core.io.jetty.certificate.CertificateGenerator.KEYSTORE_JKS_TYPE;
import static org.openhab.core.io.jetty.certificate.CertificateGenerator.KEYSTORE_PASSWORD;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.jetty.certificate.CertificateGenerator;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The {@link CertificateResource} allows changing the Jetty server certificate
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(CertificateResource.PATH_CERTIFICATE)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@javax.ws.rs.Path(CertificateResource.PATH_CERTIFICATE)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = CertificateResource.PATH_CERTIFICATE)
@NonNullByDefault
public class CertificateResource implements RESTResource {
    static final String PATH_CERTIFICATE = "certificate";
    static final String JETTY_KEYSTORE_PATH_PROPERTY = "jetty.keystore.path";

    private final Logger logger = LoggerFactory.getLogger(CertificateResource.class);

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateCertificate", summary = "Updates the Jetty server certificate.", responses = {
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "400", description = "Bad Request") })
    public Response putCertificate(
            @Parameter(description = "a certificate chain with private key", required = true) CertificateDTO value) {
        try {
            if ((value.certificateChain == null || value.certificateChain.isBlank())
                    && (value.privateKey == null || value.privateKey.isBlank())) {
                renewKeyStoreSelfSigned();
                return Response.status(Response.Status.ACCEPTED.getStatusCode(), "Self-Signed Certificate set.")
                        .build();
            } else {
                renewKeyStoreFromDTO(value);
                return Response.status(Response.Status.ACCEPTED.getStatusCode(), "Sent Certificate set.").build();
            }
        } catch (CertificateException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage()).build();
        }
    }

    public static class CertificateDTO {
        public @Nullable String certificateChain;
        public @Nullable String privateKey;
    }

    void renewKeyStoreSelfSigned() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_JKS_TYPE);
            keyStore.load(null, null);
            CertificateGenerator.generateCertificate(keyStore);
            // store in file
            keyStore.store(new FileOutputStream(getKeystoreFile()), KEYSTORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            logger.warn("Failed to re-generate self-signed certificate: {}", e.getMessage());
        }
    }

    void renewKeyStoreFromDTO(CertificateDTO certificateDTO)
            throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        List<X509Certificate> certificates = getCertsFromPEM(certificateDTO.certificateChain);
        PrivateKey privateKey = getPrivateKeyFromPEM(certificateDTO.privateKey);

        if (certificates.isEmpty() || privateKey == null) {
            logger.warn("Could not get necessary information to regenerate keystore. Check input!");
            return;
        }

        try {
            Certificate[] certArray = certificates.toArray(Certificate[]::new);

            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_JKS_TYPE);
            keyStore.load(null, null);

            // store private key with certificate
            keyStore.setKeyEntry(KEYSTORE_ENTRY_ALIAS, privateKey, KEYSTORE_PASSWORD.toCharArray(), certArray);

            // add all except own certificate to store as additional certificates
            for (int i = 1; i < certArray.length; i++) {
                keyStore.setCertificateEntry("chain" + i, certArray[i]);
            }

            // store in file
            keyStore.store(new FileOutputStream(getKeystoreFile()), KEYSTORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            logger.warn("Failed to re-generate keystore: {}", e.getMessage());
        }
    }

    private File getKeystoreFile() {
        String keystorePath = System.getProperty(JETTY_KEYSTORE_PATH_PROPERTY);
        return new File(keystorePath);
    }

    List<X509Certificate> getCertsFromPEM(@Nullable String certString) throws IOException, CertificateException {
        if (certString == null) {
            throw new CertificateException("Certificates mst not be null");
        }
        InputStream certInputStream = new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
        List<X509Certificate> certificates = new ArrayList<>();
        while (certInputStream.available() > 0) {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(certInputStream);
            logger.debug("Imported subject='{}', serial='{}', until='{}'", certificate.getSubjectDN(),
                    certificate.getSerialNumber(), certificate.getNotAfter());
            certificates.add(certificate);
        }

        logger.info("Imported {} certificates for the Jetty server certificate chain.", certificates.size());

        return certificates;
    }

    @Nullable
    PrivateKey getPrivateKeyFromPEM(@Nullable String keyString)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (keyString == null) {
            throw new CertificateException("Key must not be null");
        }
        String privateKeyPEM = keyString.replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "").replace("-----END PRIVATE KEY-----", "");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPEM));
        PrivateKey key = keyFactory.generatePrivate(keySpec);
        logger.info("Imported private key for the Jetty server certificate chain.");
        return key;
    }
}
