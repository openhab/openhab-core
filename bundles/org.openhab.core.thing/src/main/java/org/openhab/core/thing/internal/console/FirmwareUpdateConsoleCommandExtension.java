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
package org.openhab.core.thing.internal.console;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.openhab.core.thing.firmware.FirmwareRegistry;
import org.openhab.core.thing.firmware.FirmwareStatusInfo;
import org.openhab.core.thing.firmware.FirmwareUpdateService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * {@link FirmwareUpdateConsoleCommandExtension} provides console commands for managing the firmwares of things.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Christoph Knauf - added cancel command
 * @author Dimitar Ivanov - The listing of the firmwares is done for thing UID
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
@NonNullByDefault
public final class FirmwareUpdateConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_STATUS = "status";
    private static final String SUBCMD_UPDATE = "update";
    private static final String SUBCMD_CANCEL = "cancel";

    private final FirmwareUpdateService firmwareUpdateService;
    private final FirmwareRegistry firmwareRegistry;
    private final ThingRegistry thingRegistry;
    private final List<FirmwareUpdateHandler> firmwareUpdateHandlers = new CopyOnWriteArrayList<>();

    @Activate
    public FirmwareUpdateConsoleCommandExtension(final @Reference FirmwareUpdateService firmwareUpdateService,
            final @Reference FirmwareRegistry firmwareRegistry, final @Reference ThingRegistry thingRegistry) {
        super("firmware", "Manage your things' firmwares.");
        this.firmwareUpdateService = firmwareUpdateService;
        this.firmwareRegistry = firmwareRegistry;
        this.thingRegistry = thingRegistry;
    }

    @Override
    public void execute(String[] args, Console console) {
        int numberOfArguments = args.length;
        if (numberOfArguments < 1) {
            console.println("No firmware subcommand given.");
            printUsage(console);
            return;
        }

        String subCommand = args[0];

        switch (subCommand) {
            case SUBCMD_LIST:
                listFirmwares(console, args);
                break;
            case SUBCMD_STATUS:
                listFirmwareStatus(console, args);
                break;
            case SUBCMD_UPDATE:
                updateFirmware(console, args);
                break;
            case SUBCMD_CANCEL:
                cancelUpdate(console, args);
                break;
            default:
                console.println(String.format("Unknown firmware sub command '%s'.", subCommand));
                printUsage(console);
                break;
        }
    }

    private void listFirmwares(Console console, String[] args) {
        if (args.length != 2) {
            console.println("Specify the thing UID to get its available firmwares: firmware list <thingUID>");
            return;
        }

        ThingUID thingUID = new ThingUID(args[1]);
        Thing thing = thingRegistry.get(thingUID);

        if (thing == null) {
            console.println("There is no present thing with UID " + thingUID);
            return;
        }

        Collection<Firmware> firmwares = firmwareRegistry.getFirmwares(thing);

        if (firmwares.isEmpty()) {
            console.println("No firmwares found for thing with UID " + thingUID);
        }

        for (Firmware firmware : firmwares) {
            console.println(firmware.toString());
        }
    }

    private void listFirmwareStatus(Console console, String[] args) {
        if (args.length != 2) {
            console.println("Specify the thing id to get its firmware status: firmware status <thingUID>");
            return;
        }

        ThingUID thingUID = new ThingUID(args[1]);
        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService.getFirmwareStatusInfo(thingUID);

        if (firmwareStatusInfo != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("Firmware status for thing with UID %s is %s.", thingUID,
                    firmwareStatusInfo.getFirmwareStatus()));

            if (firmwareStatusInfo.getUpdatableFirmwareVersion() != null) {
                sb.append(String.format(" The latest updatable firmware version is %s.",
                        firmwareStatusInfo.getUpdatableFirmwareVersion()));
            }

            console.println(sb.toString());
        } else {
            console.println(
                    String.format("The firmware status for thing with UID %s could not be determined.", thingUID));
        }
    }

    private void cancelUpdate(Console console, String[] args) {
        if (args.length != 2) {
            console.println("Specify the thing id to cancel the update: firmware cancel <thingUID>");
            return;
        }

        ThingUID thingUID = new ThingUID(args[1]);
        FirmwareUpdateHandler firmwareUpdateHandler = getFirmwareUpdateHandler(thingUID);

        if (firmwareUpdateHandler == null) {
            console.println(String.format("No firmware update handler available for thing with UID %s.", thingUID));
            return;
        }

        firmwareUpdateService.cancelFirmwareUpdate(thingUID);
        console.println("Firmware update canceled.");
    }

    private void updateFirmware(Console console, String[] args) {
        if (args.length != 3) {
            console.println(
                    "Specify the thing id and the firmware version to update the firmware: firmware update <thingUID> <firmware version>");
            return;
        }

        ThingUID thingUID = new ThingUID(args[1]);
        FirmwareUpdateHandler firmwareUpdateHandler = getFirmwareUpdateHandler(thingUID);

        if (firmwareUpdateHandler == null) {
            console.println(String.format("No firmware update handler available for thing with UID %s.", thingUID));
            return;
        }

        firmwareUpdateService.updateFirmware(thingUID, args[2], null);
        console.println("Firmware update started.");
    }

    private @Nullable FirmwareUpdateHandler getFirmwareUpdateHandler(ThingUID thingUID) {
        for (FirmwareUpdateHandler firmwareUpdateHandler : firmwareUpdateHandlers) {
            if (thingUID.equals(firmwareUpdateHandler.getThing().getUID())) {
                return firmwareUpdateHandler;
            }
        }
        return null;
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(SUBCMD_LIST + " <thingUID>", "lists the available firmwares for a thing"),
                buildCommandUsage(SUBCMD_STATUS + " <thingUID>", "lists the firmware status for a thing"),
                buildCommandUsage(SUBCMD_CANCEL + " <thingUID>", "cancels the update for a thing"), buildCommandUsage(
                        SUBCMD_UPDATE + " <thingUID> <firmware version>", "updates the firmware for a thing") });
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addFirmwareUpdateHandler(FirmwareUpdateHandler firmwareUpdateHandler) {
        firmwareUpdateHandlers.add(firmwareUpdateHandler);
    }

    protected void removeFirmwareUpdateHandler(FirmwareUpdateHandler firmwareUpdateHandler) {
        firmwareUpdateHandlers.remove(firmwareUpdateHandler);
    }
}
