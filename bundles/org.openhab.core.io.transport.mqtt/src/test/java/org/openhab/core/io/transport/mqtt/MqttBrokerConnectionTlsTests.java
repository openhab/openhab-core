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
package org.openhab.core.io.transport.mqtt;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.net.ssl.KeyManagerFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection.MqttVersion;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection.Protocol;
import org.openhab.core.io.transport.mqtt.internal.client.MqttAsyncClientWrapper;

import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

/**
 * Regression tests for the propagation of the {@link KeyManagerFactory} (TLS client authentication) from
 * {@link MqttBrokerConnection} into the TLS configuration of the real MQTT 3 and MQTT 5 clients.
 * In contrast to {@link MqttBrokerConnectionTests}, the real client is constructed here (no mocked createClient()).
 *
 * @author Michael Loercher - Initial contribution
 */
@NonNullByDefault
public class MqttBrokerConnectionTlsTests {
    private static final String HOST = "localhost";
    private static final String CLIENT_ID = "MqttBrokerConnectionTlsTests";

    private static KeyManagerFactory newKeyManagerFactory() throws NoSuchAlgorithmException {
        return KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    }

    private static MqttBrokerConnection newConnection(MqttVersion version, boolean secure, boolean hostnameValidated) {
        return new MqttBrokerConnection(Protocol.TCP, version, HOST, 8883, secure, hostnameValidated, CLIENT_ID);
    }

    /**
     * Returns the SSL configuration of the real HiveMQ client wrapped by the given wrapper.
     */
    private static Optional<MqttClientSslConfig> sslConfigOf(MqttAsyncClientWrapper wrapper, MqttVersion version)
            throws ReflectiveOperationException {
        Field field = wrapper.getClass().getDeclaredField("client");
        field.setAccessible(true);
        Object client = field.get(wrapper);
        if (version == MqttVersion.V3) {
            return ((Mqtt3AsyncClient) client).getConfig().getSslConfig();
        }
        return ((Mqtt5AsyncClient) client).getConfig().getSslConfig();
    }

    private static MqttClientSslConfig requireSslConfig(MqttBrokerConnection connection, MqttVersion version)
            throws ReflectiveOperationException {
        Optional<MqttClientSslConfig> sslConfig = sslConfigOf(connection.createClient(), version);
        assertTrue(sslConfig.isPresent(), "secure connection must have an SSL configuration");
        return sslConfig.get();
    }

    private void assertKeyManagerFactorySet(MqttVersion version, boolean hostnameValidated) throws Exception {
        KeyManagerFactory kmf = newKeyManagerFactory();
        MqttBrokerConnection connection = newConnection(version, true, hostnameValidated);
        connection.setKeyManagerFactory(kmf);

        MqttClientSslConfig sslConfig = requireSslConfig(connection, version);

        assertSame(kmf, sslConfig.getKeyManagerFactory().orElse(null));
        assertEquals(hostnameValidated, sslConfig.getHostnameVerifier().isEmpty(),
                "hostname verifier must only be set if hostname validation is disabled");
    }

    private void assertKeyManagerFactoryUnset(MqttVersion version, boolean hostnameValidated) throws Exception {
        MqttBrokerConnection connection = newConnection(version, true, hostnameValidated);

        MqttClientSslConfig sslConfig = requireSslConfig(connection, version);

        assertTrue(sslConfig.getKeyManagerFactory().isEmpty());
        assertEquals(hostnameValidated, sslConfig.getHostnameVerifier().isEmpty());
    }

    @Test
    public void mqtt3KeyManagerFactoryWithHostnameValidation() throws Exception {
        assertKeyManagerFactorySet(MqttVersion.V3, true);
    }

    @Test
    public void mqtt3KeyManagerFactoryWithoutHostnameValidation() throws Exception {
        assertKeyManagerFactorySet(MqttVersion.V3, false);
    }

    @Test
    public void mqtt5KeyManagerFactoryWithHostnameValidation() throws Exception {
        assertKeyManagerFactorySet(MqttVersion.V5, true);
    }

    @Test
    public void mqtt5KeyManagerFactoryWithoutHostnameValidation() throws Exception {
        assertKeyManagerFactorySet(MqttVersion.V5, false);
    }

    @Test
    public void mqtt3UnsetKeyManagerFactory() throws Exception {
        assertKeyManagerFactoryUnset(MqttVersion.V3, true);
        assertKeyManagerFactoryUnset(MqttVersion.V3, false);
    }

    @Test
    public void mqtt5UnsetKeyManagerFactory() throws Exception {
        assertKeyManagerFactoryUnset(MqttVersion.V5, true);
        assertKeyManagerFactoryUnset(MqttVersion.V5, false);
    }

    @Test
    public void clearedKeyManagerFactoryIsNotPropagated() throws Exception {
        for (MqttVersion version : MqttVersion.values()) {
            MqttBrokerConnection connection = newConnection(version, true, true);
            connection.setKeyManagerFactory(newKeyManagerFactory());
            connection.setKeyManagerFactory(null);

            assertTrue(requireSslConfig(connection, version).getKeyManagerFactory().isEmpty(),
                    "cleared factory must not reach the TLS configuration for " + version);
        }
    }

    @Test
    public void nonSecureConnectionHasNoSslConfigEvenWithKeyManagerFactory() throws Exception {
        for (MqttVersion version : MqttVersion.values()) {
            MqttBrokerConnection connection = newConnection(version, false, true);
            connection.setKeyManagerFactory(newKeyManagerFactory());

            Optional<MqttClientSslConfig> sslConfig = sslConfigOf(connection.createClient(), version);
            assertTrue(sslConfig.isEmpty(), "non-secure connection must not use TLS for " + version);
        }
    }
}
