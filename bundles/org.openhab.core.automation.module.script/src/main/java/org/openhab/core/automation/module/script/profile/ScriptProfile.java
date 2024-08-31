/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.profile;

import java.util.List;
import java.util.Optional;

import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TimeSeriesProfile;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.Type;
import org.openhab.core.types.TypeParser;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptProfile} is generic profile for managing values with scripts
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ScriptProfile implements TimeSeriesProfile {

    public static final String CONFIG_TO_ITEM_SCRIPT = "toItemScript";
    public static final String CONFIG_TO_HANDLER_SCRIPT = "toHandlerScript";
    public static final String CONFIG_COMMAND_FROM_ITEM_SCRIPT = "commandFromItemScript";
    public static final String CONFIG_STATE_FROM_ITEM_SCRIPT = "stateFromItemScript";

    private final Logger logger = LoggerFactory.getLogger(ScriptProfile.class);

    private final ProfileCallback callback;
    private final TransformationService transformationService;

    private final List<Class<? extends State>> acceptedDataTypes;
    private final List<Class<? extends Command>> acceptedCommandTypes;
    private final List<Class<? extends Command>> handlerAcceptedCommandTypes;

    private final String toItemScript;
    private final String commandFromItemScript;
    private final String stateFromItemScript;
    private final ProfileTypeUID profileTypeUID;

    private final boolean isConfigured;

    public ScriptProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext profileContext,
            TransformationService transformationService) {
        this.profileTypeUID = profileTypeUID;
        this.callback = callback;
        this.transformationService = transformationService;

        this.acceptedCommandTypes = profileContext.getAcceptedCommandTypes();
        this.acceptedDataTypes = profileContext.getAcceptedDataTypes();
        this.handlerAcceptedCommandTypes = profileContext.getHandlerAcceptedCommandTypes();

        this.toItemScript = ConfigParser.valueAsOrElse(profileContext.getConfiguration().get(CONFIG_TO_ITEM_SCRIPT),
                String.class, "");
        String toHandlerScript = ConfigParser
                .valueAsOrElse(profileContext.getConfiguration().get(CONFIG_TO_HANDLER_SCRIPT), String.class, "");
        String localCommandFromItemScript = ConfigParser.valueAsOrElse(
                profileContext.getConfiguration().get(CONFIG_COMMAND_FROM_ITEM_SCRIPT), String.class, "");
        this.commandFromItemScript = localCommandFromItemScript.isBlank() ? toHandlerScript
                : localCommandFromItemScript;
        this.stateFromItemScript = ConfigParser
                .valueAsOrElse(profileContext.getConfiguration().get(CONFIG_STATE_FROM_ITEM_SCRIPT), String.class, "");

        if (!toHandlerScript.isBlank() && localCommandFromItemScript.isBlank()) {
            logger.warn(
                    "'toHandlerScript' has been deprecated! Please use 'commandFromItemScript' instead in link '{}'.",
                    callback.getItemChannelLink());
        }

        if (toItemScript.isBlank() && commandFromItemScript.isBlank() && stateFromItemScript.isBlank()) {
            logger.error(
                    "Neither 'toItemScript', 'commandFromItemScript' nor 'stateFromItemScript' defined in link '{}'. Profile will discard all states and commands.",
                    callback.getItemChannelLink());
            isConfigured = false;
            return;
        }

        isConfigured = true;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return profileTypeUID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        if (isConfigured) {
            fromItem(stateFromItemScript, state);
        }
    }

    @Override
    public void onCommandFromItem(Command command) {
        if (isConfigured) {
            fromItem(commandFromItemScript, command);
        }
    }

    private void fromItem(String script, Type type) {
        String returnValue = executeScript(script, type);
        if (returnValue != null) {
            // try to parse the value
            Command newCommand = TypeParser.parseCommand(handlerAcceptedCommandTypes, returnValue);
            if (newCommand != null) {
                callback.handleCommand(newCommand);
            } else {
                logger.debug("The given type {} could not be transformed to a command", type);
            }
        }
    }

    @Override
    public void onCommandFromHandler(Command command) {
        if (isConfigured) {
            String returnValue = executeScript(toItemScript, command);
            if (returnValue != null) {
                Command newCommand = TypeParser.parseCommand(acceptedCommandTypes, returnValue);
                if (newCommand != null) {
                    callback.sendCommand(newCommand);
                }
            }
        }
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        if (isConfigured) {
            transformState(state).ifPresent(callback::sendUpdate);
        }
    }

    @Override
    public void onTimeSeriesFromHandler(TimeSeries timeSeries) {
        if (isConfigured) {
            TimeSeries transformedTimeSeries = new TimeSeries(timeSeries.getPolicy());
            timeSeries.getStates().forEach(entry -> {
                transformState(entry.state()).ifPresent(transformedState -> {
                    transformedTimeSeries.add(entry.timestamp(), transformedState);
                });
            });
            if (transformedTimeSeries.size() > 0) {
                callback.sendTimeSeries(transformedTimeSeries);
            }
        }
    }

    private Optional<State> transformState(State state) {
        return Optional.ofNullable(executeScript(toItemScript, state)).map(output -> {
            return switch (output) {
                case "UNDEF" -> UnDefType.UNDEF;
                case "NULL" -> UnDefType.NULL;
                default -> TypeParser.parseState(acceptedDataTypes, output);
            };
        });
    }

    private @Nullable String executeScript(String script, Type input) {
        if (!script.isBlank()) {
            try {
                return transformationService.transform(script, input.toFullString());
            } catch (TransformationException e) {
                if (e.getCause() instanceof ScriptException) {
                    logger.error("Failed to process script '{}': {}", script, e.getCause().getMessage());
                } else {
                    logger.error("Failed to process script '{}': {}", script, e.getMessage());
                }
            }
        }

        return null;
    }
}
