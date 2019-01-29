/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.transport.mqtt.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnectionEx;
import org.eclipse.smarthome.io.transport.mqtt.MqttService;
import org.eclipse.smarthome.io.transport.mqtt.MqttServiceObserver;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.cm.ConfigurationException;

/**
 * Tests the MqttService class
 *
 * @author David Graeff - Initial contribution
 */
public class MqttServiceTests {
    // Tests addBrokersListener/removeBrokersListener
    @Test
    public void brokerConnectionListenerTests() throws ConfigurationException {
        MqttService service = new MqttServiceImpl();
        assertFalse(service.hasBrokerObservers());
        MqttServiceObserver observer = mock(MqttServiceObserver.class);

        service.addBrokersListener(observer);
        assertTrue(service.hasBrokerObservers());

        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("tcp://123.123.123.123", null, false,
                "brokerConnectionListenerTests");
        assertTrue(service.addBrokerConnection("name", connection));

        ArgumentCaptor<MqttBrokerConnection> argumentCaptorConn = ArgumentCaptor.forClass(MqttBrokerConnection.class);
        ArgumentCaptor<String> argumentCaptorConnName = ArgumentCaptor.forClass(String.class);

        verify(observer).brokerAdded(argumentCaptorConnName.capture(), argumentCaptorConn.capture());
        assertThat(argumentCaptorConnName.getValue(), equalTo("name"));
        assertThat(argumentCaptorConn.getValue(), equalTo(connection));

        service.removeBrokerConnection("name");
        verify(observer).brokerRemoved(argumentCaptorConnName.capture(), argumentCaptorConn.capture());
        assertThat(argumentCaptorConnName.getValue(), equalTo("name"));
        assertThat(argumentCaptorConn.getValue(), equalTo(connection));

        service.removeBrokersListener(observer);
        assertFalse(service.hasBrokerObservers());
    }

    @Test
    public void brokerConnectionAddRemoveEnumerateTests() {
        MqttService service = new MqttServiceImpl();
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("tcp://123.123.123.123", null, false,
                "brokerConnectionAddRemoveEnumerateTests");
        // Add
        assertThat(service.getAllBrokerConnections().size(), is(equalTo(0)));
        assertTrue(service.addBrokerConnection("name", connection));
        assertFalse(service.addBrokerConnection("name", connection));

        // Get/Enumerate
        assertNotNull(service.getBrokerConnection("name"));
        assertThat(service.getAllBrokerConnections().size(), is(equalTo(1)));

        // Remove
        service.removeBrokerConnection("name");
        assertThat(service.getAllBrokerConnections().size(), is(equalTo(0)));
    }
}
