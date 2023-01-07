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
import java.security.NoSuchAlgorithmException;
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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

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

    private final int KEY_SIZE = 128;
    private final int DATA_LENGTH = 128;
    private Cipher encryptionCipher;

    private String privKey;
    private Certificate cert;
    private String keystoreFileName = "";
    private String keystoreAlgorithm = "RSA";
    private int keyLength = 2048;
    private String alias = "openhab";
    private String distName = "CN=openHAB, O=openHAB, L=None, ST=None, C=None";
    private String cipher = "AES/GCM/NoPadding";

    public CryptoStore() {
    }

    public Key generateEncryptionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    public String encrypt(String data, Key key) throws Exception {
        return encrypt(data, key, this.cipher);
    }

    public String encrypt(String data, Key key, String cipher) throws Exception {
        byte[] dataInBytes = data.getBytes();
        encryptionCipher = Cipher.getInstance(cipher);
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = encryptionCipher.doFinal(dataInBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryptedData, Key key) throws Exception {
        return decrypt(encryptedData, key, this.cipher);
    }

    public String decrypt(String encryptedData, Key key, String cipher) throws Exception {
        byte[] dataInBytes = Base64.getDecoder().decode(encryptedData);
        Cipher decryptionCipher = Cipher.getInstance(cipher);
        GCMParameterSpec spec = new GCMParameterSpec(DATA_LENGTH, encryptionCipher.getIV());
        decryptionCipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decryptedBytes = decryptionCipher.doFinal(dataInBytes);
        return new String(decryptedBytes);
    }

    public void setPrivKey(String privKey, Key key) throws Exception {
        this.privKey = encrypt(privKey, key);
    }

    public String getPrivKey(Key key) throws Exception {
        return decrypt(this.privKey, key);
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

    public void setKeys(String privKey, Key key, String cert) throws GeneralSecurityException, Exception {
        setPrivKey(privKey, key);
        setCert(cert);
    }

    public void setKeyStore(String keystoreFileName) {
        this.keystoreFileName = keystoreFileName;
    }

    public void loadFromKeyStore(String keystoreFileName, String keystorePassword, Key key)
            throws GeneralSecurityException, IOException, Exception {
        this.keystoreFileName = keystoreFileName;
        loadFromKeyStore(keystorePassword, key);
    }

    public void loadFromKeyStore(String keystorePassword, Key key)
            throws GeneralSecurityException, IOException, Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        FileInputStream keystoreInputStream = new FileInputStream(this.keystoreFileName);
        keystore.load(keystoreInputStream, keystorePassword.toCharArray());
        this.privKey = encrypt(new String(keystore.getKey(this.alias, keystorePassword.toCharArray()).getEncoded()),
                key);
        this.cert = keystore.getCertificate(this.alias);
    }

    public KeyStore getKeyStore(String keystorePassword, Key key)
            throws GeneralSecurityException, IOException, Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, null);
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(getPrivKey(key));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance(this.keystoreAlgorithm);
        keystore.setKeyEntry(this.alias, kf.generatePrivate(keySpec), keystorePassword.toCharArray(),
                new java.security.cert.Certificate[] { this.cert });
        return keystore;
    }

    public void saveKeyStore(String keystorePassword, Key key) throws GeneralSecurityException, IOException, Exception {
        saveKeyStore(this.keystoreFileName, keystorePassword, key);
    }

    public void saveKeyStore(String keystoreFileName, String keystorePassword, Key key)
            throws GeneralSecurityException, IOException, Exception {
        FileOutputStream keystoreStream = new FileOutputStream(keystoreFileName);
        getKeyStore(keystorePassword, key).store(keystoreStream, keystorePassword.toCharArray());
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

    public void generateNewKeyPair(Key key)
            throws GeneralSecurityException, OperatorCreationException, IOException, Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(this.keystoreAlgorithm);
        kpg.initialize(this.keyLength);
        KeyPair kp = kpg.generateKeyPair();
        Security.addProvider(new BouncyCastleProvider());
        Signature signer = Signature.getInstance("SHA256withRSA", "BC");
        signer.initSign(kp.getPrivate());
        signer.update("openhab".getBytes(StandardCharsets.UTF_8));
        signer.sign();
        X509Certificate signedcert = generateSelfSignedCertificate(kp, this.distName);
        this.privKey = encrypt(new String(kp.getPrivate().getEncoded()), key);
        this.cert = signedcert;
    }
}
