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
package org.openhab.core.thing.internal.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.openhab.core.thing.profiles.SystemProfiles.*;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ToggleProfileTest {
    private final Logger logger = LoggerFactory.getLogger(ToggleProfileTest.class);

    private @Mock @NonNullByDefault({}) SystemProfileFactory systemProfileFactory;
    private @Mock @NonNullByDefault({}) ProfileCallback callbackMock;
    private @Mock @NonNullByDefault({}) ProfileContext contextMock;

    public static Stream<ProfileTypeUID> getAllToggleButtonSwitchProfiles() {
        return Stream.of(RAWBUTTON_TOGGLE_SWITCH, BUTTON_TOGGLE_SWITCH);
    }

    @ParameterizedTest
    @MethodSource("getAllToggleButtonSwitchProfiles")
    public void testSwitchItem(ProfileTypeUID profileUID) {
        prepareContextMock();
        TriggerProfile profile = newToggleProfile(profileUID);
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, OnOffType.ON, OnOffType.OFF);
        verifyAction(profile, OnOffType.OFF, OnOffType.ON);
    }

    @ParameterizedTest
    @MethodSource("getAllToggleButtonSwitchProfiles")
    public void testDimmerItem(ProfileTypeUID profileUID) {
        prepareContextMock();
        TriggerProfile profile = newToggleProfile(profileUID);
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, PercentType.HUNDRED, OnOffType.OFF);
        verifyAction(profile, PercentType.ZERO, OnOffType.ON);
        verifyAction(profile, new PercentType(50), OnOffType.OFF);
    }

    @ParameterizedTest
    @MethodSource("getAllToggleButtonSwitchProfiles")
    public void testColorItem(ProfileTypeUID profileUID) {
        prepareContextMock();
        TriggerProfile profile = newToggleProfile(profileUID);
        verifyAction(profile, UnDefType.NULL, OnOffType.ON);
        verifyAction(profile, HSBType.WHITE, OnOffType.OFF);
        verifyAction(profile, HSBType.BLACK, OnOffType.ON);
        verifyAction(profile, new HSBType("0,50,50"), OnOffType.OFF);
    }

    @Test
    public void testRollershutterItem() {
        prepareContextMock();
        TriggerProfile profile = newToggleProfile(RAWBUTTON_TOGGLE_ROLLERSHUTTER);
        verifyAction(profile, UnDefType.NULL, UpDownType.UP);
        verifyAction(profile, UpDownType.UP, UpDownType.DOWN);
        verifyAction(profile, UpDownType.DOWN, UpDownType.UP);

        profile = newToggleProfile(BUTTON_TOGGLE_ROLLERSHUTTER);
        verifyAction(profile, UnDefType.NULL, UpDownType.UP);
        verifyAction(profile, UpDownType.UP, UpDownType.DOWN);
        verifyAction(profile, UpDownType.DOWN, UpDownType.UP);
    }

    @Test
    public void testPlayerItem() {
        prepareContextMock();
        TriggerProfile profile = newToggleProfile(RAWBUTTON_TOGGLE_PLAYER);
        verifyAction(profile, UnDefType.NULL, PlayPauseType.PLAY);
        verifyAction(profile, PlayPauseType.PLAY, PlayPauseType.PAUSE);
        verifyAction(profile, PlayPauseType.PAUSE, PlayPauseType.PLAY);

        profile = newToggleProfile(BUTTON_TOGGLE_PLAYER);
        verifyAction(profile, UnDefType.NULL, PlayPauseType.PLAY);
        verifyAction(profile, PlayPauseType.PLAY, PlayPauseType.PAUSE);
        verifyAction(profile, PlayPauseType.PAUSE, PlayPauseType.PLAY);
    }

    @Test
    public void testCorrectUserConfiguredEvent() {
        ListAppender<ILoggingEvent> logAppender = createLogAppender();

        initializeContextMock("RELEASED");
        TriggerProfile profile = newToggleProfile(RAWBUTTON_TOGGLE_SWITCH);
        assertEquals(0, logAppender.list.size());
        verifyAction(profile, UnDefType.NULL, OnOffType.ON, "RELEASED");
        verifyAction(profile, OnOffType.ON, OnOffType.OFF, "RELEASED");
        verifyAction(profile, OnOffType.OFF, OnOffType.ON, "RELEASED");
    }

    @Test
    public void testWrongUserConfiguredEvent() {
        ListAppender<ILoggingEvent> logAppender = createLogAppender();
        initializeContextMock("SHORT_PRESSED");
        TriggerProfile profile = newToggleProfile(RAWBUTTON_TOGGLE_SWITCH);

        assertEquals(Level.WARN, logAppender.list.get(0).getLevel());
        verifyAction(profile, OnOffType.ON, OnOffType.OFF);
    }

    private void initializeContextMock(@Nullable String triggerEvent) {
        Map<String, Object> params = triggerEvent == null ? Collections.emptyMap()
                : Collections.singletonMap(ToggleProfile.EVENT_PARAM, triggerEvent);
        when(contextMock.getConfiguration()).thenReturn(new Configuration(params));
    }

    private void prepareContextMock() {
        initializeContextMock(null);
    }

    private ListAppender<ILoggingEvent> createLogAppender() {
        ch.qos.logback.classic.Logger toggleProfileLogger = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(ToggleProfile.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        toggleProfileLogger.addAppender(listAppender);

        return listAppender;
    }

    private @Nullable TriggerProfile newToggleProfile(ProfileTypeUID profileUID) {
        when(systemProfileFactory.createProfile(profileUID, callbackMock, contextMock)).thenCallRealMethod();
        return (TriggerProfile) systemProfileFactory.createProfile(profileUID, callbackMock, contextMock);
    }

    private void verifyAction(@Nullable TriggerProfile profile, State preCondition, Command expectation,
            String triggerEvent) {
        reset(callbackMock);
        profile.onStateUpdateFromItem(preCondition);
        profile.onTriggerFromHandler(triggerEvent);
        verify(callbackMock, times(1)).sendCommand(eq(expectation));
    }

    private void verifyAction(@Nullable TriggerProfile profile, State preCondition, Command expectation) {
        verifyAction(profile, preCondition, expectation,
                profile.getProfileTypeUID().getAsString().contains("rawbutton") ? CommonTriggerEvents.PRESSED
                        : CommonTriggerEvents.SHORT_PRESSED);
    }
}
