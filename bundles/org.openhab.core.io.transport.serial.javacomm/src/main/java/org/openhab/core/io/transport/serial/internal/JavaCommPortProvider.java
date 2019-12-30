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
import java.util.Enumeration;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.comm.CommPortIdentifier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.ProtocolType;
import org.openhab.core.io.transport.serial.ProtocolType.PathType;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matthias Steigenberger - Initial contribution
 */
@NonNullByDefault
@Component(service = SerialPortProvider.class)
public class JavaCommPortProvider implements SerialPortProvider {

    private final Logger logger = LoggerFactory.getLogger(JavaCommPortProvider.class);

    @Override
    public @Nullable SerialPortIdentifier getPortIdentifier(URI port) {
        CommPortIdentifier ident = null;
        try {
            ident = CommPortIdentifier.getPortIdentifier(port.getPath());
        } catch (javax.comm.NoSuchPortException e) {
            logger.debug("No SerialPortIdentifier found for: {}", port.getPath());
            return null;
        }
        return new SerialPortIdentifierImpl(ident);
    }

    @Override
    public Stream<ProtocolType> getAcceptedProtocols() {
        return Stream.of(new ProtocolType(PathType.LOCAL, "javacomm"));
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
