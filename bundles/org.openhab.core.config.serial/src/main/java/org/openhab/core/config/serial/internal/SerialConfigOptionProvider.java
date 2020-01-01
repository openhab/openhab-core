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
package org.openhab.core.config.serial.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This service provides serial port names as options for configuration parameters.
 *
 * @author Kai Kreuzer - Initial contribution
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
        List<ParameterOption> options = new ArrayList<>();
        if ("serial-port".equals(context)) {
            serialPortManager.getIdentifiers()
                    .forEach(id -> options.add(new ParameterOption(id.getName(), id.getName())));
        }
        return options;
    }

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, Locale locale) {
        return null;
    }

}
