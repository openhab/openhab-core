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
package org.eclipse.smarthome.automation.internal.sample.json.internal.handler;

import java.util.Arrays;
import java.util.List;

import org.eclipse.smarthome.automation.handler.TriggerHandler;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.AbstractConsoleCommandExtension;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Text console commands to list and execute the created sample trigger handlers
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
public class SampleHandlerFactoryCommands extends AbstractConsoleCommandExtension {

    private static final String CMD = "autotype";
    private static final String DESC = "Automation Sample Handler Factory Management";

    private static final String COMMAND_LIST = "listTrigger";
    private static final String COMMAND_EXECUTE = "executeTrigger";

    private List<TriggerHandler> currentTriggers;
    private final SampleHandlerFactory sampleHandlerFactory;
    private final ServiceRegistration<?> commandsServiceReg;

    /**
     * Constructs the SampleHandlerFactoryCommands
     *
     * @param sampleHandlerFactory HandlerFactory
     * @param bc bundleContext
     */
    public SampleHandlerFactoryCommands(SampleHandlerFactory sampleHandlerFactory, BundleContext bc) {
        super(CMD, DESC);
        this.sampleHandlerFactory = sampleHandlerFactory;
        commandsServiceReg = bc.registerService(ConsoleCommandExtension.class.getName(), this, null);
    }

    @Override
    public void execute(String[] args, Console console) {
        String command = args[0];

        String[] params = new String[args.length - 1];// extract the remaining arguments except the first one
        if (params.length > 0) {
            System.arraycopy(args, 1, params, 0, params.length);
        }

        if (COMMAND_LIST.equalsIgnoreCase(command) || "ls".equalsIgnoreCase(command)) {
            listTriggerHandlers(params, console);
        } else if (COMMAND_EXECUTE.equalsIgnoreCase(command) || "ex".equalsIgnoreCase(command)) {
            executeTriggerHandler(params, console);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] { buildCommandUsage(COMMAND_LIST, "List all created TriggerHandler"),
                buildCommandUsage(COMMAND_EXECUTE, "Executes specific TriggerHandler by its index.") });
    }

    /**
     * Dispose resources.
     */
    public void stop() {
        commandsServiceReg.unregister();
    }

    private void listTriggerHandlers(String[] params, Console console) {
        console.println("ID                             Rule                                 Trigger");
        console.println("-------------------------------------------------------------------------------------------");
        currentTriggers = sampleHandlerFactory.getCreatedTriggerHandler();
        if (currentTriggers.size() > 0) {
            for (int i = 0; i < currentTriggers.size(); i++) {
                SampleTriggerHandler triggerHandler = (SampleTriggerHandler) currentTriggers.get(i);
                console.print(Integer.toString(i + 1));
                console.print("                            ");
                console.print(triggerHandler.getRuleUID());
                console.print("                            ");
                console.println(triggerHandler.getTriggerID());
            }
        } else {
            console.println("No created TriggerHandler. List is Empty");
        }
    }

    private void executeTriggerHandler(String[] params, Console console) {
        if (params.length >= 1) {
            if (currentTriggers == null || currentTriggers.isEmpty()) {
                currentTriggers = sampleHandlerFactory.getCreatedTriggerHandler();
            }
            int index = Integer.parseInt(params[0]);
            String param = null;
            if (currentTriggers.size() >= index) {
                SampleTriggerHandler triggerHandler = (SampleTriggerHandler) currentTriggers.get(index - 1);
                if (params.length >= 2) {
                    param = params[1];
                }
                triggerHandler.trigger(param);
            }
        }
    }
}
