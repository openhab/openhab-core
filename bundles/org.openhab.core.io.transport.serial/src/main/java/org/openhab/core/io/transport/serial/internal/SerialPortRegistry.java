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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.serial.ProtocolType.PathType;
import org.openhab.core.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Registers all {@link SerialPortProvider}s which can be accessed here.
 *
 * @author Matthias Steigenberger - Initial contribution
 * @author Markus Rathgeb - Respect the possible failure of port identifier creation
 */
@Component(service = SerialPortRegistry.class)
@NonNullByDefault
public class SerialPortRegistry {

    private @NonNullByDefault({}) final Collection<SerialPortProvider> portCreators;

    public SerialPortRegistry() {
        this.portCreators = new HashSet<>();
    }

    /**
     * Registers a {@link SerialPortProvider}.
     *
     * @param creator
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void registerSerialPortCreator(SerialPortProvider creator) {
        synchronized (this.portCreators) {
            this.portCreators.add(creator);
        }
    }

    protected void unregisterSerialPortCreator(SerialPortProvider creator) {
        synchronized (this.portCreators) {
            this.portCreators.remove(creator);
        }
    }

    /**
     * Gets the best applicable {@link SerialPortProvider} for the given <code>portName</code>
     *
     * @param portName The port's name.
     * @return all possible {@link SerialPortProvider}. If no provider is available an empty collection is returned
     */
    public Collection<SerialPortProvider> getPortProvidersForPortName(URI portName) {
        final String scheme = portName.getScheme();
        final PathType pathType = PathType.fromURI(portName);

        final Predicate<SerialPortProvider> filter;
        if (scheme != null) {
            // Get port providers which accept exactly the port with its scheme.
            filter = provider -> provider.getAcceptedProtocols().filter(prot -> prot.getScheme().equals(scheme))
                    .count() > 0;
        } else {
            // Get port providers which accept the same type (local, net)
            filter = provider -> provider.getAcceptedProtocols().filter(prot -> prot.getPathType().equals(pathType))
                    .count() > 0;
        }

        return portCreators.stream().filter(filter).collect(Collectors.toList());
    }

    public Collection<SerialPortProvider> getPortCreators() {
        synchronized (this.portCreators) {
            return Collections.unmodifiableCollection(new HashSet<>(portCreators));
        }
    }
}
