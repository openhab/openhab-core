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

import static org.openhab.core.magic.binding.MagicBindingConstants.*;

import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;

/**
 * A handler for a thermostat thing.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class MagicThermostatThingHandler extends BaseThingHandler {

    public MagicThermostatThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_SET_TEMPERATURE.equals(channelUID.getId())) {
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
