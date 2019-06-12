/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.config.serial.internal;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.eclipse.smarthome.config.core.ConfigOptionProvider;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This service provides serial port names as options for configuration parameters.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Component
public class SerialConfigOptionProvider implements ConfigOptionProvider {

    private SerialPortManager serialPortManager;

    @Reference
    protected void setSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    protected void unsetSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = null;
    }

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, String context, Locale locale) {
        if ("serial-port".equals(context)) {
            return serialPortManager.getIdentifiers()
                    .sorted((id1, id2) -> id1.getName().compareToIgnoreCase(id2.getName()))
                    .map(id -> new ParameterOption(id.getName(), id.getName())).collect(toList());
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, Locale locale) {
        return null;
    }

}
