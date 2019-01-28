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
package org.eclipse.smarthome.core.thing.internal.console;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.eclipse.smarthome.core.thing.firmware.FirmwareRegistry;
import org.eclipse.smarthome.core.thing.firmware.FirmwareStatusInfo;
import org.eclipse.smarthome.core.thing.firmware.FirmwareUpdateService;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
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
public final class FirmwareUpdateConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_STATUS = "status";
    private static final String SUBCMD_UPDATE = "update";
    private static final String SUBCMD_CANCEL = "cancel";

    private FirmwareUpdateService firmwareUpdateService;
    private FirmwareRegistry firmwareRegistry;
    private ThingRegistry thingRegistry;
    private final List<FirmwareUpdateHandler> firmwareUpdateHandlers = new CopyOnWriteArrayList<>();

    public FirmwareUpdateConsoleCommandExtension() {
        super("firmware", "Manage your things' firmwares.");
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

    private FirmwareUpdateHandler getFirmwareUpdateHandler(ThingUID thingUID) {
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

    @Reference
    protected void setFirmwareUpdateService(FirmwareUpdateService firmwareUpdateService) {
        this.firmwareUpdateService = firmwareUpdateService;
    }

    protected void unsetFirmwareUpdateService(FirmwareUpdateService firmwareUpdateService) {
        this.firmwareUpdateService = null;
    }

    @Reference
    protected void setFirmwareRegistry(FirmwareRegistry firmwareRegistry) {
        this.firmwareRegistry = firmwareRegistry;
    }

    protected void unsetFirmwareRegistry(FirmwareRegistry firmwareRegistry) {
        this.firmwareRegistry = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addFirmwareUpdateHandler(FirmwareUpdateHandler firmwareUpdateHandler) {
        firmwareUpdateHandlers.add(firmwareUpdateHandler);
    }

    protected void removeFirmwareUpdateHandler(FirmwareUpdateHandler firmwareUpdateHandler) {
        firmwareUpdateHandlers.remove(firmwareUpdateHandler);
    }
}
