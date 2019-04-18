/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.StringListType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.TypeParser;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation for an expire profile.
 *
 * @author Gaël L'hopital - initial contribution and API.
 * @alsoby Michael Wyraz -- initial v1 binding implementation
 * @alsoby John Cocula -- added command support, tweaks
 *
 */
@NonNullByDefault
public class ExpireProfile implements StateProfile {
    private static final String DURATION_PARAM = "duration";
    private static final String STATE_PARAM = "state";
    private static final String COMMAND_PARAM = "command";
    private static final Pattern DURATION_PATTERN = Pattern
            .compile("(?:([0-9]+)H)?\\s*(?:([0-9]+)M)?\\s*(?:([0-9]+)S)?", Pattern.CASE_INSENSITIVE);
    private final Logger logger = LoggerFactory.getLogger(ExpireProfile.class);

    /**
     * Ordered list of types that are tried out first when trying to parse transformed command
     */
    private static final List<Class<? extends Command>> DEFAULT_COMMANDS = new ArrayList<>();
    static {
        DEFAULT_COMMANDS.add(OnOffType.class);
        DEFAULT_COMMANDS.add(OpenClosedType.class);
        DEFAULT_COMMANDS.add(PlayPauseType.class);
        DEFAULT_COMMANDS.add(StopMoveType.class);
        DEFAULT_COMMANDS.add(RewindFastforwardType.class);
        DEFAULT_COMMANDS.add(UpDownType.class);
        DEFAULT_COMMANDS.add(DateTimeType.class);
        DEFAULT_COMMANDS.add(IncreaseDecreaseType.class);
        DEFAULT_COMMANDS.add(PointType.class);
        DEFAULT_COMMANDS.add(HSBType.class);
        DEFAULT_COMMANDS.add(QuantityType.class);
        DEFAULT_COMMANDS.add(DecimalType.class);
        DEFAULT_COMMANDS.add(StringListType.class);
        DEFAULT_COMMANDS.add(StringType.class);
    }

    /**
     * Ordered list of types that are tried out first when trying to parse transformed state
     */
    private static final List<Class<? extends State>> DEFAULT_STATES = new ArrayList<>();
    static {
        DEFAULT_STATES.add(OnOffType.class);
        DEFAULT_STATES.add(OpenClosedType.class);
        DEFAULT_STATES.add(PlayPauseType.class);
        DEFAULT_STATES.add(RewindFastforwardType.class);
        DEFAULT_STATES.add(UpDownType.class);
        DEFAULT_STATES.add(DateTimeType.class);
        DEFAULT_STATES.add(PointType.class);
        DEFAULT_STATES.add(HSBType.class);
        DEFAULT_STATES.add(QuantityType.class);
        DEFAULT_STATES.add(DecimalType.class);
        DEFAULT_STATES.add(StringListType.class);
        DEFAULT_STATES.add(StringType.class);
        DEFAULT_STATES.add(RawType.class);
    }

    private final ProfileCallback callback;
    private final ProfileContext context;
    private @NonNullByDefault({}) ScheduledExecutorService executor;

    /**
     * Human readable textual representation of duration (e.g. "13h 42m 12s")
     */
    private @Nullable String durationString;

    /**
     * duration in ms
     */
    private long duration;

    /**
     * state to post if item is expired.
     */
    private State expireState = UnDefType.UNDEF;

    /**
     * command to post if item is expired.
     */
    private @Nullable Command expireCommand;

    public ExpireProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        this.context = context;

        Object durationValue = this.context.getConfiguration().get(DURATION_PARAM);
        logger.debug("Configuring profile with {} parameter '{}'", DURATION_PARAM, durationValue);

        if (!(durationValue instanceof String)) {
            logger.error("Parameter '{}' is not of type String", DURATION_PARAM);
            return;
        }

        durationString = (String) durationValue;
        duration = parseDuration(durationString);
        if (duration == -1) {
            logger.error("Invalid duration: '{}' in parameter '{}'. Expected something like: '1h 15m 30s'", duration,
                    DURATION_PARAM);
            return;
        }

        Object stateValue = this.context.getConfiguration().get(STATE_PARAM);
        Object commandValue = this.context.getConfiguration().get(COMMAND_PARAM);

        if (stateValue instanceof String || commandValue instanceof String) {
            if (commandValue != null) {
                expireCommand = TypeParser.parseCommand(DEFAULT_COMMANDS, (String) commandValue);
            } else {
                expireState = TypeParser.parseState(DEFAULT_STATES, (String) stateValue);
            }
        }
        executor = this.context.getExecutorService();
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.EXPIRE;
    }

    @Override
    public void onCommandFromItem(Command command) {
        applyExpire(command);
    }

    @Override
    public void onCommandFromHandler(Command command) {
        applyExpire(command);
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        applyExpire(state);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        applyExpire(state);
    }

    private void applyExpire(Type state) {
        logger.trace("Received '{}'", state);
        executor.shutdownNow();
        if (state.equals(expireCommand) || state.equals(expireState)) {
            logger.debug("Received '{}'; stopping any future expiration.", state);
        } else {
            logger.debug("Will expire (with '{}' {}) in {} ms", expireCommand == null ? expireState : expireCommand,
                    expireCommand == null ? "state" : "command", duration);
            executor.schedule(() -> {
                if (expireCommand != null) {
                    callback.sendCommand(expireCommand);
                    logger.debug("No command or update received for {} - sending command '{}'", durationString,
                            expireCommand);
                } else {
                    callback.sendUpdate(expireState);
                    logger.debug("No command or update received for {} - updating state '{}'", durationString,
                            expireState);
                }
            }, System.currentTimeMillis() + duration, TimeUnit.MILLISECONDS);
        }
    }

    private long parseDuration(String duration) {
        Matcher m = DURATION_PATTERN.matcher(duration);
        if (!m.matches() || (m.group(1) == null && m.group(2) == null && m.group(3) == null)) {
            return -1;
        }

        long ms = 0;
        if (m.group(1) != null) {
            ms += Long.parseLong(m.group(1)) * 60 * 60 * 1000;
        }
        if (m.group(2) != null) {
            ms += Long.parseLong(m.group(2)) * 60 * 1000;
        }
        if (m.group(3) != null) {
            ms += Long.parseLong(m.group(3)) * 1000;
        }
        return ms;
    }

}
