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
package org.eclipse.smarthome.io.transport.serial.internal;

import java.net.URI;
import java.util.Enumeration;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.serial.ProtocolType;
import org.eclipse.smarthome.io.transport.serial.ProtocolType.PathType;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;

/**
 *
 * @author Matthias Steigenberger - Initial Contribution
 *
 */
@NonNullByDefault
@Component
public class RxTxPortProvider implements SerialPortProvider {

    private final Logger logger = LoggerFactory.getLogger(RxTxPortProvider.class);

    @Override
    public @Nullable SerialPortIdentifier getPortIdentifier(URI port) {
        CommPortIdentifier ident = null;
        if ((System.getProperty("os.name").toLowerCase().indexOf("linux") != -1)) {
            SerialPortUtil.appendSerialPortProperty(port.getPath());
        }
        try {
            ident = CommPortIdentifier.getPortIdentifier(port.getPath());
        } catch (gnu.io.NoSuchPortException e) {
            logger.debug("No SerialPortIdentifier found for: {}", port.getPath());
            return null;
        }
        return new SerialPortIdentifierImpl(ident);

    }

    @Override
    public Stream<ProtocolType> getAcceptedProtocols() {
        return Stream.of(new ProtocolType(PathType.LOCAL, "rxtx"));
    }

    @Override
    public Stream<SerialPortIdentifier> getSerialPortIdentifiers() {
        @SuppressWarnings("unchecked")
        final Enumeration<CommPortIdentifier> ids = CommPortIdentifier.getPortIdentifiers();
        return StreamSupport.stream(new SplitIteratorForEnumeration<>(ids), false)
                .filter(id -> id.getPortType() == CommPortIdentifier.PORT_SERIAL)
                .map(sid -> new SerialPortIdentifierImpl(sid));
    }

    private static class SplitIteratorForEnumeration<T> extends Spliterators.AbstractSpliterator<T> {
        private final Enumeration<T> e;

        public SplitIteratorForEnumeration(final Enumeration<T> e) {
            super(Long.MAX_VALUE, Spliterator.ORDERED);
            this.e = e;
        }

        @Override
        @NonNullByDefault({})
        public boolean tryAdvance(Consumer<? super T> action) {
            if (e.hasMoreElements()) {
                action.accept(e.nextElement());
                return true;
            }
            return false;
        }

        @Override
        @NonNullByDefault({})
        public void forEachRemaining(Consumer<? super T> action) {
            while (e.hasMoreElements()) {
                action.accept(e.nextElement());
            }
        }
    }
}
