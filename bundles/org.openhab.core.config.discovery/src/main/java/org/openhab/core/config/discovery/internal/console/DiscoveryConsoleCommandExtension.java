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
package org.openhab.core.config.discovery.internal.console;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.ThingTypeUID;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DiscoveryConsoleCommandExtension} provides console commands for thing discovery.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Dennis Nobel - Added background discovery commands
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
@NonNullByDefault
public class DiscoveryConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_START = "start";
    private static final String SUBCMD_BACKGROUND_DISCOVERY_ENABLE = "enableBackgroundDiscovery";
    private static final String SUBCMD_BACKGROUND_DISCOVERY_DISABLE = "disableBackgroundDiscovery";

    private final Logger logger = LoggerFactory.getLogger(DiscoveryConsoleCommandExtension.class);

    private final DiscoveryServiceRegistry discoveryServiceRegistry;
    private final ConfigurationAdmin configurationAdmin;

    @Activate
    public DiscoveryConsoleCommandExtension(final @Reference DiscoveryServiceRegistry discoveryServiceRegistry,
            final @Reference ConfigurationAdmin configurationAdmin) {
        super("discovery", "Control the discovery mechanism.");
        this.discoveryServiceRegistry = discoveryServiceRegistry;
        this.configurationAdmin = configurationAdmin;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_START:
                    if (args.length > 1) {
                        String arg1 = args[1];
                        if (arg1.contains(":")) {
                            ThingTypeUID thingTypeUID = new ThingTypeUID(arg1);
                            runDiscoveryForThingType(console, thingTypeUID);
                        } else {
                            runDiscoveryForBinding(console, arg1);
                        }
                    } else {
                        console.println("Specify thing type id or binding id to discover: discovery "
                                + "start <thingTypeUID|bindingID> (e.g. \"hue:bridge\" or \"hue\")");
                    }
                    return;
                case SUBCMD_BACKGROUND_DISCOVERY_ENABLE:
                    if (args.length > 1) {
                        String discoveryServiceName = args[1];
                        configureBackgroundDiscovery(console, discoveryServiceName, true);
                    } else {
                        console.println("Specify discovery service PID to configure background discovery: discovery "
                                + "enableBackgroundDiscovery <PID> (e.g. \"hue.discovery\")");
                    }
                    return;
                case SUBCMD_BACKGROUND_DISCOVERY_DISABLE:
                    if (args.length > 1) {
                        String discoveryServiceName = args[1];
                        configureBackgroundDiscovery(console, discoveryServiceName, false);
                    } else {
                        console.println("Specify discovery service PID to configure background discovery: discovery "
                                + "disableBackgroundDiscovery <PID> (e.g. \"hue.discovery\")");
                    }
                    return;
                default:
                    break;
            }
        } else {
            console.println(getUsages().get(0));
        }
    }

    private void configureBackgroundDiscovery(Console console, String discoveryServicePID, boolean enabled) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(discoveryServicePID);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                properties = new Hashtable<>();
            }
            properties.put(DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY, enabled);
            configuration.update(properties);
            console.println("Background discovery for discovery service '" + discoveryServicePID + "' was set to "
                    + enabled + ".");
        } catch (IOException ex) {
            String errorText = "Error occurred while trying to configure background discovery with PID '"
                    + discoveryServicePID + "': " + ex.getMessage();
            logger.error(errorText, ex);
            console.println(errorText);
        }
    }

    private void runDiscoveryForThingType(Console console, ThingTypeUID thingTypeUID) {
        discoveryServiceRegistry.startScan(thingTypeUID, null);
    }

    private void runDiscoveryForBinding(Console console, String bindingId) {
        discoveryServiceRegistry.startScan(bindingId, null);
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(SUBCMD_START + " <thingTypeUID|bindingID>",
                        "runs a discovery on a given thing type or binding"),
                buildCommandUsage(SUBCMD_BACKGROUND_DISCOVERY_ENABLE + " <PID>",
                        "enables background discovery for the discovery service with the given PID"),
                buildCommandUsage(SUBCMD_BACKGROUND_DISCOVERY_DISABLE + " <PID>",
                        "disables background discovery for the discovery service with the given PID") });
    }
}
