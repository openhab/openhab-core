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
package org.eclipse.smarthome.magic.binding.handler;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.magic.binding.internal.automation.modules.MagicThingActionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThingHandler which provides annotated actions that will become Action modules for the automation engine
 *
 * @author Stefan Triller - initial contribution
 *
 */
@NonNullByDefault
public class MagicActionModuleThingHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MagicActionModuleThingHandler.class);

    public MagicActionModuleThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // doing nothing here
    }

    public void communicateActionToDevice(String doSomething) {
        String text = (String) thing.getConfiguration().get("textParam");
        logger.debug("Handler with textParam={} pushes action {}  to device.", text, doSomething);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singletonList(MagicThingActionsService.class);
    }

}
