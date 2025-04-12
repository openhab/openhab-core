/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.library.items;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.MediaType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.UnDefType;

/**
 * An {@link PlayerItem} allows to control a player, e.g. an audio player.
 *
 * @author Alex Tugarev - Initial contribution
 */
@NonNullByDefault
public class PlayerItem extends GenericItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(PlayPauseType.class,
            RewindFastforwardType.class, MediaType.class, StringType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(PlayPauseType.class,
            RewindFastforwardType.class, NextPreviousType.class, MediaType.class, StringType.class, RefreshType.class);

    public PlayerItem(String name) {
        super(CoreItemFactory.PLAYER, name);
    }

    /* package */ PlayerItem(String type, String name) {
        super(type, name);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return ACCEPTED_DATA_TYPES;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return ACCEPTED_COMMAND_TYPES;
    }

    /**
     * Send a PLAY/PAUSE command to the item.
     *
     * @param command the command to be sent
     */
    public void send(MediaType command) {
        internalSend(command, null);
    }

    /**
     * Send a PLAY/PAUSE command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(PlayPauseType command, @Nullable String source) {
        internalSend(command, source);
    }

    /**
     * Send a REWIND/FASTFORWARD command to the item.
     *
     * @param command the command to be sent
     */
    public void send(RewindFastforwardType command) {
        internalSend(command, null);
    }

    /**
     * Send a REWIND/FASTFORWARD command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(RewindFastforwardType command, @Nullable String source) {
        internalSend(command, source);
    }

    /**
     * Send a NEXT/PREVIOUS command to the item.
     *
     * @param command the command to be sent
     */
    public void send(NextPreviousType command) {
        internalSend(command, null);
    }

    /**
     * Send a NEXT/PREVIOUS command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(NextPreviousType command, @Nullable String source) {
        internalSend(command, source);
    }

    @Override
    public void setState(State state, @Nullable String source) {
        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            applyState(state, source);
        } else {
            logSetTypeError(state);
        }
    }

    @Override
    public void setTimeSeries(TimeSeries timeSeries) {
        if (timeSeries.getStates().allMatch(s -> s.state() instanceof PlayPauseType
                || s.state() instanceof RewindFastforwardType || s.state() instanceof MediaType)) {
            applyTimeSeries(timeSeries);
        } else {
            logSetTypeError(timeSeries);
        }
    }
}
