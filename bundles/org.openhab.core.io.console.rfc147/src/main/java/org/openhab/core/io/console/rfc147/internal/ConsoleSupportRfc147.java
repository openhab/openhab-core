/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.console.rfc147.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.io.console.rfc147.internal.extension.HelpConsoleCommandExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the console support using the OSGi interface to support commands proposal RFC 147.
 *
 * Read this link to get a short overview about the way to implement commands for RFC 147:
 * https://felix.apache.org/site/rfc-147-overview.html
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = {})
public class ConsoleSupportRfc147 implements ConsoleCommandsContainer {

    // private static final String KEY_SCOPE = CommandProcessor.COMMAND_SCOPE;
    // private static final String KEY_FUNCTION = CommandProcessor.COMMAND_FUNCTION;
    private static final String KEY_SCOPE = "osgi.command.scope";
    private static final String KEY_FUNCTION = "osgi.command.function";

    private static final String SCOPE = "openhab";

    public static final OSGiConsole CONSOLE = new OSGiConsole(SCOPE);

    private final Logger logger = LoggerFactory.getLogger(ConsoleSupportRfc147.class);

    private final HelpConsoleCommandExtension helpCommand = new HelpConsoleCommandExtension();

    private @Nullable BundleContext bundleContext;

    /*
     * This map will contain all console command extensions.
     * The key consists of the reference to the console command extensions.
     * The value is set to null, if the console command extension is not registered, yet (e.g. the bundle context is not
     * known). Otherwise it stores the registered service reference, so we could unregister the command extension later.
     */
    private final Map<ConsoleCommandExtension, @Nullable ServiceRegistration<?>> commands = Collections
            .synchronizedMap(new HashMap<>());

    /**
     * Constructs a new RFC 147 console support instance.
     * Initializes the commands map and registers the built-in help command.
     */
    public ConsoleSupportRfc147() {
        // Add our custom help console command extensions.
        commands.put(helpCommand, null);
    }

    /**
     * Activates this OSGi component.
     * Registers all pending console command extensions as OSGi services and activates the help command.
     *
     * @param ctx the component context provided by OSGi
     */
    @Activate
    public void activate(ComponentContext ctx) {
        // Save bundle context to register services.
        this.bundleContext = ctx.getBundleContext();

        /*
         * The bundle context is available.
         * Register all console command extensions that are not registered before.
         */
        for (Map.Entry<ConsoleCommandExtension, @Nullable ServiceRegistration<?>> entry : commands.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(registerCommand(entry.getKey()));
            }
        }

        // We are activated now, so the help command should be able to fetch all our commands.
        helpCommand.setConsoleCommandsContainer(this);
    }

    /**
     * Deactivates this OSGi component.
     * Unregisters all console command extensions and clears the help command reference.
     */
    @Deactivate
    public void deactivate() {
        // If we get deactivated, remove from help command (so GC could do their work).
        helpCommand.setConsoleCommandsContainer(null);

        /*
         * De-register all previously registered command extensions.
         */
        for (Map.Entry<ConsoleCommandExtension, @Nullable ServiceRegistration<?>> entry : commands.entrySet()) {
            ServiceRegistration<?> value = entry.getValue();
            if (value != null) {
                unregisterCommand(value);
                entry.setValue(null);
            }
        }

        // Remove bundle context reference.
        this.bundleContext = null;
    }

    /**
     * Adds a console command extension to the registry.
     * This method is called dynamically by OSGi when a new console command extension is registered.
     * The command is immediately registered as an OSGi service if the component is active.
     *
     * @param consoleCommandExtension the console command extension to add
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addConsoleCommandExtension(ConsoleCommandExtension consoleCommandExtension) {
        final ServiceRegistration<?> old;

        old = commands.put(consoleCommandExtension, registerCommand(consoleCommandExtension));
        if (old != null) {
            unregisterCommand(old);
        }
    }

    /**
     * Removes a console command extension from the registry.
     * This method is called dynamically by OSGi when a console command extension is unregistered.
     * The command's OSGi service registration is also removed.
     *
     * @param consoleCommandExtension the console command extension to remove
     */
    public void removeConsoleCommandExtension(ConsoleCommandExtension consoleCommandExtension) {
        final ServiceRegistration<?> old;

        old = commands.remove(consoleCommandExtension);
        if (old != null) {
            unregisterCommand(old);
        }
    }

    /**
     * Creates an empty properties dictionary for OSGi service registration.
     *
     * @return an empty dictionary that can be used to store service properties
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Dictionary<String, Object> createProperties() {
        return (Dictionary) new Properties();
    }

    /**
     * Register a console command extension.
     *
     * @param cmd the console command extension that should be registered.
     * @return the service registration reference on success, null if the registration was not successful.
     */
    private @Nullable ServiceRegistration<?> registerCommand(final ConsoleCommandExtension cmd) {
        // We could only register the service if the bundle context is known.
        BundleContext bundleContext = this.bundleContext;
        if (bundleContext == null) {
            return null;
        }

        Dictionary<String, Object> props = createProperties();
        props.put(KEY_SCOPE, SCOPE);
        props.put(KEY_FUNCTION, cmd.getCommand());

        try {
            final ServiceRegistration<?> serviceRegistration;
            serviceRegistration = bundleContext.registerService(CommandWrapper.class.getName(), new CommandWrapper(cmd),
                    props);
            return serviceRegistration;
        } catch (final IllegalStateException ex) {
            logger.trace("Registration failed.");
            return null;
        }
    }

    /**
     * Unregister a service registration.
     *
     * @param serviceRegistration the service registration for the service that should be unregistered.
     */
    private void unregisterCommand(final ServiceRegistration<?> serviceRegistration) {
        try {
            serviceRegistration.unregister();
        } catch (final IllegalStateException ex) {
            logger.trace("Service already unregistered.");
        }
    }

    @Override
    public Collection<ConsoleCommandExtension> getConsoleCommandExtensions() {
        final Set<ConsoleCommandExtension> set = new HashSet<>();

        // Fill set with registered commands only.
        for (Map.Entry<ConsoleCommandExtension, @Nullable ServiceRegistration<?>> entry : commands.entrySet()) {
            if (entry.getValue() != null) {
                set.add(entry.getKey());
            }
        }

        return set;
    }
}
