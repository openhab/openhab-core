/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.console.internal.extension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonService;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.ConsoleCommandCompleter;
import org.openhab.core.io.console.StringsCompleter;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Console command extension to manage add-ons
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class AddonConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_SERVICES = "services";
    private static final String SUBCMD_INSTALL = "install";
    private static final String SUBCMD_UNINSTALL = "uninstall";
    private static final StringsCompleter SUBCMD_COMPLETER = new StringsCompleter(
            List.of(SUBCMD_LIST, SUBCMD_SERVICES, SUBCMD_INSTALL, SUBCMD_UNINSTALL), false);

    private class AddonConsoleCommandCompleter implements ConsoleCommandCompleter {
        @Override
        public boolean complete(String[] args, int cursorArgumentIndex, int cursorPosition, List<String> candidates) {
            if (cursorArgumentIndex <= 0) {
                return SUBCMD_COMPLETER.complete(args, cursorArgumentIndex, cursorPosition, candidates);
            }
            return false;
        }
    }

    private final Map<String, AddonService> addonServices = new ConcurrentHashMap<>();

    @Activate
    public AddonConsoleCommandExtension() {
        super("addons", "Manage add-ons.");
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindAddonService(AddonService addonService) {
        addonServices.put(addonService.getId(), addonService);
    }

    public void unbindAddonService(AddonService addonService) {
        addonServices.remove(addonService.getId());
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_SERVICES, "list all available add-on services"),
                buildCommandUsage(SUBCMD_LIST + " [<serviceId>]",
                        "lists names of all add-ons (from the named service, if given)"),
                buildCommandUsage(SUBCMD_INSTALL + " <addonUid>", "installs the given add-on"),
                buildCommandUsage(SUBCMD_UNINSTALL + " <addonUid>", "uninstalls the given add-on"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_SERVICES:
                    listServices(console);
                    break;
                case SUBCMD_LIST:
                    listAddons(console, (args.length < 2) ? "" : args[1]);
                    break;
                case SUBCMD_INSTALL:
                    if (args.length == 2) {
                        installAddon(console, args[1]);
                    } else {
                        console.println("Specify the UID of the add-on to install: " + getCommand() + " "
                                + SUBCMD_INSTALL + " <addonUid>");
                    }
                    break;
                case SUBCMD_UNINSTALL:
                    if (args.length == 2) {
                        uninstallAddon(console, args[1]);
                    } else {
                        console.println("Specify the UID of the add-on to uninstall: " + getCommand() + " "
                                + SUBCMD_UNINSTALL + " <addonUid>");
                    }
                    break;
                default:
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    @Override
    public @Nullable ConsoleCommandCompleter getCompleter() {
        return new AddonConsoleCommandCompleter();
    }

    private void listServices(Console console) {
        addonServices.values().forEach(s -> console.println(String.format("%-20s %s", s.getId(), s.getName())));
    }

    private void listAddons(Console console, String serviceId) {
        List<Addon> addons;
        if (serviceId.isBlank()) {
            addons = addonServices.values().stream().map(s -> s.getAddons(null)).flatMap(List::stream).toList();
        } else {
            AddonService service = addonServices.get(serviceId);
            if (service == null) {
                console.println("Add-on service '" + serviceId + "' is not known.");
                return;
            }
            addons = service.getAddons(null);
        }
        addons.forEach(addon -> console.println(String.format("%s %-45s %-20s %s", addon.isInstalled() ? "i" : " ",
                addon.getUid(), addon.getVersion().isBlank() ? "not set" : addon.getVersion(), addon.getLabel())));
    }

    private void installAddon(Console console, String addonUid) {
        String[] parts = addonUid.split(":");
        String serviceId = parts.length == 2 ? parts[0] : "karaf";
        String addonId = parts.length == 2 ? parts[1] : parts[0];
        AddonService service = addonServices.get(serviceId);
        if (service == null) {
            console.println("Could not find requested add-on service. Add-on " + addonUid + " not installed.");
        } else {
            Addon addon = service.getAddon(addonId, null);
            if (addon == null) {
                console.println("Could not find add-on in add-on service. Add-on " + addonUid + " not installed.");
            } else if (addon.isInstalled()) {
                console.println("Add-on " + addonUid + " is already installed.");
            } else {
                service.install(addonId);
                console.println("Installed " + addonUid + ".");
            }
        }
    }

    private void uninstallAddon(Console console, String addonUid) {
        String[] parts = addonUid.split(":");
        String serviceId = parts.length == 2 ? parts[0] : "karaf";
        String addonId = parts.length == 2 ? parts[1] : parts[0];
        AddonService service = addonServices.get(serviceId);
        if (service == null) {
            console.println("Could not find requested add-on service. Add-on " + addonUid + " not uninstalled.");
        } else {
            Addon addon = service.getAddon(addonId, null);
            if (addon == null) {
                console.println("Could not find add-on in add-on service. Add-on " + addonUid + " not uninstalled.");
            } else if (!addon.isInstalled()) {
                console.println("Add-on " + addonUid + " is not installed.");
            } else {
                service.uninstall(addonId);
                console.println("Uninstalled " + addonUid + ".");
            }
        }
    }
}
