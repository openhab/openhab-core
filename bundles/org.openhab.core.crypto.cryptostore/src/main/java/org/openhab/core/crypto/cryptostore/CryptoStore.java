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
package org.openhab.core.crypto.cryptostore;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CryptoStore} is responsible for handling PKI functions.
 *
 * @author Ben Rosenblum - Initial contribution
 */
// @NonNullByDefault
public class CryptoStore {

    private final Logger logger = LoggerFactory.getLogger(CryptoStore.class);

    private Key privKey;
    private Certificate cert;
    private String keystoreFileName = "";
    private String keystoreAlgorithm = "RSA";
    private int keyLength = 2048;
    private String alias = "openhab";
    private String distName = "CN=openHAB, O=openHAB, L=None, ST=None, C=None";

    public CryptoStore() {
    }

    public void setPrivKey(String privKey) throws GeneralSecurityException {
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance(this.keystoreAlgorithm);
        this.privKey = kf.generatePrivate(keySpec);
    }

    public void setPrivKey(Key privKey) {
        this.privKey = privKey;
    }

    public Key getPrivKey() {
        return this.privKey;
    }

    public void setCert(String cert) throws GeneralSecurityException {
        this.cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(cert)));
    }

    public void setCert(Certificate cert) throws GeneralSecurityException {
        this.cert = cert;
    }

    public Certificate getCert() {
        return this.cert;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlgorithm(String keystoreAlgorithm) {
        this.keystoreAlgorithm = keystoreAlgorithm;
    }

    public String getAlgorithm() {
        return this.keystoreAlgorithm;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public int getKeyLength() {
        return this.keyLength;
    }

    public void setDistName(String distName) {
        this.distName = distName;
    }

    public String getDistName() {
        return this.distName;
    }

    public void setKeys(String privKey, String cert) throws GeneralSecurityException {
        setPrivKey(privKey);
        setCert(cert);
    }

    public void setKeys(Key privKey, Certificate cert) {
        this.privKey = privKey;
        this.cert = cert;
    }

    public void setKeyStore(String keystoreFileName) {
        this.keystoreFileName = keystoreFileName;
    }

    public void loadFromKeyStore(String keystorePassword) throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance("JKS");
        FileInputStream keystoreInputStream = new FileInputStream(this.keystoreFileName);
        keystore.load(keystoreInputStream, keystorePassword.toCharArray());
        this.privKey = keystore.getKey(this.alias, keystorePassword.toCharArray());
        this.cert = keystore.getCertificate(this.alias);
    }

    public void saveKeyStore(String keystorePassword) throws GeneralSecurityException, IOException {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, null);
        keystore.setKeyEntry(this.alias, this.privKey, keystorePassword.toCharArray(),
                new java.security.cert.Certificate[] { this.cert });
        FileOutputStream keystoreStream = new FileOutputStream(keystoreFileName);
        keystore.store(keystoreStream, keystorePassword.toCharArray());
    }

    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String distName)
            throws GeneralSecurityException, OperatorCreationException {
        final Instant now = Instant.now();
        final Date notBefore = Date.from(now);
        final Date notAfter = Date.from(now.plus(Duration.ofDays(365 * 10)));
        X500Name name = new X500Name(distName);
        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(name,
                BigInteger.valueOf(now.toEpochMilli()), notBefore, notAfter, name, keyPair.getPublic());
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                .getCertificate(certificateBuilder.build(contentSigner));
    }

    public void generateNewKeyPair(String keystorePassword)
            throws GeneralSecurityException, OperatorCreationException, IOException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(this.keystoreAlgorithm);
        kpg.initialize(this.keyLength);
        KeyPair kp = kpg.generateKeyPair();
        Security.addProvider(new BouncyCastleProvider());
        Signature signer = Signature.getInstance("SHA256withRSA", "BC");
        signer.initSign(kp.getPrivate());
        signer.update("openhab".getBytes(StandardCharsets.UTF_8));
        signer.sign();
        X509Certificate signedcert = generateSelfSignedCertificate(kp, this.distName);
        this.privKey = kp.getPrivate();
        this.cert = signedcert;
    }
}
