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
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_INBOUND_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_OUTBOUND_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_SCRIPT_TYPE;

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
        ProfileContext profileContext = ProfileContextBuilder.create().withInboundScript("inScript")
                .withOutboundScript("outScript").build();

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
                .withInboundScript("inScript").withOutboundScript("outScript").build();

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
                .withInboundScript("inScript").withOutboundScript("outScript").build();

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
                .withInboundScript("inScript").withOutboundScript("outScript").build();

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
                .withInboundScript("inScript").withOutboundScript("outScript").build();

        when(profileCallback.getAcceptedCommandTypes()).thenReturn(List.of(OnOffType.class));
        when(profileCallback.getAcceptedDataTypes()).thenReturn(List.of(OnOffType.class));
        when(profileCallback.getHandlerAcceptedCommandTypes()).thenReturn(List.of(OnOffType.class));

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
    public void onlyInboundScriptDoesNotForwardOutboundCommands() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withInboundScript("inScript").build();

        when(profileCallback.getAcceptedCommandTypes()).thenReturn(List.of(OnOffType.class));
        when(profileCallback.getAcceptedDataTypes()).thenReturn(List.of(OnOffType.class));
        when(profileCallback.getHandlerAcceptedCommandTypes()).thenReturn(List.of(DecimalType.class));

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
    public void onlyOutboundScriptDoesNotForwardInboundCommands() throws ScriptException {
        ProfileContext profileContext = ProfileContextBuilder.create().withScriptType(SUPPORTED_SCRIPT_TYPE)
                .withOutboundScript("outScript").build();

        when(profileCallback.getAcceptedCommandTypes()).thenReturn(List.of(DecimalType.class));
        when(profileCallback.getAcceptedDataTypes()).thenReturn(List.of(DecimalType.class));
        when(profileCallback.getHandlerAcceptedCommandTypes()).thenReturn(List.of(OnOffType.class));

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
                .withInboundScript("inScript").withOutboundScript("outScript").build();

        when(profileCallback.getAcceptedCommandTypes()).thenReturn(List.of(DecimalType.class));
        when(profileCallback.getAcceptedDataTypes()).thenReturn(List.of(PercentType.class));
        when(profileCallback.getHandlerAcceptedCommandTypes()).thenReturn(List.of(HSBType.class));

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

        public static ProfileContextBuilder create() {
            return new ProfileContextBuilder();
        }

        public ProfileContextBuilder withInboundScript(String inboundScript) {
            configuration.put(CONFIG_INBOUND_SCRIPT, inboundScript);
            return this;
        }

        public ProfileContextBuilder withOutboundScript(String outboundScript) {
            configuration.put(CONFIG_OUTBOUND_SCRIPT, outboundScript);
            return this;
        }

        public ProfileContextBuilder withScriptType(String scriptType) {
            configuration.put(CONFIG_SCRIPT_TYPE, scriptType);
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
            };
        }
    }
}
