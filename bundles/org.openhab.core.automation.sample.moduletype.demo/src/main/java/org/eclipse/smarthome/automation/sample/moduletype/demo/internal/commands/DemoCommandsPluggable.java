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
package org.eclipse.smarthome.automation.sample.moduletype.demo.internal.commands;

import java.util.Arrays;
import java.util.List;

import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

/**
 * This class provides functionality for defining and executing automation commands for importing, exporting, removing
 * and listing the automation objects.
 *
 * @author Plamen Peev - Initial contribution
 */
@Component(immediate = true, service = ConsoleCommandExtension.class)
public class DemoCommandsPluggable extends AbstractConsoleCommandExtension {

    /**
     * This constant defines the commands group name.
     */
    public static final String NAME = "atmdemo";

    /**
     * This constant describes the commands group.
     */
    public static final String DESCRIPTION = "Group of commands for the sample module type demo.";

    /**
     * {@link PostEventCommand} uses this reference.
     */
    static EventAdmin eventAdmin;

    public DemoCommandsPluggable() {
        super(NAME, DESCRIPTION);
    }

    /**
     * Activating this component - called from DS.
     *
     * @param componentContext
     */
    @Activate
    protected void activate(ComponentContext componentContext) {
    }

    /**
     * Deactivating this component - called from DS.
     */
    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        DemoCommandsPluggable.eventAdmin = null;
    }

    /**
     * This method is called when {@link EventAdmin} service becomes available.
     * It set the 'eventAdmin' field to points to this service, so it can be used from {@link DemoCommands}.
     *
     * @param eventAdmin - a reference to the {@link EventAdmin} service.
     */
    @Reference
    protected void setEventAdmin(EventAdmin eventAdmin) {
        DemoCommandsPluggable.eventAdmin = eventAdmin;
    }

    /**
     * This method is called when the {@link EventAdmin} service becomes unavailable.
     *
     * @param eventAdmin - a reference to the service.
     */
    protected void unsetEventAdmin(EventAdmin eventAdmin) {
        DemoCommandsPluggable.eventAdmin = null;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length == 0) {
            List<String> usages = getUsages();
            StringBuilder commandUsages = new StringBuilder();
            for (String usage : usages) {
                commandUsages.append(usage);
                commandUsages.append('\n');
            }
            console.println(commandUsages.toString());
            return;
        }
        String command = args[0];
        String[] params = new String[args.length - 1];
        if (params.length > 0) {
            System.arraycopy(args, 1, params, 0, params.length);
        }
        String res = executeCommand(command, params);
        if (res == null) {
            console.println(String.format("Unsupported command %s", command));
        } else {
            console.println(res);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] { buildCommandUsage(PostEventCommand.SYNTAX, PostEventCommand.DESCRIPTION) });
    }

    /**
     * This method is used to return the correct {@link DemoCommand}
     * instance for execution.
     *
     * @param command - the name of the command.
     * @param params - array of strings which are basis for initialising
     *            the options and parameters of the command.
     * @return
     */
    private DemoCommand parseCommand(String command, String[] params) {
        if (command.equalsIgnoreCase(PostEventCommand.POST_EVENT)
                || command.equalsIgnoreCase(PostEventCommand.POST_EVENT_SHORT)) {
            return new PostEventCommand(params);
        }
        return null;
    }

    /**
     * This method is responsible for execution of every particular command and to return the result of the execution.
     *
     * @param command - the name of the command.
     * @param parameterValues - array of strings which are basis for initialising the options and parameters of the
     *            command.
     * @return understandable for the user message containing information on the outcome of the command.
     */
    private String executeCommand(String command, String[] parameterValues) {
        DemoCommand commandInst = parseCommand(command, parameterValues);
        if (commandInst != null) {
            return commandInst.execute();
        }
        return String.format("Command \"%s\" is not supported!", command);
    }
}
