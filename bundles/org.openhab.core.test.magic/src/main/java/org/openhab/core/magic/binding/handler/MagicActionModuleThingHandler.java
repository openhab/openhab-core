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
package org.openhab.core.magic.binding.handler;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.magic.binding.internal.automation.modules.MagicThingActionsService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThingHandler which provides annotated actions that will become Action modules for the automation engine
 *
 * @author Stefan Triller - Initial contribution
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
