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
package org.openhab.core.magic.binding.internal.automation.modules;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.ActionScope;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.magic.binding.handler.MagicActionModuleThingHandler;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some automation actions to be used with a {@link MagicActionModuleThingHandler}
 *
 * @author Stefan Triller - Initial contribution
 */
@ActionScope(name = "magic")
public class MagicThingActionsService implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(MagicThingActionsService.class);

    private MagicActionModuleThingHandler handler;

    @RuleAction(label = "Magic thingHandlerAction", description = "Action that calls some logic in a thing handler")
    public @ActionOutput(name = "output1", type = "java.lang.String") @ActionOutput(name = "output2", type = "java.lang.String") Map<String, Object> thingHandlerAction(
            @ActionInput(name = "input1") String input1, @ActionInput(name = "input2") String input2) {
        logger.debug("thingHandlerAction called with inputs: {} {}", input1, input2);

        // one can pass any data to the handler of the selected thing, here we are passing the first input parameter
        // passed into this module via the automation engine
        handler.communicateActionToDevice(input1);

        // hint: one could also put handler results into the output map for further processing within the automation
        // engine
        Map<String, Object> result = new HashMap<>();
        result.put("output1", 23);
        result.put("output2", "myThing");
        return result;
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof MagicActionModuleThingHandler) {
            this.handler = (MagicActionModuleThingHandler) handler;
        }
    }

    @Override
    public ThingHandler getThingHandler() {
        return this.handler;
    }
}
