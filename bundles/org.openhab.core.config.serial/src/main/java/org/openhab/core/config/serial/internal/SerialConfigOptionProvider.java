/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

    /**
     * Creates a new SerialConfigOptionProvider.
     * This constructor is called by the OSGi framework during component activation.
     *
     * @param serialPortManager the serial port manager service used to retrieve available serial ports
     */
    @Activate
    public SerialConfigOptionProvider(final @Reference SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    /**
     * Dynamically adds a USB serial discovery service.
     * This method is called by the OSGi framework when a new {@link UsbSerialDiscovery} service becomes available.
     * The discovery service is registered as a listener to receive notifications about USB serial device
     * additions and removals.
     *
     * <p>
     * This method is synchronized to prevent race conditions with {@link #removeUsbSerialDiscovery(UsbSerialDiscovery)}
     * when services are dynamically bound and unbound.
     *
     * @param usbSerialDiscovery the USB serial discovery service to add (must not be null)
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscoveries.add(usbSerialDiscovery);
        usbSerialDiscovery.registerDiscoveryListener(this);
    }

    /**
     * Dynamically removes a USB serial discovery service.
     * This method is called by the OSGi framework when a {@link UsbSerialDiscovery} service becomes unavailable.
     * The discovery service is unregistered as a listener and removed from the active discovery set.
     *
     * <p>
     * <b>Note:</b> This method clears all previously discovered USB serial devices, not just those
     * discovered by this specific service. This ensures a clean state when discovery services are
     * dynamically removed and re-added, preventing stale device information.
     *
     * <p>
     * This method is synchronized to prevent race conditions with {@link #addUsbSerialDiscovery(UsbSerialDiscovery)}
     * when services are dynamically bound and unbound.
     *
     * @param usbSerialDiscovery the USB serial discovery service to remove (must not be null)
     */
    protected synchronized void removeUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscovery.unregisterDiscoveryListener(this);
        usbSerialDiscoveries.remove(usbSerialDiscovery);
        previouslyDiscovered.clear();
    }

    /**
     * Called when a USB serial device is discovered.
     * This method is invoked by {@link UsbSerialDiscovery} services when they detect a new USB serial device.
     * The discovered device is added to the internal cache and will be included in the parameter options
     * returned by {@link #getParameterOptions(URI, String, String, Locale)}.
     *
     * @param usbSerialDeviceInformation information about the discovered USB serial device
     */
    @Override
    public void usbSerialDeviceDiscovered(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        previouslyDiscovered.add(usbSerialDeviceInformation);
    }

    /**
     * Called when a USB serial device is removed.
     * This method is invoked by {@link UsbSerialDiscovery} services when they detect that a USB serial device
     * has been disconnected. The device is removed from the internal cache and will no longer be included
     * in the parameter options.
     *
     * @param usbSerialDeviceInformation information about the removed USB serial device
     */
    @Override
    public void usbSerialDeviceRemoved(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        previouslyDiscovered.remove(usbSerialDeviceInformation);
    }

    /**
     * Provides serial port names as configuration parameter options.
     * This method is called by the configuration framework to populate serial port selection dropdowns
     * in the UI. It combines serial ports from both the {@link SerialPortManager} and any USB serial
     * devices discovered through {@link UsbSerialDiscovery} services.
     *
     * @param uri the URI of the configuration (not used in this implementation)
     * @param param the parameter name (not used in this implementation)
     * @param context the parameter context; returns serial port options only if context equals "serial-port"
     * @param locale the locale for internationalization (not used in this implementation)
     * @return a collection of parameter options containing available serial port names,
     *         or {@code null} if the context is not "serial-port"
     */
    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (SERIAL_PORT.equals(context)) {
            return Stream
                    .concat(serialPortManager.getIdentifiers().map(SerialPortIdentifier::getName),
                            previouslyDiscovered.stream().map(UsbSerialDeviceInformation::getSerialPort))
                    .filter(serialPortName -> serialPortName != null && !serialPortName.isEmpty()) //
                    .distinct() //
                    .map(serialPortName -> new ParameterOption(serialPortName, serialPortName)) //
                    .toList();
        }
        return null;
    }
}
