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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_COMMAND_FROM_ITEM_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_STATE_FROM_ITEM_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_TO_HANDLER_SCRIPT;
import static org.openhab.core.automation.module.script.profile.ScriptProfile.CONFIG_TO_ITEM_SCRIPT;

import java.time.Instant;
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
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

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

        ItemChannelLink link = new ItemChannelLink("DummyItem", new ChannelUID("foo:bar:baz:qux"));
        when(profileCallback.getItemChannelLink()).thenReturn(link);

        setupInterceptedLogger(ScriptProfile.class, LogLevel.ERROR);

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(OnOffType.ON));
        scriptProfile.onCommandFromItem(OnOffType.ON);

        verify(transformationServiceMock, never()).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());

        assertLogMessage(ScriptProfile.class, LogLevel.ERROR,
                "Neither 'toItemScript', 'commandFromItemScript' nor 'stateFromItemScript' defined in link '"
                        + link.toString() + "'. Profile will discard all states and commands.");
    }

    @Test
    public void fallsBackToToHandlerScriptIfCommandFromItemScriptNotDefined() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withToHandlerScript("outScript").withAcceptedCommandTypes(List.of(OnOffType.class))
                .withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        ItemChannelLink link = new ItemChannelLink("DummyItem", new ChannelUID("foo:bar:baz:qux"));
        when(profileCallback.getItemChannelLink()).thenReturn(link);

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        setupInterceptedLogger(ScriptProfile.class, LogLevel.WARN);

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);

        verify(transformationServiceMock, times(3)).transform(any(), any());
        verify(profileCallback, times(1)).handleCommand(OnOffType.OFF);
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);

        assertLogMessage(ScriptProfile.class, LogLevel.WARN,
                "'toHandlerScript' has been deprecated! Please use 'commandFromItemScript' instead in link '"
                        + link.toString() + "'.");
    }

    @Test
    public void scriptExecutionErrorForwardsNoValueToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withCommandFromItemScript("outScript").withStateFromItemScript("outScript").build();

        when(transformationServiceMock.transform(any(), any()))
                .thenThrow(new TransformationException("intentional failure"));

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);
        scriptProfile.onStateUpdateFromItem(OnOffType.ON);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(OnOffType.ON));

        verify(transformationServiceMock, times(5)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
    }

    @Test
    public void scriptExecutionResultNullForwardsNoValueToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withCommandFromItemScript("outScript").withStateFromItemScript("outScript").build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(null);

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(OnOffType.ON);
        scriptProfile.onStateUpdateFromHandler(OnOffType.ON);
        scriptProfile.onCommandFromItem(OnOffType.ON);
        scriptProfile.onStateUpdateFromItem(OnOffType.ON);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(OnOffType.ON));

        verify(transformationServiceMock, times(5)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
    }

    @Test
    public void scriptExecutionResultForwardsTransformedValueToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withCommandFromItemScript("outScript").withStateFromItemScript("outScript")
                .withAcceptedCommandTypes(List.of(OnOffType.class)).withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromItem(DecimalType.ZERO);

        TimeSeries timeSeries = createTimeSeries(DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(timeSeries);

        verify(transformationServiceMock, times(5)).transform(any(), any());
        verify(profileCallback, times(2)).handleCommand(OnOffType.OFF);
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);
        verify(profileCallback).sendTimeSeries(replaceTimeSeries(timeSeries, OnOffType.OFF));
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
        scriptProfile.onStateUpdateFromItem(DecimalType.ZERO);

        TimeSeries timeSeries = createTimeSeries(DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(timeSeries);

        verify(transformationServiceMock, times(3)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback).sendUpdate(OnOffType.OFF);
        verify(profileCallback).sendCommand(OnOffType.OFF);
        verify(profileCallback).sendTimeSeries(replaceTimeSeries(timeSeries, OnOffType.OFF));
    }

    @Test
    public void onlyToHandlerCommandScriptDoesNotForwardInboundCommands() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withCommandFromItemScript("outScript")
                .withAcceptedCommandTypes(List.of(DecimalType.class)).withAcceptedDataTypes(List.of(DecimalType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromItem(DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(DecimalType.ZERO));

        verify(transformationServiceMock, times(1)).transform(any(), any());
        verify(profileCallback, times(1)).handleCommand(OnOffType.OFF);
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
    }

    @Test
    public void onlyToHandlerStateScriptDoesNotForwardInboundCommands() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withStateFromItemScript("outScript")
                .withAcceptedCommandTypes(List.of(DecimalType.class)).withAcceptedDataTypes(List.of(DecimalType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromItem(DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(DecimalType.ZERO));

        verify(transformationServiceMock, times(1)).transform(any(), any());
        verify(profileCallback, times(1)).handleCommand(OnOffType.OFF);
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
    }

    @Test
    public void incompatibleStateOrCommandNotForwardedToCallback() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withCommandFromItemScript("outScript").withStateFromItemScript("outScript")
                .withAcceptedCommandTypes(List.of(DecimalType.class)).withAcceptedDataTypes(List.of(PercentType.class))
                .withHandlerAcceptedCommandTypes(List.of(HSBType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromItem(DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(DecimalType.ZERO));

        verify(transformationServiceMock, times(5)).transform(any(), any());
        verify(profileCallback, never()).handleCommand(any());
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
    }

    @Test
    public void fallbackToToHandlerScriptIfNotToHandlerCommandScript() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToHandlerScript("outScript")
                .withAcceptedCommandTypes(List.of(DecimalType.class)).withAcceptedDataTypes(List.of(DecimalType.class))
                .withHandlerAcceptedCommandTypes(List.of(OnOffType.class)).build();

        when(transformationServiceMock.transform(any(), any())).thenReturn(OnOffType.OFF.toString());

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        scriptProfile.onCommandFromHandler(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromHandler(DecimalType.ZERO);
        scriptProfile.onCommandFromItem(DecimalType.ZERO);
        scriptProfile.onStateUpdateFromItem(DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(createTimeSeries(DecimalType.ZERO));

        verify(transformationServiceMock, times(1)).transform(any(), any());
        verify(profileCallback, times(1)).handleCommand(OnOffType.OFF);
        verify(profileCallback, never()).sendUpdate(any());
        verify(profileCallback, never()).sendCommand(any());
        verify(profileCallback, never()).sendTimeSeries(any());
    }

    @Test
    public void filteredTimeSeriesTest() throws TransformationException {
        ProfileContext profileContext = ProfileContextBuilder.create().withToItemScript("inScript")
                .withAcceptedCommandTypes(List.of(OnOffType.class)).withAcceptedDataTypes(List.of(OnOffType.class))
                .withHandlerAcceptedCommandTypes(List.of(DecimalType.class)).build();

        when(transformationServiceMock.transform(any(), eq("0"))).thenReturn(OnOffType.OFF.toString());
        when(transformationServiceMock.transform(any(), eq("1"))).thenReturn(null);

        ScriptProfile scriptProfile = new ScriptProfile(mock(ProfileTypeUID.class), profileCallback, profileContext,
                transformationServiceMock);

        TimeSeries timeSeries = createTimeSeries(DecimalType.ZERO, DecimalType.valueOf("1"), DecimalType.ZERO);
        scriptProfile.onTimeSeriesFromHandler(timeSeries);

        verify(transformationServiceMock, times(3)).transform(any(), any());

        TimeSeries transformedTimeSeries = new TimeSeries(timeSeries.getPolicy());
        timeSeries.getStates().forEach(entry -> {
            if (entry.state().equals(DecimalType.ZERO)) {
                transformedTimeSeries.add(entry.timestamp(), OnOffType.OFF);
            }
        });
        verify(profileCallback).sendTimeSeries(transformedTimeSeries);
    }

    private TimeSeries createTimeSeries(State... states) {
        TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.ADD);
        Instant instant = Instant.now();
        for (State state : states) {
            timeSeries.add(instant, state);
            instant = instant.plusMillis(100);
        }
        return timeSeries;
    }

    private TimeSeries replaceTimeSeries(TimeSeries timeSeries, State state) {
        TimeSeries newTimeSeries = new TimeSeries(timeSeries.getPolicy());
        timeSeries.getStates().forEach(entry -> newTimeSeries.add(entry.timestamp(), state));
        return newTimeSeries;
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

        public ProfileContextBuilder withCommandFromItemScript(String commandFromItemScript) {
            configuration.put(CONFIG_COMMAND_FROM_ITEM_SCRIPT, commandFromItemScript);
            return this;
        }

        public ProfileContextBuilder withStateFromItemScript(String stateFromItemScript) {
            configuration.put(CONFIG_STATE_FROM_ITEM_SCRIPT, stateFromItemScript);
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
