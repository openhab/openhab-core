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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_SCRIPT_TYPE;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_TO_HANDLER_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_TO_ITEM_SCRIPT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * The {@link ScriptProfileTest} contains tests for the {@link ScriptProfile}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ScriptProfileTest {
    private static final String SUPPORTED_SCRIPT_TYPE = "supportedScriptType";
    private static final String UNSUPPORTED_SCRIPT_TYPE = "unsupportedScriptType";

    private @Mock @NonNullByDefault({}) ScriptEngineManager scriptEngineManager;
    private @Mock @NonNullByDefault({}) ScriptEngineContainer scriptEngineContainer;

    private @Mock @NonNullByDefault({}) ScriptEngine scriptEngine;
    private @Mock @NonNullByDefault({}) ScriptContext scriptContext;

    private @Mock @NonNullByDefault({}) ProfileCallback profileCallback;

    @BeforeEach
    public void setUp() {
        when(scriptEngineManager.isSupported(SUPPORTED_SCRIPT_TYPE)).thenReturn(true);
        when(scriptEngineManager.isSupported(UNSUPPORTED_SCRIPT_TYPE)).thenReturn(false);

        when(scriptEngineManager.createScriptEngine(any(), any())).thenReturn(scriptEngineContainer);

        when(scriptEngineContainer.getScriptEngine()).thenReturn(scriptEngine);
        when(scriptEngine.getContext()).thenReturn(scriptContext);
    }

    @Test
    public void testScriptNotExecutedAndNoValueForwardedToCallbackIfScriptTypeMissing() {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withToHandlerScript("outScript").build();

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(scriptEngineManager, never()).createScriptEngine(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void testScriptNotExecutedAndNoValueForwardedToCallbackIfNoScriptDefined() {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE).build();

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(scriptEngineManager, never()).createScriptEngine(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void testScriptNotExecutedAndNoValueForwardedToCallbackIfUnsupportedScriptTypeDefined() {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(UNSUPPORTED_SCRIPT_TYPE)
                .withToItemScript("inScript").withToHandlerScript("outScript").build();

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(scriptEngineManager, never()).createScriptEngine(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void scriptExecutionErrorForwardsNoValueToCallback() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withToItemScript("inScript").withToHandlerScript("outScript").build();

        when(scriptEngine.eval((String) any())).thenThrow(new ScriptException("intentional failure"));

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(scriptEngineManager).createScriptEngine(any(), any());
        verify(scriptEngine, times(3)).eval((String) any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void scriptExecutionResultNullForwardsNoValueToCallback() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withToItemScript("inScript").withToHandlerScript("outScript").build();

        when(scriptEngine.eval((String) any())).thenReturn(null);

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(scriptEngineManager).createScriptEngine(any(), any());
        verify(scriptEngine, times(3)).eval((String) any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void scriptExecutionResultForwardsTransformedValueToCallback() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withToItemScript("inScript").withToHandlerScript("outScript")
                .withAcceptedCommandTypes(List.of(OnOffType.class)).withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(scriptEngine.eval((String) any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(scriptEngineManager).createScriptEngine(any(), any());
        verify(scriptEngine, times(3)).eval((String) any());
        verify(profileCallback).handleCommand(OnOffType.OFF);
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);
    }

    @Test
    public void onlyToItemScriptDoesNotForwardOutboundCommands() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withToItemScript("inScript").withAcceptedCommandTypes(List.of(OnOffType.class))
                .withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(DecimalType.class)).build();

        when(scriptEngine.eval((String) any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(scriptEngineManager).createScriptEngine(any(), any());
        verify(scriptEngine, times(2)).eval((String) any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);
    }

    @Test
    public void onlyToHandlerScriptDoesNotForwardInboundCommands() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withToHandlerScript("outScript").withAcceptedCommandTypes(List.of(DecimalType.class))
                .withAcceptedDataTypes(List.of(DecimalType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(scriptEngine.eval((String) any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(scriptEngineManager).createScriptEngine(any(), any());
        verify(scriptEngine, times(1)).eval((String) any());
        verify(profileCallback).handleCommand(OnOffType.OFF);
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void incompatibleStateOrCommandNotForwardedToCallback() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withToItemScript("inScript").withToHandlerScript("outScript")
                .withAcceptedCommandTypes(List.of(DecimalType.class)).withAcceptedDataTypes(List.of(PercentType.class))
                .withHandlerAcceptedCommandTypes(List.of(HSBType.class)).build();

        when(scriptEngine.eval((String) any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(profileCallback, profileContext, scriptEngineManager);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(scriptEngineManager).createScriptEngine(any(), any());
        verify(scriptEngine, times(3)).eval((String) any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    private static class ProfileContextBuilder {
        private final Map<String, Object> configuration = new HashMap<>();
        private List<Class<? extends State>> acceptedDataTypes = List.of();
        private List<Class<? extends Command>> acceptedCommandTypes = List.of();
        private List<Class<? extends Command>> handlerAcceptedCommandTypes = List.of();

        public static ProfileContextBuilder create() {
            return new ProfileContextBuilder();
        }

        public ProfileContextBuilder withToItemScript(String toItem) {
            configuration.put(CONFIG_TO_ITEM_SCRIPT, toItem);
            return this;
        }

        public ProfileContextBuilder withToHandlerScript(String toHandlerScript) {
            configuration.put(CONFIG_TO_HANDLER_SCRIPT, toHandlerScript);
            return this;
        }

        public ProfileContextBuilder withScriptType(String scriptType) {
            configuration.put(CONFIG_SCRIPT_TYPE, scriptType);
            return this;
        }

        public ProfileContextBuilder withAcceptedDataTypes(List<Class<? extends State>> acceptedDataTypes) {
            this.acceptedDataTypes = acceptedDataTypes;
            return this;
        }

        public ProfileContextBuilder withAcceptedCommandTypes(List<Class<? extends Command>> acceptedCommandTypes) {
            this.acceptedCommandTypes = acceptedCommandTypes;
            return this;
        }

        public ProfileContextBuilder withHandlerAcceptedCommandTypes(
                List<Class<? extends Command>> handlerAcceptedCommandTypes) {
            this.handlerAcceptedCommandTypes = handlerAcceptedCommandTypes;
            return this;
        }

        public ProfileContext build() {
            return new ProfileContext() {
                @Override
                public Configuration getConfiguration() {
                    return new Configuration(configuration);
                }

                @Override
                public ScheduledExecutorService getExecutorService() {
                    throw new IllegalStateException();
                }

                @Override
                public List<Class<? extends State>> getAcceptedDataTypes() {
                    return acceptedDataTypes;
                }

                @Override
                public List<Class<? extends Command>> getAcceptedCommandTypes() {
                    return acceptedCommandTypes;
                }

                @Override
                public List<Class<? extends Command>> getHandlerAcceptedCommandTypes() {
                    return handlerAcceptedCommandTypes;
                }
            };
        }
    }
}
