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
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific serial port manager implementation.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Markus Rathgeb - Respect the possible failure of port identifier creation
 */
@NonNullByDefault
@Component
public class SerialPortManagerImpl implements SerialPortManager {

    private final Logger logger = LoggerFactory.getLogger(SerialPortManagerImpl.class);

    private final SerialPortRegistry registry;

    @Activate
    public SerialPortManagerImpl(final @Reference SerialPortRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Stream<SerialPortIdentifier> getIdentifiers() {
        return registry.getPortCreators().stream().flatMap(provider -> {
            try {
                return provider.getSerialPortIdentifiers();
            } catch (final UnsatisfiedLinkError error) {
                /*
                 * At the time of writing every serial implementation needs some native code.
                 * So missing some native code for a specific platform is a potential error and we should not
                 * break the whole handling just because one of the provider miss some of that code.
                 */
                logger.warn("The provider \"{}\" miss some native code support.", provider.getClass().getSimpleName(),
                        error);
                return Stream.empty();
            } catch (final RuntimeException ex) {
                logger.warn("The provider \"{}\" cannot provide its serial port identifiers.",
                        provider.getClass().getSimpleName(), ex);
                return Stream.empty();
            }
        });
    }

    @Override
    public @Nullable SerialPortIdentifier getIdentifier(String name) {
        final URI portUri = URI.create(name);
        for (final SerialPortProvider provider : registry.getPortProvidersForPortName(portUri)) {
            try {
                return provider.getPortIdentifier(portUri);
            } catch (final UnsatisfiedLinkError error) {
                /*
                 * At the time of writing every serial implementation needs some native code.
                 * So missing some native code for a specific platform is a potential error and we should not
                 * break the whole handling just because one of the provider miss some of that code.
                 */
                logger.warn("The provider \"{}\" miss some native code support.", provider.getClass().getSimpleName(),
                        error);
            } catch (final RuntimeException ex) {
                logger.warn("The provider \"{}\" cannot provide a serial port itendifier for \"{}\".",
                        provider.getClass().getSimpleName(), name, ex);
            }
        }
        logger.warn("No SerialPortProvider found for: {}", name);
        return null;
    }
}
