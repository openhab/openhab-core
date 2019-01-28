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
package org.eclipse.smarthome.config.discovery.usbserial.internal;

import static java.util.stream.Collectors.toSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDiscovery;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DiscoveryService} for discovering USB devices with an associated serial port.
 * <p/>
 * This discovery service is intended to be used by bindings that support USB devices, but do not directly talk to the
 * USB devices but rather use a serial port for the communication, where the serial port is provided by an operating
 * system driver outside the scope of Eclipse SmartHome. Examples for such USB devices are USB dongles that provide
 * access to wireless networks, like, e.g., Zigbeee or Zwave dongles.
 * <p/>
 * This discovery service provides functionality for discovering added and removed USB devices and the corresponding
 * serial ports. The actual {@link DiscoveryResult}s are then provided by {@link UsbSerialDiscoveryParticipant}s, which
 * are called by this discovery service whenever new devices are detected or devices are removed. Such
 * {@link UsbSerialDiscoveryParticipant}s should be provided by bindings accessing USB devices via a serial port.
 * <p/>
 * This discovery service requires a component implementing the interface {@link UsbSerialDiscovery}, which performs the
 * actual serial port and USB device discovery (as this discovery might differ depending on the operating system).
 *
 * @author Henning Sudbrock - initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { DiscoveryService.class,
        UsbSerialDiscoveryService.class }, configurationPid = "discovery.usbserial")
public class UsbSerialDiscoveryService extends AbstractDiscoveryService implements UsbSerialDiscoveryListener {

    private final Logger logger = LoggerFactory.getLogger(UsbSerialDiscoveryService.class);

    private final Set<UsbSerialDiscoveryParticipant> discoveryParticipants = new CopyOnWriteArraySet<>();

    private final Set<UsbSerialDeviceInformation> previouslyDiscovered = new CopyOnWriteArraySet<>();

    private @NonNullByDefault({}) UsbSerialDiscovery usbSerialDiscovery;

    public UsbSerialDiscoveryService() {
        super(5);
    }

    @Override
    @Activate
    protected void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        super.activate(configProperties);
        usbSerialDiscovery.registerDiscoveryListener(this);
    }

    @Modified
    @Override
    protected void modified(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
        super.modified(configProperties);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addUsbSerialDiscoveryParticipant(UsbSerialDiscoveryParticipant participant) {
        this.discoveryParticipants.add(participant);
        for (UsbSerialDeviceInformation usbSerialDeviceInformation : previouslyDiscovered) {
            DiscoveryResult result = participant.createResult(usbSerialDeviceInformation);
            if (result != null) {
                thingDiscovered(result);
            }
        }
    }

    protected void removeUsbSerialDiscoveryParticipant(UsbSerialDiscoveryParticipant participant) {
        this.discoveryParticipants.remove(participant);
    }

    @Reference
    protected void setUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        this.usbSerialDiscovery = usbSerialDiscovery;
    }

    protected synchronized void unsetUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscovery.stopBackgroundScanning();
        usbSerialDiscovery.unregisterDiscoveryListener(this);
        this.usbSerialDiscovery = null;
        this.previouslyDiscovered.clear();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return discoveryParticipants.stream().flatMap(participant -> participant.getSupportedThingTypeUIDs().stream())
                .collect(toSet());
    }

    @Override
    protected void startScan() {
        if (usbSerialDiscovery != null) {
            usbSerialDiscovery.doSingleScan();
        } else {
            logger.info("Could not scan, as there is no USB-Serial discovery service configured.");
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (usbSerialDiscovery != null) {
            usbSerialDiscovery.startBackgroundScanning();
        } else {
            logger.info(
                    "Could not start background discovery, as there is no USB-Serial discovery service configured.");
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        if (usbSerialDiscovery != null) {
            usbSerialDiscovery.stopBackgroundScanning();
        } else {
            logger.info("Could not stop background discovery, as there is no USB-Serial discovery service configured.");
        }
    }

    @Override
    public void usbSerialDeviceDiscovered(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        logger.debug("Discovered new USB-Serial device: {}", usbSerialDeviceInformation);
        previouslyDiscovered.add(usbSerialDeviceInformation);
        for (UsbSerialDiscoveryParticipant participant : discoveryParticipants) {
            DiscoveryResult result = participant.createResult(usbSerialDeviceInformation);
            if (result != null) {
                thingDiscovered(result);
            }
        }
    }

    @Override
    public void usbSerialDeviceRemoved(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        logger.debug("Discovered removed USB-Serial device: {}", usbSerialDeviceInformation);
        previouslyDiscovered.remove(usbSerialDeviceInformation);
        for (UsbSerialDiscoveryParticipant participant : discoveryParticipants) {
            ThingUID thingUID = participant.getThingUID(usbSerialDeviceInformation);
            if (thingUID != null) {
                thingRemoved(thingUID);
            }
        }
    }

}
