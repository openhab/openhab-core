/**
 * Copyright (c) 2015-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.jetty.certificate.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECField;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.net.HttpServiceUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Registration;
import org.shredzone.acme4j.RegistrationBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.challenge.TlsSni01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeUnauthorizedException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CertificateGenerator is capable of generating self-signed certificates for stand-alone instances of openHAB, but also
 * implements the Automatic Certificate Management Environment (ACME) protocol. ACME is a protocol that a certificate
 * authority (CA) and an applicant can use to automate the process of verification and certificate issuance. This
 * implementation is compatible with certificate provider Let's Encrypt (www.letsencrypt.org), and implements both the
 * HTTP challenge mechanism as well as the TLS-SNI v1 challenge mechanism (when Jetty 9.3.x will be integrated)
 *
 * @author Kai Kreuzer - Initial Contribution
 * @author Karel Goderis - ACME implementation
 *
 */
@SuppressWarnings("deprecation") // We support the depreciated TSL-SNI v1 challenge type, since v2 is not supported by
                                 // Lets Encrypt, but that API is in fact depreciated
public class CertificateGenerator implements Servlet {

    private final Logger logger = LoggerFactory.getLogger(CertificateGenerator.class);

    // sun.security.ssl.SunX509KeyManagerImpl is flawed. More specifically, at some time the constructor
    // SunX509KeyManagerImpl(KeyStore ks, char[] password) is called with the password for the certificate that was set
    // in the SSLContextFactory,e.g "mykey"/"openhab". In this constructor, the SunX509KeyManager traverses all the
    // aliases in the KeyStore with that given password, and tries to load them. In short, if one of the Keys in the
    // KeyStore is protected with a different password, an Exception in thrown, and Jetty will not started as it should
    // be. And thus, all Keys in the KeyStore should be protected with the same password

    private static final String JETTY_KEYSTORE_PATH_PROPERTY = "jetty.keystore.path";
    private static final String KEYSTORE_USER_PASSWORD = "openhab";
    private static final String KEYSTORE_USER_ALIAS = "acmeuser";
    private static final String KEYSTORE_SIGN_PASSWORD = "openhab";
    private static final String KEYSTORE_SIGN_ALIAS = "mykey";
    private static final String KEYSTORE_SNI_PASSWORD = "openhab";
    private static final String KEYSTORE_SNI_ALIAS = "sni";
    private static final String KEYSTORE_JKS_TYPE = "JKS";
    private static final String CURVE_NAME = "prime256v1";
    private static final String KEY_PAIR_GENERATOR_TYPE = "EC";
    private static final String KEY_FACTORY_TYPE = "EC";
    private static final String CONTENT_SIGNER_ALGORITHM = "SHA256withECDSA";
    private static final String CERTIFICATE_X509_TYPE = "X.509";
    private static final String X500_NAME = "CN=openhab.org, OU=None, O=None, L=None, C=None";
    private static final String WEBAPP_ALIAS = "/.well-known";
    private static final String SERVLET_NAME = "acme-challenge";

    private BundleContext bundleContext;
    private ComponentContext componentContext;
    private boolean tlssni = false;;

    private File keystoreFile;
    private Challenge challenge;

    protected HttpService httpService;
    private ServletConfig sc = null;

    private static final String GENERATOR_THREADPOOL_NAME = "certificateGenerator";
    protected final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(GENERATOR_THREADPOOL_NAME);
    private ScheduledFuture<?> renewJob;

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    public void activate(ComponentContext componentContext, BundleContext bundleContext) throws Exception {

        this.bundleContext = bundleContext;
        this.componentContext = componentContext;
        Dictionary<String, Object> props = componentContext.getProperties();

        for (Bundle aBundle : bundleContext.getBundles()) {
            if (aBundle.getSymbolicName().equals("org.eclipse.jetty.server")) {
                if (aBundle.getVersion().getMajor() == 9 && aBundle.getVersion().getMinor() >= 3) {
                    tlssni = true;
                    break;
                }
            }
        }

        logger.info("TLS-SNI challenges are {}enabled", tlssni ? "" : "not ");

        if (props.get("type") == null || props.get("domain") == null) {
            return;
        }

        try {
            logger.debug("Starting up the ACME-challenge servlet at " + WEBAPP_ALIAS + "/" + SERVLET_NAME);

            Hashtable<String, String> servletProps = new Hashtable<String, String>();
            if (httpService != null) {
                httpService.registerServlet(WEBAPP_ALIAS + "/" + SERVLET_NAME, this, servletProps, createHttpContext());
            }
        } catch (NamespaceException e) {
            logger.error("An exception occured while starting the servlet : '{}'", e);
        } catch (ServletException e) {
            logger.error("An exception occured while starting the servlet : '{}'", e);
        }

        installCertificate();
    }

    public void deactivate(ComponentContext context) throws Exception {
        if (httpService != null) {
            httpService.unregister(WEBAPP_ALIAS + "/" + SERVLET_NAME);
        }

        if (renewJob != null) {
            renewJob.cancel(true);
        }
    }

    protected void installCertificate() {
        try {
            Dictionary<String, Object> props = componentContext.getProperties();

            KeyStore keystore = ensureKeystore();
            KeyPair keyPair = null;
            Key privateKey;

            privateKey = keystore.getKey(KEYSTORE_SIGN_ALIAS, KEYSTORE_SIGN_PASSWORD.toCharArray());

            if (privateKey == null || !(privateKey instanceof PrivateKey)) {
                logger.debug("Generating a new instance keypair because of a missing private key");
                keyPair = generateKeyPair();
                privateKey = keyPair.getPrivate();
            }

            if (privateKey != null && keyPair == null) {
                Certificate certificate = keystore.getCertificate(KEYSTORE_SIGN_ALIAS);
                if (certificate != null) {
                    keyPair = new KeyPair(certificate.getPublicKey(), (PrivateKey) privateKey);
                } else {
                    logger.debug("Generating a new instance keypair because of a missing certificate");
                    keyPair = generateKeyPair();
                }
            }

            String type = (String) props.get("type");

            if (type.equals("selfsigned")) {
                Certificate certificate = keystore.getCertificate(KEYSTORE_SIGN_ALIAS);

                if (certificate == null) {
                    logger.debug("{} alias not found. Generating a new certificate.", KEYSTORE_SIGN_ALIAS);
                    certificate = generateCertificate(keyPair);
                } else {

                    boolean isSelfSigned = false;
                    try {
                        PublicKey key = certificate.getPublicKey();
                        certificate.verify(key);
                        isSelfSigned = true;
                    } catch (SignatureException sigEx) {
                        isSelfSigned = false;
                    } catch (InvalidKeyException keyEx) {
                        isSelfSigned = false;
                    }

                    if (!isSelfSigned) {
                        logger.debug("Replacing a CA-signed certificate with a self-signed certificate");
                        certificate = generateCertificate(keyPair);
                    } else {
                        logger.debug("{} alias found. Do nothing.", KEYSTORE_SIGN_ALIAS);
                    }
                }

                if (certificate != null) {
                    logger.debug("Putting the self-signed certificate in the keystore");
                    keystore.setKeyEntry(KEYSTORE_SIGN_ALIAS, privateKey, KEYSTORE_SIGN_PASSWORD.toCharArray(),
                            new java.security.cert.Certificate[] { certificate });
                    logger.debug("Save the keystore into {}.", keystoreFile.getAbsolutePath());
                    keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_SIGN_PASSWORD.toCharArray());

                    for (Bundle aBundle : bundleContext.getBundles()) {
                        if (aBundle.getSymbolicName().equals("org.eclipse.jetty.osgi.boot")) {
                            logger.info("Restarting Jetty");
                            aBundle.stop();
                            aBundle.start();
                            break;
                        }
                    }
                }
            } else if (type.equals("acme")) {

                Certificate certificate = keystore.getCertificate(KEYSTORE_SIGN_ALIAS);
                String challengeType;

                if (certificate != null) {

                    boolean isSelfSigned = false;
                    try {
                        PublicKey key = certificate.getPublicKey();
                        certificate.verify(key);
                        isSelfSigned = true;
                    } catch (SignatureException sigEx) {
                        isSelfSigned = false;
                    } catch (InvalidKeyException keyEx) {
                        isSelfSigned = false;
                    }

                    if (!isSelfSigned) {
                        logger.debug("The certificate is expiring on {}",
                                ((X509Certificate) certificate).getNotAfter());

                        Date expiry = ((X509Certificate) certificate).getNotAfter();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(expiry);
                        cal.add(Calendar.DAY_OF_MONTH, -7);

                        if (cal.getTime().after(new Date())) {

                            logger.debug("Scheduling a certificate renewal on {}", cal.getTime().toString());
                            if (renewJob != null) {
                                renewJob.cancel(false);
                            }

                            renewJob = scheduler.schedule(renewRunnable,
                                    cal.getTimeInMillis() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

                            return;
                        }

                        if (tlssni) {
                            challengeType = TlsSni01Challenge.TYPE;
                        } else {
                            challengeType = Http01Challenge.TYPE;
                        }
                    } else {
                        challengeType = Http01Challenge.TYPE;
                    }
                    certificate = null;
                } else {
                    challengeType = Http01Challenge.TYPE;
                }

                String domain = (String) props.get("domain");

                if (certificate == null) {
                    certificate = fetchCertificate(keyPair, domain, keystore, challengeType);
                }

                if (certificate != null) {
                    logger.debug("Putting the CA-signed certificate in the keystore");
                    keystore.setKeyEntry(KEYSTORE_SIGN_ALIAS, privateKey, KEYSTORE_SIGN_PASSWORD.toCharArray(),
                            new java.security.cert.Certificate[] { certificate });
                    logger.debug("Save the keystore into {}.", keystoreFile.getAbsolutePath());
                    keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_SIGN_PASSWORD.toCharArray());

                    for (Bundle aBundle : bundleContext.getBundles()) {
                        if (aBundle.getSymbolicName().equals("org.eclipse.jetty.osgi.boot")) {
                            logger.info("Restarting Jetty");
                            aBundle.stop();
                            aBundle.start();
                            break;
                        }
                    }

                    if (renewJob == null) {
                        Date expiry = ((X509Certificate) certificate).getNotAfter();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(expiry);
                        cal.add(Calendar.DAY_OF_MONTH, -7);

                        logger.debug("Scheduling a certificate renewal on {}", cal.getTime().toString());
                        if (renewJob != null) {
                            renewJob.cancel(false);
                        }

                        renewJob = scheduler.schedule(renewRunnable, cal.getTimeInMillis() - System.currentTimeMillis(),
                                TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to generate or to fetch a new SSL Certificate : '{}'", e.getMessage());
        }
    }

    protected Runnable renewRunnable = new Runnable() {

        @Override
        public void run() {
            installCertificate();
        }
    };

    /**
     * Ensure that the keystore exist and is readable. If not, create a new one.
     *
     * @throws KeyStoreException if the creation of the keystore fails or if it is not readable.
     */
    private KeyStore ensureKeystore() throws KeyStoreException {
        String keystorePath = System.getProperty(JETTY_KEYSTORE_PATH_PROPERTY);
        keystoreFile = new File(keystorePath);
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_JKS_TYPE);
        if (!keystoreFile.exists()) {
            try {
                logger.debug("No keystore found. Creation of {}", keystoreFile.getAbsolutePath());
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
            try (InputStream keystoreStream = new FileInputStream(keystoreFile);) {
                logger.debug("Keystore found. Trying to load {}", keystoreFile.getAbsolutePath());
                keyStore.load(keystoreStream, KEYSTORE_SIGN_PASSWORD.toCharArray());
            } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
                throw new KeyStoreException("Failed to load the keystore " + keystoreFile.getAbsolutePath(), e);
            }
        }

        return keyStore;
    }

    private KeyPair generateKeyPair() throws KeyException {
        try {
            long startTime = System.currentTimeMillis();

            org.bouncycastle.jce.spec.ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
            ECField field = new ECFieldFp(ecSpec.getCurve().getField().getCharacteristic());
            EllipticCurve curve = new EllipticCurve(field, ecSpec.getCurve().getA().toBigInteger(),
                    ecSpec.getCurve().getB().toBigInteger());
            ECPoint pointG = new ECPoint(ecSpec.getG().getXCoord().toBigInteger(),
                    ecSpec.getG().getYCoord().toBigInteger());
            ECParameterSpec spec = new ECParameterSpec(curve, pointG, ecSpec.getN(), ecSpec.getH().intValue());
            KeyPairGenerator g = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_TYPE);
            g.initialize(spec, new SecureRandom());
            KeyPair keysPair = g.generateKeyPair();

            ECPrivateKeySpec ecPrivSpec = new ECPrivateKeySpec(((ECPrivateKey) keysPair.getPrivate()).getS(), spec);
            ECPublicKeySpec ecPublicSpec = new ECPublicKeySpec(((ECPublicKey) keysPair.getPublic()).getW(), spec);
            KeyFactory kf = KeyFactory.getInstance(KEY_FACTORY_TYPE);
            PrivateKey privateKey = kf.generatePrivate(ecPrivSpec);
            PublicKey publicKey = kf.generatePublic(ecPublicSpec);

            logger.debug("Keys generated in {} ms.", (System.currentTimeMillis() - startTime));

            return new KeyPair(publicKey, privateKey);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            throw new KeyException("Failed to generate a new keypair.", e);
        }
    }

    private Certificate generateCertificate(KeyPair keyPair) throws CertificateException, KeyStoreException {
        try {
            long startTime = System.currentTimeMillis();

            X500Name issuerDN = new X500Name(X500_NAME);
            Integer randomNumber = new Random().nextInt();
            BigInteger serialNumber = BigInteger.valueOf(randomNumber >= 0 ? randomNumber : randomNumber * -1);
            Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
            Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
            X500Name subjectDN = new X500Name(X500_NAME);
            byte[] publickeyb = keyPair.getPublic().getEncoded();
            ASN1Sequence sequence = (ASN1Sequence) ASN1Primitive.fromByteArray(publickeyb);
            SubjectPublicKeyInfo subPubKeyInfo = new SubjectPublicKeyInfo(sequence);
            X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(issuerDN, serialNumber, notBefore,
                    notAfter, subjectDN, subPubKeyInfo);

            ContentSigner contentSigner = new JcaContentSignerBuilder(CONTENT_SIGNER_ALGORITHM)
                    .build(keyPair.getPrivate());
            X509CertificateHolder certificateHolder = v3CertGen.build(contentSigner);

            Certificate certificate = java.security.cert.CertificateFactory.getInstance(CERTIFICATE_X509_TYPE)
                    .generateCertificate(new ByteArrayInputStream(
                            ByteBuffer.wrap(certificateHolder.toASN1Structure().getEncoded()).array()));

            logger.debug("Certificate generated in {} ms.", (System.currentTimeMillis() - startTime));

            return certificate;
        } catch (IOException | OperatorCreationException e) {
            throw new CertificateException("Failed to generate the new certificate.", e);
        }
    }

    public X509Certificate fetchCertificate(KeyPair keyPair, String domain, KeyStore keystore, String challengeType)
            throws IOException, AcmeException {
        boolean isNewKeyPair = false;
        Key privateKey = null;
        KeyPair userKeyPair = null;

        try {
            privateKey = keystore.getKey(KEYSTORE_USER_ALIAS, KEYSTORE_USER_PASSWORD.toCharArray());
        } catch (Exception e) {
            logger.error("An exception ocurred while recovering the ACME user private key : '{}'", e.getMessage());
        }

        if (privateKey == null || !(privateKey instanceof PrivateKey)) {
            logger.debug("Generating a new ACME user keypair");
            try {
                userKeyPair = generateKeyPair();
                privateKey = userKeyPair.getPrivate();
                isNewKeyPair = true;

                try {
                    Certificate certificate = generateCertificate(userKeyPair);
                    logger.debug("Putting the ACME user certificate in the keystore");
                    keystore.setKeyEntry(KEYSTORE_USER_ALIAS, privateKey, KEYSTORE_USER_PASSWORD.toCharArray(),
                            new java.security.cert.Certificate[] { certificate });
                    logger.debug("Save the keystore into {}.", keystoreFile.getAbsolutePath());
                    keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_SIGN_PASSWORD.toCharArray());
                } catch (Exception e) {
                    logger.error(
                            "An exception ocurred while creating & storing a certificate for the ACME user key pair : '{}'",
                            e.getMessage());
                }
            } catch (KeyException e) {
                logger.error("An exception ocurred while creating a new ACME user key pair : '{}'", e.getMessage());
            }
        }

        if ((privateKey != null && userKeyPair == null)) {
            Certificate certificate = null;

            try {
                certificate = keystore.getCertificate(KEYSTORE_USER_ALIAS);
            } catch (KeyStoreException e) {
                logger.error("An exception ocurred while recovering the ACME user public key : '{}'", e.getMessage());
            }

            if (certificate != null) {
                logger.debug("Recovered the ACME user keypair from the keystore");
                userKeyPair = new KeyPair(certificate.getPublicKey(), (PrivateKey) privateKey);
            } else {
                logger.debug("Generating a new ACME user keypair because of a missing certificate");

                try {
                    userKeyPair = generateKeyPair();
                    privateKey = userKeyPair.getPrivate();
                    isNewKeyPair = true;
                } catch (KeyException e) {
                    logger.error("An exception ocurred while creating a new ACME user key pair : '{}'", e.getMessage());
                }

                try {
                    certificate = generateCertificate(userKeyPair);
                    logger.debug("Putting the ACME user certificate in the keystore");
                    keystore.setKeyEntry(KEYSTORE_USER_ALIAS, privateKey, KEYSTORE_USER_PASSWORD.toCharArray(),
                            new java.security.cert.Certificate[] { certificate });
                    logger.debug("Save the keystore into {}.", keystoreFile.getAbsolutePath());
                    keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_SIGN_PASSWORD.toCharArray());
                } catch (Exception e) {
                    logger.error(
                            "An exception ocurred while creating & storing a certificate for the ACME user key pair : '{}'",
                            e.getMessage());
                }
            }
        }

        // Create a session for Let's Encrypt
        // Use "acme://letsencrypt.org" for production server
        // Use "acme://letsencrypt.org/staging" for staging server
        Session session = new Session("acme://letsencrypt.org", userKeyPair);

        // Register a new user
        Registration reg = null;
        try {
            reg = new RegistrationBuilder().create(session);
            logger.info("Registered a new user, URI: {}", reg.getLocation());
        } catch (AcmeConflictException ex) {
            reg = Registration.bind(session, ex.getLocation());
            logger.info("Account does already exist, URI: {}", reg.getLocation());
        }

        URI agreement = reg.getAgreement();
        logger.info("Terms of Service: " + agreement);
        if (isNewKeyPair) {
            reg.modify().setAgreement(agreement).commit();
        }

        // Create a new authorization
        Authorization auth = null;
        try {
            auth = reg.authorizeDomain(domain);
        } catch (AcmeUnauthorizedException ex) {
            // Accept new terms & conditions, if any
            reg.modify().setAgreement(agreement).commit();
            auth = reg.authorizeDomain(domain);
        }
        logger.info("Obtained an authorization for domain {}", domain);

        challenge = auth.findChallenge(challengeType);
        if (challenge == null) {
            logger.error("Found no {} challenge", challengeType);
            return null;
        }

        if (challenge instanceof Http01Challenge) {
            logger.info(
                    "Let's Encrypt will verify the challenge. Make sure the following URL is reachable: http://{}:{}/.well-known/acme-challenge/{}",
                    new Object[] { domain, HttpServiceUtil.getHttpServicePort(bundleContext),
                            ((Http01Challenge) challenge).getToken() });
        }

        if (challenge instanceof TlsSni01Challenge) {
            String subject = ((TlsSni01Challenge) challenge).getSubject();

            logger.debug("Generating a new keypair for the TLS-SNI challenge");
            KeyPair domainKeyPair = null;
            try {
                domainKeyPair = KeyPairUtils.createKeyPair(2048);
            } catch (Exception e) {
                logger.error("An exception ocurred while creating the TLS-SNI challenge key pair : '{}'",
                        e.getMessage());
                return null;
            }

            X509Certificate cert;
            logger.debug("Generating a new certificate for the TLS-SNI challenge");
            try {
                cert = CertificateUtils.createTlsSniCertificate(domainKeyPair, subject);
            } catch (Exception e) {
                logger.error("An exception occurred while generating a certificate for the TLS-SNI challenge : '{}'",
                        e.getMessage());
                return null;
            }

            // Store the TLS-SNI certificate in the keystore. Jetty >9.3 is capable of returning that when an SNI
            // request is made
            try {
                logger.debug("Storing the new certificate for the TLS-SNI challenge in the keystore");
                keystore.setKeyEntry(KEYSTORE_SNI_ALIAS, privateKey, KEYSTORE_SNI_PASSWORD.toCharArray(),
                        new java.security.cert.Certificate[] { cert });
            } catch (KeyStoreException e) {
                logger.error("An exception ocurred while storing a certificate for the TLS-SNI challenge : '{}'",
                        e.getMessage());
                return null;
            }

            logger.info(
                    "Let's Encrypt will verify the challenge. The TLS-SNI certificate must be returned upon a SNI request to: https://{}:{}/{}",
                    new Object[] { domain, HttpServiceUtil.getHttpServicePortSecure(bundleContext), subject });
        }

        challenge.trigger();

        int attempts = 10;
        while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
            if (challenge.getStatus() == Status.INVALID) {
                logger.error("Challenge verification failed");
                return null;
            }
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException ex) {
                logger.warn("The challenge verification was interrupted");
            }
            challenge.update();
        }
        if (challenge.getStatus() != Status.VALID) {
            logger.error("Challenge verification failed");
            return null;
        }

        if (keyPair != null) {
            CSRBuilder csrb = new CSRBuilder();
            csrb.addDomain(domain);
            csrb.sign(keyPair);

            org.shredzone.acme4j.Certificate signedCertificate = reg.requestCertificate(csrb.getEncoded());
            if (signedCertificate != null) {
                logger.info("The certificate for domain {} has been generated : {}", domain,
                        signedCertificate.getLocation());
                return signedCertificate.download();
            }
        }

        return null;
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
        http((HttpServletRequest) req, (HttpServletResponse) res);
    }

    public void http(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("application/jose+json");
            if (challenge != null && challenge instanceof Http01Challenge) {
                res.getOutputStream().write(((Http01Challenge) challenge).getAuthorization().getBytes());
            }
        } catch (Exception e) {
            logger.error("An exception occurred while serving the challenge response: '{}'", e.getMessage());
            res.setStatus(500);
        }
    }

    @Override
    public void destroy() {
        this.sc = null;
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.sc;
    }

    @Override
    public String getServletInfo() {
        return SERVLET_NAME;
    }

    @Override
    public void init(final ServletConfig v) throws ServletException {
        this.sc = v;
    }

    protected HttpContext createHttpContext() {
        if (httpService != null) {
            HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
            return defaultHttpContext;
        }
        return null;
    }
}
