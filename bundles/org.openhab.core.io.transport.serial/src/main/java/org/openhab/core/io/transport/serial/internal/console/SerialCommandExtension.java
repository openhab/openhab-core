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
package org.openhab.core.io.transport.serial.internal.console;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.internal.SerialPortRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link SerialCommandExtension} provides console commands for serial ports.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
public class SerialCommandExtension extends AbstractConsoleCommandExtension {

    private static final String CMD_SERIAL = "serial";
    private static final String SUBCMD_IDENTIFIER_ALL = "identifiers";
    private static final String SUBCMD_IDENTIFIER_NAME = "identifier";
    private static final String SUBCMD_PORT_CREATORS = "creators";

    private final SerialPortManager serialPortManager;
    private final SerialPortRegistry serialPortRegistry;

    @Activate
    public SerialCommandExtension(final @Reference SerialPortManager serialPortManager,
            final @Reference SerialPortRegistry serialPortRegistry) {
        super(CMD_SERIAL, "Access your serial port interfaces.");
        this.serialPortManager = serialPortManager;
        this.serialPortRegistry = serialPortRegistry;
    }

    @Override
    public void execute(String[] args, Console console) {
        final Deque<String> argList = new LinkedList<>(Arrays.asList(args));
        if (argList.isEmpty()) {
            printUsage(console);
            return;
        }

        final String subCmd = argList.removeFirst();
        switch (subCmd) {
            case SUBCMD_IDENTIFIER_ALL:
                serialPortManager.getIdentifiers().forEach(id -> {
                    console.println(str(id));
                });
                return;
            case SUBCMD_IDENTIFIER_NAME:
                if (argList.isEmpty()) {
                    console.println("Missing name");
                    return;
                }
                final String name = argList.removeFirst();
                console.println(str(serialPortManager.getIdentifier(name)));
                return;
            case SUBCMD_PORT_CREATORS:
                serialPortRegistry.getPortCreators().forEach(provider -> {
                    console.printf("%s, accepted protocols: %s, port identifiers: %s%n", provider.getClass(),
                            provider.getAcceptedProtocols().collect(Collectors.toList()),
                            provider.getSerialPortIdentifiers().map(SerialCommandExtension::str)
                                    .collect(Collectors.toList()));
                });
                return;
            default:
                console.printf("Unknown sub command: %s%n", subCmd);
                return;
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] { //
                buildCommandUsage(SUBCMD_IDENTIFIER_ALL, "lists all identifiers"), //
                buildCommandUsage(SUBCMD_IDENTIFIER_NAME, "lists a specific identifier"), //
                buildCommandUsage(SUBCMD_PORT_CREATORS, "gets details about the port creators") //
        });
    }

    private static String str(final @Nullable SerialPortIdentifier id) {
        if (id == null) {
            return "<null>";
        } else {
            return String.format("[name: %s, current owner: %s]", id.getName(), id.getCurrentOwner());
        }
    }
}
