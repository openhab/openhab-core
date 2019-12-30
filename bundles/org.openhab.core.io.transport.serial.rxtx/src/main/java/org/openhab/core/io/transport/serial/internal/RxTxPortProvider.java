/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.transport.serial.internal;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.ProtocolType;
import org.openhab.core.io.transport.serial.ProtocolType.PathType;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;

/**
 *
 * @author Matthias Steigenberger - Initial contribution
 * @author Wouter Born - Fix serial ports missing when ports are added to system property
 */
@NonNullByDefault
@Component
public class RxTxPortProvider implements SerialPortProvider {

    private final Logger logger = LoggerFactory.getLogger(RxTxPortProvider.class);

    @Override
    public @Nullable SerialPortIdentifier getPortIdentifier(URI port) {
        try {
            CommPortIdentifier ident = SerialPortUtil.getPortIdentifier(port.getPath());
            return new SerialPortIdentifierImpl(ident);
        } catch (NoSuchPortException e) {
            logger.debug("No SerialPortIdentifier found for: {}", port.getPath());
            return null;
        }
    }

    @Override
    public Stream<ProtocolType> getAcceptedProtocols() {
        return Stream.of(new ProtocolType(PathType.LOCAL, "rxtx"));
    }

    @Override
    public Stream<SerialPortIdentifier> getSerialPortIdentifiers() {
        Stream<CommPortIdentifier> scanIds = SerialPortUtil.getPortIdentifiersUsingScan();
        Stream<CommPortIdentifier> propIds = SerialPortUtil.getPortIdentifiersUsingProperty();

        return Stream.concat(scanIds, propIds).filter(distinctByKey(CommPortIdentifier::getName))
                .filter(id -> id.getPortType() == CommPortIdentifier.PORT_SERIAL)
                .map(sid -> new SerialPortIdentifierImpl(sid));
    }

    @SuppressWarnings("null")
    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, String> seen = new ConcurrentHashMap<>();
        return t -> seen.put(keyExtractor.apply(t), "") == null;
    }
}
