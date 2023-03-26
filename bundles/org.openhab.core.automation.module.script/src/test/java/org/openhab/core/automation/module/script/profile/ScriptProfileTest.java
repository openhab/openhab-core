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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_TO_HANDLER_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_TO_ITEM_SCRIPT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
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
public class ScriptProfileTest extends JavaTest {
    private @Mock @NonNullByDefault({}) ProfileCallback profileCallback;

    private @Mock @NonNullByDefault({}) TransformationService transformationServiceMock;

    @BeforeEach
    public void setUp() throws TransformationException {
        when(transformationServiceMock.transform(any(), any())).thenReturn("");
    }

    @Test
    public void testScriptNotExecutedAndNoValueForwardedToCallbackIfNoScriptDefined() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().build();

        setupInterceptedLogger(ScriptProfile.class, LogLevel.ERROR);

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(transformationServiceMock, never()).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());

        assertLogMessage(ScriptProfile.class, LogLevel.ERROR,
                "Neither 'toItem' nor 'toHandler' script defined. Profile will discard all states and commands.");
    }

    @Test
    public void scriptExecutionErrorForwardsNoValueToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withToHandlerScript("outScript").build();

        when(transformationServiceMock.transform(any(), any()))
                .thenThrow(new TransformationException("intentional failure"));

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(transformationServiceMock, times(3)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void scriptExecutionResultNullForwardsNoValueToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withToHandlerScript("outScript").build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(null);

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(transformationServiceMock, times(3)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void scriptExecutionResultForwardsTransformedValueToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withToHandlerScript("outScript").withAcceptedCommandTypes(List.of(OnOffType.class))
                .withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(transformationServiceMock, times(3)).transform(any(), any());
        verify(profileCallback).handleCommand(OnOffType.OFF);
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);
    }

    @Test
    public void onlyToItemScriptDoesNotForwardOutboundCommands() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withAcceptedCommandTypes(List.of(OnOffType.class)).withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(DecimalType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(transformationServiceMock, times(2)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);
    }

    @Test
    public void onlyToHandlerScriptDoesNotForwardInboundCommands() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToHandlerScript("outScript")
                .withAcceptedCommandTypes(List.of(DecimalType.class)).withAcceptedDataTypes(List.of(DecimalType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(transformationServiceMock).transform(any(), any());
        verify(profileCallback).handleCommand(OnOffType.OFF);
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
    }

    @Test
    public void incompatibleStateOrCommandNotForwardedToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withToHandlerScript("outScript").withAcceptedCommandTypes(List.of(DecimalType.class))
                .withAcceptedDataTypes(List.of(PercentType.class))
                .withHandlerAcceptedCommandTypes(List.of(HSBType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(transformationServiceMock, times(3)).transform(any(), any());
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
