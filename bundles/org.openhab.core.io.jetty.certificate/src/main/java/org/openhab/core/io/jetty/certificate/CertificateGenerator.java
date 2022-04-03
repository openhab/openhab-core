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
package org.openhab.core.io.jetty.certificate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class CertificateGenerator implements BundleActivator {

    public static final String JETTY_KEYSTORE_PATH_PROPERTY = "jetty.keystore.path";
    public static final String KEYSTORE_PASSWORD = "openhab";
    public static final String KEYSTORE_ENTRY_ALIAS = "mykey";
    public static final String KEYSTORE_JKS_TYPE = "JKS";
    private static final String KEY_PAIR_GENERATOR_TYPE = "EC";
    private static final String CONTENT_SIGNER_ALGORITHM = "SHA256withECDSA";
    private static final String CERTIFICATE_X509_TYPE = "X.509";
    public static final String X500_NAME = "CN=openhab.org, OU=None, O=None, L=None, C=None";

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);

    @Override
    public void start(BundleContext context) throws Exception {

        try {
            String keystorePath = System.getProperty(JETTY_KEYSTORE_PATH_PROPERTY);
            File keystoreFile = new File(keystorePath);
            KeyStore keystore = ensureKeystore(keystoreFile);

            if (!isCertificateInKeystore(keystore)) {
                LOGGER.debug("{} alias not found. Generating a new certificate.", KEYSTORE_ENTRY_ALIAS);
                generateCertificate(keystore);
                LOGGER.debug("Save the keystore into {}.", keystoreFile.getAbsolutePath());
                keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_PASSWORD.toCharArray());
            } else {
                LOGGER.debug("{} alias found. Do nothing.", KEYSTORE_ENTRY_ALIAS);
            }
        } catch (CertificateException | KeyStoreException e) {
            LOGGER.error("Failed to generate a new SSL Certificate.", e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Nothing to do.
    }

    /**
     * Ensure that the keystore exist and is readable. If not, create a new one.
     *
     * @throws KeyStoreException if the creation of the keystore fails or if it is not readable.
     */
    private KeyStore ensureKeystore(File keystoreFile) throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_JKS_TYPE);
        if (!keystoreFile.exists()) {
            try {
                LOGGER.debug("No keystore found. Creation of {}", keystoreFile.getAbsolutePath());
                boolean newFileCreated = keystoreFile.createNewFile();
                if (newFileCreated) {
                    keyStore.load(null, null);
                } else {
                    throw new IOException("Keystore file creation failed.");
                }
            } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
                throw new KeyStoreException("Failed to create the keystore " + keystoreFile.getAbsolutePath(), e);
            }
        } else {
            try (InputStream keystoreStream = new FileInputStream(keystoreFile)) {
                LOGGER.debug("Keystore found. Trying to load {}", keystoreFile.getAbsolutePath());
                keyStore.load(keystoreStream, KEYSTORE_PASSWORD.toCharArray());
            } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
                throw new KeyStoreException("Failed to load the keystore " + keystoreFile.getAbsolutePath(), e);
            }
        }

        return keyStore;
    }

    /**
     * Check if the keystore contains a certificate with the KEYSTORE_ENTRY_ALIAS.
     *
     * @param keystore the keystore to check
     * @return true if the alias is already present in the keystore, else false.
     * @throws KeyStoreException If the keystore cannot be read.
     */
    private boolean isCertificateInKeystore(KeyStore keystore) throws KeyStoreException {
        return keystore.getCertificate(KEYSTORE_ENTRY_ALIAS) != null;
    }

    /**
     * Generate a new certificate and store it in the given keystore.
     *
     * @param keystore the keystore to add this certificate to
     * @throws CertificateException if the certificate generation has failed.
     * @throws KeyStoreException If save of the keystore has failed.
     */
    public static void generateCertificate(KeyStore keystore) throws CertificateException, KeyStoreException {
        try {
            long startTime = System.currentTimeMillis();
            KeyPair keyPair = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_TYPE).generateKeyPair();
            LOGGER.debug("Keys generated in {} ms.", (System.currentTimeMillis() - startTime));

            X500Name issuerDN = new X500Name(X500_NAME);
            SubjectPublicKeyInfo pubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
            Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));

            X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(issuerDN,
                    new BigInteger(10, new SecureRandom()), notBefore, notAfter, issuerDN, pubKeyInfo);
            ContentSigner contentSigner = new JcaContentSignerBuilder(CONTENT_SIGNER_ALGORITHM)
                    .build(keyPair.getPrivate());
            X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

            Certificate certificate = java.security.cert.CertificateFactory.getInstance(CERTIFICATE_X509_TYPE)
                    .generateCertificate(new ByteArrayInputStream(
                            ByteBuffer.wrap(certificateHolder.toASN1Structure().getEncoded()).array()));

            LOGGER.debug("Total certificate generation time: {} ms.", (System.currentTimeMillis() - startTime));

            keystore.setKeyEntry(KEYSTORE_ENTRY_ALIAS, keyPair.getPrivate(), KEYSTORE_PASSWORD.toCharArray(),
                    new java.security.cert.Certificate[] { certificate });
        } catch (NoSuchAlgorithmException | IOException | OperatorCreationException e) {
            throw new CertificateException("Failed to generate the new certificate.", e);
        }
    }
}
