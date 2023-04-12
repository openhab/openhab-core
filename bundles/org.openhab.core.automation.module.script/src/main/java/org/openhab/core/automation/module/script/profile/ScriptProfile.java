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
package org.openhab.core.automation.module.script.profile;

import java.util.List;

import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
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
public class ScriptProfile implements StateProfile {

    public static final String CONFIG_TO_ITEM_SCRIPT = "toItemScript";
    public static final String CONFIG_TO_HANDLER_SCRIPT = "toHandlerScript";

    private final Logger logger = LoggerFactory.getLogger(ScriptProfile.class);

    private final ProfileCallback callback;
    private final TransformationService transformationService;

    private final List<Class<? extends State>> acceptedDataTypes;
    private final List<Class<? extends Command>> acceptedCommandTypes;
    private final List<Class<? extends Command>> handlerAcceptedCommandTypes;

    private final String toItemScript;
    private final String toHandlerScript;
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
        this.toHandlerScript = ConfigParser
                .valueAsOrElse(profileContext.getConfiguration().get(CONFIG_TO_HANDLER_SCRIPT), String.class, "");

        if (toItemScript.isBlank() && toHandlerScript.isBlank()) {
            logger.error(
                    "Neither 'toItem' nor 'toHandler' script defined. Profile will discard all states and commands.");
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
    }

    @Override
    public void onCommandFromItem(Command command) {
        if (isConfigured) {
            String returnValue = executeScript(toHandlerScript, command);
            if (returnValue != null) {
                // try to parse the value
                Command newCommand = TypeParser.parseCommand(handlerAcceptedCommandTypes, returnValue);
                if (newCommand != null) {
                    callback.handleCommand(newCommand);
                }
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
            String returnValue = executeScript(toItemScript, state);
            // special handling for UnDefType, it's not available in the TypeParser
            if ("UNDEF".equals(returnValue)) {
                callback.sendUpdate(UnDefType.UNDEF);
            } else if ("NULL".equals(returnValue)) {
                callback.sendUpdate(UnDefType.NULL);
            } else if (returnValue != null) {
                State newState = TypeParser.parseState(acceptedDataTypes, returnValue);
                if (newState != null) {
                    callback.sendUpdate(newState);
                }
            }
        }
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
