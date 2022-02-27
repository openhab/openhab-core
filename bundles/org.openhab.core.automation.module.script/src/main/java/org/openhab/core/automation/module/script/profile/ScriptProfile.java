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

import java.util.UUID;

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
    protected static final String CONFIG_INBOUND_SCRIPT = "inboundScript";
    protected static final String CONFIG_OUTBOUND_SCRIPT = "outboundScript";
    protected static final String CONFIG_SCRIPT_TYPE = "scriptType";

    private final Logger logger = LoggerFactory.getLogger(ScriptProfile.class);

    private final String profileUuid = ScriptProfileFactory.SCRIPT_PROFILE_UID + ":" + UUID.randomUUID();

    private final ProfileCallback callback;
    private final ScriptEngineManager scriptEngineManager;

    private final String inboundScript;
    private final String outboundScript;
    private final String scriptType;

    private final boolean isConfigured;

    private @Nullable ScriptEngineContainer scriptEngineContainer;

    public ScriptProfile(ProfileCallback callback, ProfileContext profileContext,
            ScriptEngineManager scriptEngineManager) {
        this.callback = callback;
        this.scriptEngineManager = scriptEngineManager;

        this.inboundScript = ConfigParser.valueAsOrElse(profileContext.getConfiguration().get(CONFIG_INBOUND_SCRIPT),
                String.class, "");
        this.outboundScript = ConfigParser.valueAsOrElse(profileContext.getConfiguration().get(CONFIG_OUTBOUND_SCRIPT),
                String.class, "");
        this.scriptType = ConfigParser.valueAsOrElse(profileContext.getConfiguration().get(CONFIG_SCRIPT_TYPE),
                String.class, "");

        if (this.inboundScript.isBlank() && this.outboundScript.isBlank()) {
            logger.error("Neither inbound nor outbound script defined. Profile will discard states and commands.");
            isConfigured = false;
            return;
        }
        if (this.scriptType.isBlank()) {
            logger.error("Parameter 'scriptType' must not be empty. Profile will discard states and commands.");
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
        if (isConfigured && !outboundScript.isBlank()) {
            Object returnValue = executeScript(outboundScript, command);
            if (returnValue != null) {
                Command newCommand = TypeParser.parseCommand(callback.getHandlerAcceptedCommandTypes(),
                        returnValue.toString());
                if (newCommand != null) {
                    callback.handleCommand(newCommand);
                }
            }
        }
    }

    @Override
    public void onCommandFromHandler(Command command) {
        if (isConfigured && !inboundScript.isBlank()) {
            Object returnValue = executeScript(inboundScript, command);
            if (returnValue != null) {
                Command newCommand = TypeParser.parseCommand(callback.getAcceptedCommandTypes(),
                        returnValue.toString());
                if (newCommand != null) {
                    callback.sendCommand(newCommand);
                }
            }
        }
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        if (isConfigured && !inboundScript.isBlank()) {
            Object returnValue = executeScript(inboundScript, state);
            if ("UNDEF".equals(returnValue) || UnDefType.UNDEF.equals(returnValue)) {
                callback.sendUpdate(UnDefType.UNDEF);
            } else if ("NULL".equals(returnValue) || UnDefType.NULL.equals(returnValue)) {
                callback.sendUpdate(UnDefType.NULL);
            } else if (returnValue != null) {
                State newState = TypeParser.parseState(callback.getAcceptedDataTypes(), returnValue.toString());
                if (newState != null) {
                    callback.sendUpdate(newState);
                }
            }
        }
    }

    private synchronized @Nullable Object executeScript(String script, Type input) {
        if (!scriptEngineManager.isSupported(scriptType)) {
            if (scriptEngineContainer != null) {
                scriptEngineContainer = null;
                scriptEngineManager.removeEngine(profileUuid);
            }
            return null;
        }

        if (scriptEngineContainer == null) {
            this.scriptEngineContainer = scriptEngineManager.createScriptEngine(scriptType, profileUuid);
        }

        ScriptEngineContainer localContainer = this.scriptEngineContainer;
        if (localContainer != null) {
            try {
                ScriptEngine engine = localContainer.getScriptEngine();
                ScriptContext executionContext = engine.getContext();
                executionContext.setAttribute("input", input.toFullString(), ScriptContext.ENGINE_SCOPE);
                return engine.eval(script);
            } catch (ScriptException e) {
                logger.warn("Failed to execute script: {}", e.getMessage());
            }
        }
        return null;
    }
}
