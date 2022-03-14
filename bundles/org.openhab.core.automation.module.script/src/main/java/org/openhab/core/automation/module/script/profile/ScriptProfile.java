/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
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
    protected static final String CONFIG_TO_ITEM_SCRIPT = "toItemScript";
    protected static final String CONFIG_TO_HANDLER_SCRIPT = "toHandlerScript";
    protected static final String CONFIG_SCRIPT_TYPE = "scriptType";

    private final Logger logger = LoggerFactory.getLogger(ScriptProfile.class);

    private final String profileUuid = ScriptProfileFactory.SCRIPT_PROFILE_UID + ":" + UUID.randomUUID();

    private final ProfileCallback callback;
    private final ScriptEngineManager scriptEngineManager;

    private final List<Class<? extends State>> acceptedDataTypes;
    private final List<Class<? extends Command>> acceptedCommandTypes;
    private final List<Class<? extends Command>> handlerAcceptedCommandTypes;

    private final Map<Direction, String> scripts = new HashMap<>();
    private final Map<Direction, CompiledScript> compiledScripts = new HashMap<>();

    private final String scriptType;

    private final boolean isConfigured;

    private @Nullable ScriptEngineContainer scriptEngineContainer;

    public ScriptProfile(ProfileCallback callback, ProfileContext profileContext,
            ScriptEngineManager scriptEngineManager) {
        this.callback = callback;
        this.scriptEngineManager = scriptEngineManager;

        this.acceptedCommandTypes = profileContext.getAcceptedCommandTypes();
        this.acceptedDataTypes = profileContext.getAcceptedDataTypes();
        this.handlerAcceptedCommandTypes = profileContext.getHandlerAcceptedCommandTypes();

        this.scripts.put(Direction.TO_ITEM, ConfigParser
                .valueAsOrElse(profileContext.getConfiguration().get(CONFIG_TO_ITEM_SCRIPT), String.class, ""));
        this.scripts.put(Direction.TO_HANDLER, ConfigParser
                .valueAsOrElse(profileContext.getConfiguration().get(CONFIG_TO_HANDLER_SCRIPT), String.class, ""));
        this.scriptType = ConfigParser.valueAsOrElse(profileContext.getConfiguration().get(CONFIG_SCRIPT_TYPE),
                String.class, "");

        if (this.scripts.values().stream().allMatch(String::isBlank)) {
            logger.error(
                    "Neither 'toItem' nor 'toHandler' script defined. Profile will discard all states and commands.");
            isConfigured = false;
            return;
        }
        if (this.scriptType.isBlank()) {
            logger.error("Parameter 'scriptType' must not be empty. Profile will discard all states and commands.");
            isConfigured = false;
            return;
        }

        isConfigured = true;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return ScriptProfileFactory.SCRIPT_PROFILE_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
    }

    @Override
    public void onCommandFromItem(Command command) {
        if (isConfigured) {
            Object returnValue = executeScript(Direction.TO_HANDLER, command);
            if (returnValue instanceof String) {
                // try to parse the value
                Command newCommand = TypeParser.parseCommand(handlerAcceptedCommandTypes, (String) returnValue);
                if (newCommand != null) {
                    callback.handleCommand(newCommand);
                }
            } else if (returnValue instanceof Command) {
                if (handlerAcceptedCommandTypes.contains(returnValue.getClass())) {
                    callback.handleCommand((Command) returnValue);
                }
            }
        }
    }

    @Override
    public void onCommandFromHandler(Command command) {
        if (isConfigured) {
            Object returnValue = executeScript(Direction.TO_ITEM, command);
            if (returnValue instanceof String) {
                Command newCommand = TypeParser.parseCommand(acceptedCommandTypes, (String) returnValue);
                if (newCommand != null) {
                    callback.sendCommand(newCommand);
                }
            } else if (returnValue instanceof Command) {
                if (acceptedCommandTypes.contains(returnValue.getClass())) {
                    callback.sendCommand((Command) returnValue);
                }
            }
        }
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        if (isConfigured) {
            Object returnValue = executeScript(Direction.TO_ITEM, state);
            if (returnValue instanceof String) {
                // special handling for UnDefType, it's not available in the TypeParser
                if ("UNDEF".equals(returnValue)) {
                    callback.sendUpdate(UnDefType.UNDEF);
                } else if ("NULL".equals(returnValue)) {
                    callback.sendUpdate(UnDefType.NULL);
                } else {
                    State newState = TypeParser.parseState(acceptedDataTypes, (String) returnValue);
                    if (newState != null) {
                        callback.sendUpdate(newState);
                    }
                }
            } else if (returnValue instanceof State) {
                if (acceptedDataTypes.contains(returnValue.getClass())) {
                    callback.sendUpdate((State) returnValue);
                }
            }
        }
    }

    private synchronized @Nullable Object executeScript(Direction direction, Type input) {
        String script = this.scripts.get(direction);

        if (script.isBlank()) {
            return null;
        }

        if (!scriptEngineManager.isSupported(scriptType)) {
            // language has been removed, clear container and compiled scripts if found
            if (this.scriptEngineContainer != null) {
                this.scriptEngineContainer = null;
                scriptEngineManager.removeEngine(profileUuid);
            }
            compiledScripts.clear();
            return null;
        } else if (this.scriptEngineContainer == null) {
            // try to create a ScriptEngine
            this.scriptEngineContainer = scriptEngineManager.createScriptEngine(scriptType, profileUuid);
        }

        ScriptEngineContainer scriptEngineContainer = this.scriptEngineContainer;

        if (scriptEngineContainer != null) {
            try {
                CompiledScript compiledScript = this.compiledScripts.get(direction);

                if (compiledScript == null && scriptEngineContainer.getScriptEngine() instanceof Compilable) {
                    // no compiled script available but compiling is supported
                    compiledScript = ((Compilable) scriptEngineContainer.getScriptEngine()).compile(script);
                    this.compiledScripts.put(direction, compiledScript);
                }

                ScriptEngine engine = compiledScript != null ? compiledScript.getEngine()
                        : scriptEngineContainer.getScriptEngine();
                ScriptContext executionContext = engine.getContext();
                executionContext.setAttribute("input", input, ScriptContext.ENGINE_SCOPE);
                executionContext.setAttribute("inputString", input.toFullString(), ScriptContext.ENGINE_SCOPE);

                return compiledScript != null ? compiledScript.eval() : engine.eval(script);
            } catch (ScriptException e) {
                logger.warn("Failed to execute script: {}", e.getMessage());
            }
        }

        return null;
    }

    private enum Direction {
        TO_ITEM,
        TO_HANDLER;
    }
}
