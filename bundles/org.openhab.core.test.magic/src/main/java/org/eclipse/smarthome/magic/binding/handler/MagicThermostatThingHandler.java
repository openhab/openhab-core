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

import static org.eclipse.smarthome.magic.binding.MagicBindingConstants.*;

import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;

/**
 * A handler for a thermostat thing.
 *
 * @author Henning Treu - Initial contribution
 */
public class MagicThermostatThingHandler extends BaseThingHandler {

    public MagicThermostatThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        if (channelUID.getId().equals(CHANNEL_SET_TEMPERATURE)) {
            if (command instanceof DecimalType || command instanceof QuantityType) {
                String state = command.toFullString() + (command instanceof DecimalType ? " °C" : "");
                scheduler.schedule(() -> {
                    updateState(CHANNEL_TEMPERATURE, new QuantityType<>(state));
                }, 2, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
    }
}
