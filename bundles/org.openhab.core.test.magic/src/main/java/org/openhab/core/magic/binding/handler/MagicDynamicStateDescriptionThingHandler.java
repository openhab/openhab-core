/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.magic.binding.internal.MagicDynamicCommandDescriptionProvider;
import org.openhab.core.magic.binding.internal.MagicDynamicStateDescriptionProvider;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.StateOption;

/**
 * ThingHandler which provides channels with dynamic state descriptions.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class MagicDynamicStateDescriptionThingHandler extends BaseThingHandler {

    private static final String SYSTEM_COMMAND_HIBERNATE = "Hibernate";
    private static final String SYSTEM_COMMAND_REBOOT = "Reboot";
    private static final String SYSTEM_COMMAND_SHUTDOWN = "Shutdown";
    private static final String SYSTEM_COMMAND_SUSPEND = "Suspend";
    private static final String SYSTEM_COMMAND_QUIT = "Quit";

    private final MagicDynamicCommandDescriptionProvider commandDescriptionProvider;
    private final MagicDynamicStateDescriptionProvider stateDescriptionProvider;

    public MagicDynamicStateDescriptionThingHandler(Thing thing,
            MagicDynamicCommandDescriptionProvider commandDescriptionProvider,
            MagicDynamicStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
        this.commandDescriptionProvider = commandDescriptionProvider;
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    @Override
    public void initialize() {
        ChannelUID systemCommandChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_SYSTEM_COMMAND);
        commandDescriptionProvider.setCommandOptions(systemCommandChannelUID,
                List.of(new CommandOption(SYSTEM_COMMAND_HIBERNATE, SYSTEM_COMMAND_HIBERNATE),
                        new CommandOption(SYSTEM_COMMAND_REBOOT, SYSTEM_COMMAND_REBOOT),
                        new CommandOption(SYSTEM_COMMAND_SHUTDOWN, SYSTEM_COMMAND_SHUTDOWN),
                        new CommandOption(SYSTEM_COMMAND_SUSPEND, SYSTEM_COMMAND_SUSPEND),
                        new CommandOption(SYSTEM_COMMAND_QUIT, SYSTEM_COMMAND_QUIT)));

        ChannelUID signalStrengthChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_SIGNAL_STRENGTH);
        stateDescriptionProvider.setStateOptions(signalStrengthChannelUID, List.of(new StateOption("1", "Unusable"),
                new StateOption("2", "Okay"), new StateOption("3", "Amazing")));

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // doing nothing here
    }
}
