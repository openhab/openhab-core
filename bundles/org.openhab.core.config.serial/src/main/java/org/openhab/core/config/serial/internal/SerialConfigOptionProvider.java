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
package org.openhab.core.config.serial.internal;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This service provides serial port names as options for configuration parameters.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Add discovered USB serial port names to serial port parameter options
 */
@NonNullByDefault
@Component
public class SerialConfigOptionProvider implements ConfigOptionProvider, UsbSerialDiscoveryListener {

    static final String SERIAL_PORT = "serial-port";

    private final SerialPortManager serialPortManager;
    private final Set<UsbSerialDeviceInformation> previouslyDiscovered = new CopyOnWriteArraySet<>();
    private final Set<UsbSerialDiscovery> usbSerialDiscoveries = new CopyOnWriteArraySet<>();

    @Activate
    public SerialConfigOptionProvider(final @Reference SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscoveries.add(usbSerialDiscovery);
        usbSerialDiscovery.registerDiscoveryListener(this);
    }

    protected synchronized void removeUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscovery.unregisterDiscoveryListener(this);
        usbSerialDiscoveries.remove(usbSerialDiscovery);
        previouslyDiscovered.clear();
    }

    @Override
    public void usbSerialDeviceDiscovered(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        previouslyDiscovered.add(usbSerialDeviceInformation);
    }

    @Override
    public void usbSerialDeviceRemoved(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        previouslyDiscovered.remove(usbSerialDeviceInformation);
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (SERIAL_PORT.equals(context)) {
            return Stream
                    .concat(serialPortManager.getIdentifiers().map(SerialPortIdentifier::getName),
                            previouslyDiscovered.stream().map(UsbSerialDeviceInformation::getSerialPort))
                    .distinct() //
                    .map(serialPortName -> new ParameterOption(serialPortName, serialPortName)) //
                    .collect(Collectors.toList());
        }
        return null;
    }
}
