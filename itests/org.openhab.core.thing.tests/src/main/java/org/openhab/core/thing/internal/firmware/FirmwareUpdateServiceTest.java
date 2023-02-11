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
package org.openhab.core.thing.internal.firmware;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.openhab.core.thing.firmware.Constants.*;
import static org.openhab.core.thing.firmware.FirmwareStatusInfo.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.config.core.validation.ConfigDescriptionValidator;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateBackgroundTransferHandler;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.openhab.core.thing.binding.firmware.ProgressCallback;
import org.openhab.core.thing.binding.firmware.ProgressStep;
import org.openhab.core.thing.firmware.FirmwareRegistry;
import org.openhab.core.thing.firmware.FirmwareStatusInfo;
import org.openhab.core.thing.firmware.FirmwareStatusInfoEvent;
import org.openhab.core.thing.firmware.FirmwareUpdateProgressInfoEvent;
import org.openhab.core.thing.firmware.FirmwareUpdateResult;
import org.openhab.core.thing.firmware.FirmwareUpdateResultInfoEvent;
import org.openhab.core.thing.firmware.FirmwareUpdateService;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;

/**
 * Testing the {@link FirmwareUpdateService}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Simon Kaufmann - Converted to standalone Java tests
 * @author Dimitar Ivanov - Added a test for valid cancel execution during firmware update; Replaced Firmware UID with
 *         thing UID and firmware version
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class FirmwareUpdateServiceTest extends JavaOSGiTest {

    public static final ProgressStep[] SEQUENCE = new ProgressStep[] { ProgressStep.REBOOTING, ProgressStep.DOWNLOADING,
            ProgressStep.TRANSFERRING, ProgressStep.UPDATING };

    private @NonNullByDefault({}) Thing thing1;
    private @NonNullByDefault({}) Thing thing2;
    private @NonNullByDefault({}) Thing thing3;

    private @NonNullByDefault({}) FirmwareUpdateServiceImpl firmwareUpdateService;

    private @Mock @NonNullByDefault({}) FirmwareRegistry firmwareRegistryMock;
    private @Mock @NonNullByDefault({}) FirmwareUpdateHandler handler1Mock;
    private @Mock @NonNullByDefault({}) FirmwareUpdateHandler handler2Mock;
    private @Mock @NonNullByDefault({}) FirmwareUpdateHandler handler3Mock;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;
    private @Mock @NonNullByDefault({}) LocaleProvider localeProviderMock;
    private @Mock @NonNullByDefault({}) TranslationProvider translationProviderMock;
    private @Mock @NonNullByDefault({}) ConfigDescriptionValidator configDescriptionValidatorMock;
    private @Mock @NonNullByDefault({}) BundleResolver bundleResolverMock;

    @BeforeEach
    public void beforeEach() {
        Map<String, String> props1 = new HashMap<>();
        props1.put(Thing.PROPERTY_FIRMWARE_VERSION, V111);
        props1.put(Thing.PROPERTY_MODEL_ID, MODEL1);
        props1.put(Thing.PROPERTY_VENDOR, VENDOR1);
        thing1 = ThingBuilder.create(THING_TYPE_UID1, THING1_ID).withProperties(props1).build();

        Map<String, String> props2 = new HashMap<>();
        props2.put(Thing.PROPERTY_FIRMWARE_VERSION, V112);
        props2.put(Thing.PROPERTY_MODEL_ID, MODEL1);
        props2.put(Thing.PROPERTY_VENDOR, VENDOR1);
        thing2 = ThingBuilder.create(THING_TYPE_UID1, THING2_ID).withProperties(props2).build();

        Map<String, String> props3 = new HashMap<>();
        props3.put(Thing.PROPERTY_FIRMWARE_VERSION, VALPHA);
        props3.put(Thing.PROPERTY_MODEL_ID, MODEL2);
        props3.put(Thing.PROPERTY_VENDOR, VENDOR2);
        thing3 = ThingBuilder.create(THING_TYPE_UID2, THING3_ID).withProperties(props3).build();

        SafeCaller safeCaller = getService(SafeCaller.class);
        assertNotNull(safeCaller);

        firmwareUpdateService = new FirmwareUpdateServiceImpl(bundleResolverMock, configDescriptionValidatorMock,
                eventPublisherMock, firmwareRegistryMock, translationProviderMock, localeProviderMock, safeCaller);

        handler1Mock = addHandler(thing1);
        handler2Mock = addHandler(thing2);
        handler3Mock = addHandler(thing3);

        when(localeProviderMock.getLocale()).thenReturn(Locale.ENGLISH);

        initialFirmwareRegistryMocking();

        when(bundleResolverMock.resolveBundle(any())).thenReturn(mock(Bundle.class));
    }

    private void initialFirmwareRegistryMocking() {
        when(firmwareRegistryMock.getFirmware(eq(thing1), eq(V009))).thenReturn(FW009_EN);

        when(firmwareRegistryMock.getFirmware(eq(thing1), eq(V111))).thenReturn(FW111_EN);
        when(firmwareRegistryMock.getFirmware(eq(thing2), eq(V111))).thenReturn(FW111_EN);
        when(firmwareRegistryMock.getFirmware(eq(thing3), eq(V111))).thenReturn(FW111_EN);

        when(firmwareRegistryMock.getFirmware(eq(thing1), eq(V112), any())).thenReturn(FW112_EN);
        when(firmwareRegistryMock.getFirmware(eq(thing1), eq(V112))).thenReturn(FW112_EN);

        when(firmwareRegistryMock.getFirmware(eq(thing2), eq(VALPHA))).thenReturn(FWALPHA_EN);
        when(firmwareRegistryMock.getFirmware(eq(thing3), eq(VALPHA))).thenReturn(FWALPHA_EN);

        Answer<?> firmwaresAnswer = invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID_WITHOUT_FW.equals(thing.getThingTypeUID())
                    || THING_TYPE_UID2.equals(thing.getThingTypeUID())
                    || THING_TYPE_UID3.equals(thing.getThingTypeUID())) {
                return Collections.emptySet();
            } else {
                Supplier<TreeSet<Firmware>> supplier = () -> new TreeSet<>();
                return Stream.of(FW009_EN, FW111_EN, FW112_EN).collect(Collectors.toCollection(supplier));
            }
        };

        when(firmwareRegistryMock.getFirmwares(any(Thing.class))).then(firmwaresAnswer);
        when(firmwareRegistryMock.getFirmwares(any(Thing.class), any())).then(firmwaresAnswer);
    }

    @AfterEach
    public void tearDown() {
        firmwareUpdateService.deactivate();
    }

    @Test
    public void testGetFirmwareStatusInfo() {
        final FirmwareStatusInfo unknownInfo = createUnknownInfo(thing2.getUID());
        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing2.getUID());
        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V111));
        assertThat(thing2.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V112));

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING2_UID), is(upToDateInfo));
        thing2.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, null);
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING2_UID), is(unknownInfo));

        // verify that the corresponding events are sent
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(3)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(0), updateExecutableInfoFw112);
        assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(1), upToDateInfo);
        assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(2), unknownInfo);
    }

    @Test
    public void testGetFirmwareStatusInfoNoFirmwareProvider() {
        final FirmwareStatusInfo unknownInfo = createUnknownInfo(thing3.getUID());
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING3_UID), is(unknownInfo));

        // verify that the corresponding events are sent
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(1)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING3_UID, eventCaptor.getValue(), unknownInfo);
    }

    @Test
    public void testGetFirmwareStatusInfoFirmwareProviderAddedLate() {
        final FirmwareStatusInfo unknownInfo = createUnknownInfo(thing3.getUID());
        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing3.getUID());
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING3_UID), is(unknownInfo));

        when(firmwareRegistryMock.getFirmware(eq(thing2), eq(VALPHA))).thenReturn(FWALPHA_EN);
        when(firmwareRegistryMock.getFirmwares(any(Thing.class))).thenAnswer(invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID_WITHOUT_FW.equals(thing.getThingTypeUID())
                    || THING_TYPE_UID1.equals(thing.getThingTypeUID())) {
                return Collections.emptySet();
            } else {
                return Set.of(FWALPHA_EN);
            }
        });

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING3_UID), is(upToDateInfo));

        // verify that the corresponding events are sent
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(2)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING3_UID, eventCaptor.getAllValues().get(0), unknownInfo);
        assertFirmwareStatusInfoEvent(THING3_UID, eventCaptor.getAllValues().get(1), upToDateInfo);
    }

    @Test
    public void testGetFirmwareStatusInfoEventsOnlySentOnce() {
        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));

        // verify that the corresponding events are sent
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(1)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(0), updateExecutableInfoFw112);
    }

    @Test
    public void firmwareStatusPropagatedRegularly() throws Exception {
        final FirmwareStatusInfo unknownInfo = createUnknownInfo(thing3.getUID());
        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing2.getUID());
        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V111));
        assertThat(thing2.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V112));

        updateConfig(1, 0, TimeUnit.SECONDS);

        waitForAssert(() -> {
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventPublisherMock, times(3)).post(eventCaptor.capture());
            assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(0), updateExecutableInfoFw112);
            assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(1), upToDateInfo);
            assertFirmwareStatusInfoEvent(THING3_UID, eventCaptor.getAllValues().get(2), unknownInfo);

        });

        reset(eventPublisherMock);

        // Simulate addition of extra firmware provider
        when(firmwareRegistryMock.getFirmware(eq(thing2), eq(VALPHA))).thenReturn(null);
        when(firmwareRegistryMock.getFirmwares(any(Thing.class))).thenAnswer(invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID_WITHOUT_FW.equals(thing.getThingTypeUID())
                    || THING_TYPE_UID2.equals(thing.getThingTypeUID())) {
                return Collections.emptySet();
            } else {
                return Set.of(FW113_EN);
            }
        });

        waitForAssert(() -> {
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventPublisherMock, times(2)).post(eventCaptor.capture());

            FirmwareStatusInfo updateExecutableInfoFw113 = createUpdateExecutableInfo(thing1.getUID(), V113);
            assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(0), updateExecutableInfoFw113);

            updateExecutableInfoFw113 = createUpdateExecutableInfo(thing2.getUID(), V113);
            assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(1), updateExecutableInfoFw113);
        });

        reset(eventPublisherMock);

        // Simulate removed firmware provider - get back everything as it was initially
        initialFirmwareRegistryMocking();

        waitForAssert(() -> {
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventPublisherMock, times(2)).post(eventCaptor.capture());
            assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(0), updateExecutableInfoFw112);
            assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(1), upToDateInfo);
        });
    }

    @Test
    public void testUpdateFirmware() {
        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing1.getUID());
        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));

        firmwareUpdateService.updateFirmware(THING1_UID, V112, null);

        waitForAssert(() -> {
            assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V112.toString()));
        });

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(upToDateInfo));

        // verify that the corresponding events are sent
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(2)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(0), updateExecutableInfoFw112);
        assertFirmwareStatusInfoEvent(THING1_UID, eventCaptor.getAllValues().get(1), upToDateInfo);
    }

    @Test
    public void testCancelFirmwareUpdate() {
        firmwareUpdateService.updateFirmware(THING1_UID, V111, Locale.ENGLISH);
        firmwareUpdateService.updateFirmware(THING2_UID, V111, Locale.ENGLISH);
        firmwareUpdateService.updateFirmware(THING3_UID, VALPHA, Locale.ENGLISH);

        firmwareUpdateService.cancelFirmwareUpdate(THING3_UID);

        waitForAssert(() -> {
            verify(handler1Mock, times(0)).cancel();
            verify(handler2Mock, times(0)).cancel();
            verify(handler3Mock, times(1)).cancel();
        });
    }

    @Test
    public void testCancelFirmwareUpdateIntheMiddleOfUpdate() {
        final long stepsTime = 10;
        final int numberOfSteps = SEQUENCE.length;
        final AtomicBoolean isUpdateFinished = new AtomicBoolean(false);

        doAnswer(invocation -> {
            ProgressCallback progressCallback = (ProgressCallback) invocation.getArguments()[1];
            progressCallback.defineSequence(SEQUENCE);

            // Simulate update steps with delay
            for (int updateStepsCount = 0; updateStepsCount < numberOfSteps; updateStepsCount++) {
                progressCallback.next();
                Thread.sleep(stepsTime);
            }
            progressCallback.success();
            isUpdateFinished.set(true);
            return null;
        }).when(handler1Mock).updateFirmware(any(Firmware.class), any(ProgressCallback.class));

        // Execute update and cancel it immediately
        firmwareUpdateService.updateFirmware(THING1_UID, V112, null);
        firmwareUpdateService.cancelFirmwareUpdate(THING1_UID);

        // Be sure that the cancel is executed before the completion of the update
        waitForAssert(() -> {
            verify(handler1Mock, times(1)).cancel();
        }, stepsTime * numberOfSteps, stepsTime);

        assertThat(isUpdateFinished.get(), is(false));
    }

    @Test
    public void cancelFirmwareUpdateNoFirmwareUpdateHandler() {
        assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.cancelFirmwareUpdate(new ThingUID("dummy:thing:withoutHandler")));
    }

    @Test
    public void cancelFirmwareUpdateThingIdIdNull() {
        assertThrows(IllegalArgumentException.class, () -> firmwareUpdateService.cancelFirmwareUpdate(giveNull()));
    }

    @Test
    public void cancelFirmwareUpdateUpdateNotStarted() {
        assertThrows(IllegalStateException.class, () -> firmwareUpdateService.cancelFirmwareUpdate(THING3_UID));
    }

    @Test
    public void testCancelFirmwareUpdateUnexpectedFailure() {
        FirmwareUpdateHandler firmwareUpdateHandler = mock(FirmwareUpdateHandler.class);
        doThrow(new RuntimeException()).when(firmwareUpdateHandler).cancel();
        doReturn(true).when(firmwareUpdateHandler).isUpdateExecutable();

        Map<String, String> props4 = new HashMap<>();
        props4.put(Thing.PROPERTY_MODEL_ID, MODEL1);
        props4.put(Thing.PROPERTY_VENDOR, VENDOR1);

        Thing thing4 = ThingBuilder.create(THING_TYPE_UID1, THING4_UID).withProperties(props4).build();

        when(firmwareUpdateHandler.getThing()).thenReturn(thing4);

        firmwareUpdateService.addFirmwareUpdateHandler(firmwareUpdateHandler);

        when(firmwareRegistryMock.getFirmware(eq(thing4), eq(V111))).thenReturn(FW111_EN);

        assertCancellationMessage("unexpected-handler-error-during-cancel", "english", Locale.ENGLISH, 1);

        when(localeProviderMock.getLocale()).thenReturn(Locale.FRENCH);
        assertCancellationMessage("unexpected-handler-error-during-cancel", "français", Locale.FRENCH, 2);

        when(localeProviderMock.getLocale()).thenReturn(Locale.GERMAN);
        assertCancellationMessage("unexpected-handler-error-during-cancel", "deutsch", Locale.GERMAN, 3);
    }

    @Test
    public void testCancelFirmwareUpdateTakesLong() {
        firmwareUpdateService.timeout = 50;
        FirmwareUpdateHandler firmwareUpdateHandler = mock(FirmwareUpdateHandler.class);
        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(firmwareUpdateHandler).cancel();
        doReturn(true).when(firmwareUpdateHandler).isUpdateExecutable();

        Map<String, String> props4 = new HashMap<>();
        props4.put(Thing.PROPERTY_MODEL_ID, MODEL1);
        props4.put(Thing.PROPERTY_VENDOR, VENDOR1);

        Thing thing4 = ThingBuilder.create(THING_TYPE_UID1, THING4_UID).withProperties(props4).build();

        when(firmwareUpdateHandler.getThing()).thenReturn(thing4);

        when(firmwareRegistryMock.getFirmware(eq(thing4), eq(V111))).thenReturn(FW111_EN);

        firmwareUpdateService.addFirmwareUpdateHandler(firmwareUpdateHandler);

        assertCancellationMessage("timeout-error-during-cancel", "english", Locale.ENGLISH, 1);

        when(localeProviderMock.getLocale()).thenReturn(Locale.FRENCH);
        assertCancellationMessage("timeout-error-during-cancel", "français", Locale.FRENCH, 2);

        when(localeProviderMock.getLocale()).thenReturn(Locale.GERMAN);
        assertCancellationMessage("timeout-error-during-cancel", "deutsch", Locale.GERMAN, 3);
    }

    @Test
    public void testUpdateFirmwareDowngrade() {
        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing2.getUID());
        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing2.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING2_UID), is(upToDateInfo));

        firmwareUpdateService.updateFirmware(THING2_UID, V111, null);

        waitForAssert(() -> {
            assertThat(thing2.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V111.toString()));
        });

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING2_UID), is(updateExecutableInfoFw112));

        // verify that the corresponding events are sent
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(2)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(0), upToDateInfo);
        assertFirmwareStatusInfoEvent(THING2_UID, eventCaptor.getAllValues().get(1), updateExecutableInfoFw112);
    }

    @Test
    public void testGetFirmwareStatusInfoNull() {
        assertThrows(IllegalArgumentException.class, () -> firmwareUpdateService.getFirmwareStatusInfo(giveNull()));
    }

    @Test
    public void testGetFirmwareStatusInfoNull2() {
        assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.updateFirmware(giveNull(), V009, null));
    }

    @Test
    public void testGetFirmwareStatusInfoNull3() {
        assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.updateFirmware(THING1_UID, giveNull(), null));
    }

    @Test
    public void testGetFirmwareStatusInfoNotFirmwareUpdateHandler() {
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(UNKNOWN_THING_UID), is(nullValue()));
    }

    @Test
    public void testUpdateFirmwareUnknownFirmware() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.updateFirmware(THING1_UID, UNKNOWN_FIRMWARE_VERSION, null));

        assertThat(exception.getMessage(),
                is(String.format("Firmware with version %s for thing with UID %s was not found.",
                        UNKNOWN_FIRMWARE_VERSION, THING1_UID)));
    }

    @Test
    public void testUpdateFirmwareNoFirmwareUpdateHandler() {
        Thing thing4 = ThingBuilder.create(THING_TYPE_UID_WITHOUT_FW, THING5_ID).build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.updateFirmware(thing4.getUID(), V009, null));

        assertThat(exception.getMessage(),
                is(String.format("There is no firmware update handler for thing with UID %s.", thing4.getUID())));
    }

    @Test
    public void testUpdateFirmwareNotExecutable() {
        doReturn(false).when(handler1Mock).isUpdateExecutable();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> firmwareUpdateService.updateFirmware(THING1_UID, V112, null));

        assertThat(exception.getMessage(),
                is(String.format("The firmware update of thing with UID %s is not executable.", THING1_UID)));
    }

    @Test
    public void testPrerequisiteVersionCheckIllegalVersion() {
        when(firmwareRegistryMock.getFirmware(any(Thing.class), any(String.class))).thenAnswer(invocation -> {
            String firmwareVersion = (String) invocation.getArguments()[1];
            if (V111_FIX.equals(firmwareVersion)) {
                return FW111_FIX_EN;
            } else if (V113.equals(firmwareVersion)) {
                return FW113_EN;
            } else {
                return null;
            }

        });

        when(firmwareRegistryMock.getFirmwares(any(Thing.class))).thenAnswer(invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID_WITHOUT_FW.equals(thing.getThingTypeUID())
                    || THING_TYPE_UID2.equals(thing.getThingTypeUID())) {
                return Collections.emptySet();
            } else {
                Supplier<TreeSet<Firmware>> supplier = () -> new TreeSet<>();
                return Stream.of(FW111_FIX_EN, FW113_EN).collect(Collectors.toCollection(supplier));
            }
        });

        when(firmwareRegistryMock.getFirmware(any(Thing.class), eq(V113))).thenReturn(FW113_EN);

        final FirmwareStatusInfo updateExecutableInfoFw113 = createUpdateExecutableInfo(thing1.getUID(), V113);
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw113));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.updateFirmware(THING1_UID, V113, null));
        assertThat(exception.getMessage(),
                is(String.format("Firmware %s is not suitable for thing with UID %s.", FW113_EN, THING1_UID)));
    }

    @Test
    public void testPrerequisiteVersionCheck() {
        when(firmwareRegistryMock.getFirmware(any(Thing.class), any(String.class), any())).thenAnswer(invocation -> {
            String version = (String) invocation.getArguments()[1];
            if (V111_FIX.equals(version)) {
                return FW111_FIX_EN;
            } else if (V113.equals(version)) {
                return FW113_EN;
            } else {
                return null;
            }

        });

        Answer<?> firmwaresAnswer = invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID_WITHOUT_FW.equals(thing.getThingTypeUID())
                    || THING_TYPE_UID2.equals(thing.getThingTypeUID())) {
                return Collections.emptySet();
            } else {
                Supplier<TreeSet<Firmware>> supplier = () -> new TreeSet<>();
                return Stream.of(FW111_FIX_EN, FW113_EN).collect(Collectors.toCollection(supplier));
            }
        };

        when(firmwareRegistryMock.getFirmwares(any(Thing.class), any())).thenAnswer(firmwaresAnswer);
        when(firmwareRegistryMock.getFirmwares(any(Thing.class))).thenAnswer(firmwaresAnswer);
        when(firmwareRegistryMock.getFirmware(any(Thing.class), eq(V111_FIX))).thenReturn(FW111_FIX_EN);
        when(firmwareRegistryMock.getFirmware(any(Thing.class), eq(V113))).thenReturn(FW113_EN);

        final FirmwareStatusInfo updateExecutableInfoFw113 = createUpdateExecutableInfo(thing1.getUID(), V113);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw113));

        firmwareUpdateService.updateFirmware(THING1_UID, FW111_FIX_EN.getVersion(), null);

        waitForAssert(() -> {
            assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V111_FIX));
            assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw113));
        });

        firmwareUpdateService.updateFirmware(THING1_UID, FW113_EN.getVersion(), null);

        final FirmwareStatusInfo upToDateInfo1 = createUpToDateInfo(thing1.getUID());
        final FirmwareStatusInfo updateExecutableInfoFw1132 = createUpdateExecutableInfo(thing2.getUID(), V113);

        waitForAssert(() -> {
            assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V113));
            assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(upToDateInfo1));
            assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING2_UID), is(updateExecutableInfoFw1132));
        });

        firmwareUpdateService.updateFirmware(THING2_UID, FW113_EN.getVersion(), null);
        final FirmwareStatusInfo upToDateInfo2 = createUpToDateInfo(thing2.getUID());

        waitForAssert(() -> {
            assertThat(thing2.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V113));
            assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING2_UID), is(upToDateInfo2));
        });
    }

    @Test
    public void testFirmwareStatusUpdateIsNotExecutable() {
        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));

        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing1.getUID());
        final FirmwareStatusInfo updateAvailableInfo = createUpdateAvailableInfo(thing1.getUID());

        doReturn(false).when(handler1Mock).isUpdateExecutable();
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateAvailableInfo));

        doReturn(true).when(handler1Mock).isUpdateExecutable();
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));

        firmwareUpdateService.updateFirmware(THING1_UID, FW112_EN.getVersion(), null);

        waitForAssert(() -> {
            assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V112));
        });

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(upToDateInfo));
    }

    @Test
    public void testUpdateFirmwareWrongModel() {
        when(firmwareRegistryMock.getFirmware(any(Thing.class), eq(FWALPHA_RESTRICTED_TO_MODEL2.getVersion()), any()))
                .thenReturn(FWALPHA_RESTRICTED_TO_MODEL2);
        when(firmwareRegistryMock.getFirmwares(any(Thing.class), any())).thenAnswer(invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID1.equals(thing.getThingTypeUID())) {
                return Set.of(FWALPHA_RESTRICTED_TO_MODEL2);
            } else {
                return Collections.emptySet();
            }
        });

        Map<String, String> props1 = new HashMap<>();
        props1.put(Thing.PROPERTY_MODEL_ID, MODEL1);

        thing1.setProperties(props1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> firmwareUpdateService.updateFirmware(THING1_UID, VALPHA, null));

        assertThat(exception.getMessage(),
                is(String.format("Firmware with version %s for thing with UID %s was not found.", VALPHA, THING1_UID)));
    }

    @Test
    public void testEvents() {
        doAnswer(invocation -> {
            Firmware firmware = (Firmware) invocation.getArguments()[0];
            ProgressCallback progressCallback = (ProgressCallback) invocation.getArguments()[1];

            progressCallback.defineSequence(SEQUENCE);
            progressCallback.next();
            progressCallback.next();
            progressCallback.next();
            progressCallback.next();
            thing1.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, firmware.getVersion());
            return null;
        }).when(handler1Mock).updateFirmware(any(Firmware.class), any(ProgressCallback.class));

        // getFirmwareStatusInfo() method will internally generate and post one FirmwareStatusInfoEvent event.

        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));
        firmwareUpdateService.updateFirmware(THING1_UID, FW112_EN.getVersion(), null);

        AtomicReference<List<Event>> events = new AtomicReference<>(new ArrayList<>());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        waitForAssert(() -> {
            // Wait for four FirmwareUpdateProgressInfoEvents plus one FirmwareStatusInfoEvent event.
            verify(eventPublisherMock, atLeast(SEQUENCE.length + 1)).post(eventCaptor.capture());
        });
        events.get().addAll(eventCaptor.getAllValues());
        List<Event> list = events.get().stream().filter(event -> event instanceof FirmwareUpdateProgressInfoEvent)
                .collect(Collectors.toList());
        assertTrue(list.size() >= SEQUENCE.length);
        for (int i = 0; i < SEQUENCE.length; i++) {
            FirmwareUpdateProgressInfoEvent event = (FirmwareUpdateProgressInfoEvent) list.get(i);
            assertThat(event.getTopic(), containsString(THING1_UID.getAsString()));
            assertThat(event.getProgressInfo().getThingUID(), is(THING1_UID));
            assertThat(event.getProgressInfo().getProgressStep(), is(SEQUENCE[i]));
        }
    }

    @Test
    public void testUpdateFirmwareTimeOut() {
        firmwareUpdateService.timeout = 50;

        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));
        waitForAssert(() -> {
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventPublisherMock, times(1)).post(eventCaptor.capture());
        });

        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(handler1Mock).updateFirmware(any(Firmware.class), any(ProgressCallback.class));

        assertResultInfoEvent(THING1_UID, FW112_EN, "timeout-error", Locale.ENGLISH, "english", 1);
        assertResultInfoEvent(THING1_UID, FW112_EN, "timeout-error", Locale.GERMAN, "deutsch", 2);
    }

    @Test
    public void testUpdateFirmwareError() {
        doAnswer(invocation -> {
            ProgressCallback progressCallback = (ProgressCallback) invocation.getArguments()[1];
            progressCallback.defineSequence(SEQUENCE);
            progressCallback.next();
            progressCallback.next();
            progressCallback.next();
            progressCallback.next();
            try {
                progressCallback.next();
            } catch (NoSuchElementException e) {
                fail("Unexcepted exception thrown");
            }
            return null;
        }).when(handler1Mock).updateFirmware(any(Firmware.class), any(ProgressCallback.class));

        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));

        assertResultInfoEvent(THING1_UID, FW112_EN, "unexpected-handler-error", Locale.ENGLISH, "english", 1);
        assertResultInfoEvent(THING1_UID, FW112_EN, "unexpected-handler-error", Locale.GERMAN, "deutsch", 2);

        assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V111.toString()));
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));
    }

    @Test
    public void testUpdateFirmwareCustomError() {
        doAnswer(invocation -> {
            ProgressCallback progressCallback = (ProgressCallback) invocation.getArguments()[1];
            progressCallback.failed("test-error");
            return null;
        }).when(handler1Mock).updateFirmware(any(Firmware.class), any(ProgressCallback.class));

        final FirmwareStatusInfo updateExecutableInfoFw112 = createUpdateExecutableInfo(thing1.getUID(), V112);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));

        assertResultInfoEvent(THING1_UID, FW112_EN, "test-error", Locale.ENGLISH, "english", 1);
        assertResultInfoEvent(THING1_UID, FW112_EN, "test-error", Locale.GERMAN, "deutsch", 2);

        assertThat(thing1.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V111.toString()));
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING1_UID), is(updateExecutableInfoFw112));
    }

    @Test
    public void testIndependentHandlers() {
        final FirmwareStatusInfo unknownInfo = createUnknownInfo(UNKNOWN_THING_UID);
        FirmwareUpdateHandler firmwareUpdateHandler = mock(FirmwareUpdateHandler.class);
        when(firmwareUpdateHandler.getThing())
                .thenReturn(ThingBuilder.create(THING_TYPE_UID3, UNKNOWN_THING_UID).build());

        firmwareUpdateService.addFirmwareUpdateHandler(firmwareUpdateHandler);

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(UNKNOWN_THING_UID), is(unknownInfo));
    }

    @Test
    public void testInvalidValuesAreRejected() throws Exception {
        int originalPeriod = firmwareUpdateService.getFirmwareStatusInfoJobPeriod();
        int originalDelay = firmwareUpdateService.getFirmwareStatusInfoJobDelay();
        TimeUnit originalTimeUnit = firmwareUpdateService.getFirmwareStatusInfoJobTimeUnit();

        assertThrows(IllegalArgumentException.class, () -> {
            updateInvalidConfigAndAssert(0, 0, TimeUnit.SECONDS, originalPeriod, originalDelay, originalTimeUnit);
            updateInvalidConfigAndAssert(1, -1, TimeUnit.SECONDS, originalPeriod, originalDelay, originalTimeUnit);
            updateInvalidConfigAndAssert(1, 0, TimeUnit.NANOSECONDS, originalPeriod, originalDelay, originalTimeUnit);
        });
    }

    private volatile AtomicBoolean updateExecutable = new AtomicBoolean(false);

    @Test
    public void testBackgroundTransfer() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put(Thing.PROPERTY_FIRMWARE_VERSION, V111);
        props.put(Thing.PROPERTY_VENDOR, VENDOR1);
        props.put(Thing.PROPERTY_MODEL_ID, MODEL1);
        Thing thing4 = ThingBuilder.create(THING_TYPE_UID3, THING4_ID).withProperties(props).build();

        final FirmwareStatusInfo unknownInfo = createUnknownInfo(thing4.getUID());
        final FirmwareStatusInfo updateAvailableInfo = createUpdateAvailableInfo(thing4.getUID());

        FirmwareUpdateBackgroundTransferHandler handler4 = mock(FirmwareUpdateBackgroundTransferHandler.class);
        when(handler4.getThing()).thenReturn(thing4);
        doAnswer(invocation -> {
            return updateExecutable.get();
        }).when(handler4).isUpdateExecutable();
        doAnswer(invocation -> {
            Firmware firmware = (Firmware) invocation.getArguments()[0];
            thing4.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, firmware.getVersion());
            updateExecutable.set(false);
            return null;
        }).when(handler4).updateFirmware(any(Firmware.class), any(ProgressCallback.class));

        firmwareUpdateService.addFirmwareUpdateHandler(handler4);

        doAnswer(invocation -> {
            assertDoesNotThrow(() -> Thread.sleep(25));
            updateExecutable.set(true);
            return null;
        }).when(handler4).transferFirmware(any(Firmware.class));

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING4_UID), is(unknownInfo));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, times(1)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING4_UID, eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1),
                unknownInfo);

        when(firmwareRegistryMock.getFirmware(any(Thing.class), eq(FW120_EN.getVersion()))).thenReturn(FW120_EN);
        when(firmwareRegistryMock.getFirmwares(any(Thing.class))).thenAnswer(invocation -> {
            Thing thing = (Thing) invocation.getArguments()[0];
            if (THING_TYPE_UID3.equals(thing.getThingTypeUID())) {
                return Set.of(FW120_EN);
            } else {
                return Set.of();
            }
        });

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING4_UID), is(updateAvailableInfo));
        verify(eventPublisherMock, times(2)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING4_UID, eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1),
                updateAvailableInfo);

        final FirmwareStatusInfo updateExecutableInfoFw120 = createUpdateExecutableInfo(thing4.getUID(), V120);

        waitForAssert(() -> {
            assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING4_UID), is(updateExecutableInfoFw120));
            verify(eventPublisherMock, times(3)).post(eventCaptor.capture());
            assertFirmwareStatusInfoEvent(THING4_UID,
                    eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1), updateExecutableInfoFw120);
        });

        firmwareUpdateService.updateFirmware(THING4_UID, FW120_EN.getVersion(), null);

        waitForAssert(() -> {
            assertThat(thing4.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION), is(V120));
        });

        final FirmwareStatusInfo upToDateInfo = createUpToDateInfo(thing4.getUID());

        assertThat(firmwareUpdateService.getFirmwareStatusInfo(THING4_UID), is(upToDateInfo));
        verify(eventPublisherMock, times(4)).post(eventCaptor.capture());
        assertFirmwareStatusInfoEvent(THING4_UID, eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1),
                upToDateInfo);

        assertThat(handler4.isUpdateExecutable(), is(false));
    }

    private void updateInvalidConfigAndAssert(int period, int delay, TimeUnit timeUnit, int expectedPeriod,
            int expectedDelay, TimeUnit expectedTimeUnit) throws IOException {
        updateConfig(period, delay, timeUnit);
        waitForAssert(() -> {
            assertThat(firmwareUpdateService.getFirmwareStatusInfoJobPeriod(), is(expectedPeriod));
            assertThat(firmwareUpdateService.getFirmwareStatusInfoJobDelay(), is(expectedDelay));
            assertThat(firmwareUpdateService.getFirmwareStatusInfoJobTimeUnit(), is(expectedTimeUnit));
        });
    }

    private void assertResultInfoEvent(ThingUID thingUID, Firmware firmware, String messageKey, Locale locale,
            String text, int expectedEventCount) {
        when(localeProviderMock.getLocale()).thenReturn(locale);
        when(translationProviderMock.getText(any(), eq(messageKey), any(), eq(locale), any())).thenReturn(text);

        firmwareUpdateService.updateFirmware(thingUID, firmware.getVersion(), locale);
        waitForAssert(() -> {
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventPublisherMock, atLeast(expectedEventCount)).post(eventCaptor.capture());
            List<Event> allValues = eventCaptor.getAllValues().stream()
                    .filter(e -> e instanceof FirmwareUpdateResultInfoEvent).collect(Collectors.toList());
            assertEquals(expectedEventCount, allValues.size());
            assertFailedFirmwareUpdate(THING1_UID, allValues.get(expectedEventCount - 1), text);
        });
    }

    private void assertCancellationMessage(String messageKey, String expectedEnglishMessage, Locale locale,
            int expectedEventCount) {
        when(localeProviderMock.getLocale()).thenReturn(locale);

        when(translationProviderMock.getText(any(Bundle.class), eq(messageKey), any(), eq(locale), any()))
                .thenReturn(expectedEnglishMessage);

        Exception exception = new RuntimeException();

        doThrow(exception).when(handler3Mock).cancel();

        firmwareUpdateService.updateFirmware(THING4_UID, FW111_EN.getVersion(), locale);
        firmwareUpdateService.cancelFirmwareUpdate(THING4_UID);

        AtomicReference<FirmwareUpdateResultInfoEvent> resultEvent = new AtomicReference<>();
        waitForAssert(() -> {
            ArgumentCaptor<FirmwareUpdateResultInfoEvent> eventCaptor = ArgumentCaptor
                    .forClass(FirmwareUpdateResultInfoEvent.class);
            verify(eventPublisherMock, times(expectedEventCount)).post(eventCaptor.capture());
            assertEquals(expectedEventCount, eventCaptor.getAllValues().size());
            resultEvent.set(eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1));
        });
        assertThat(resultEvent.get().getFirmwareUpdateResultInfo().getThingUID(), is(THING4_UID));
        assertThat(resultEvent.get().getFirmwareUpdateResultInfo().getResult(), is(FirmwareUpdateResult.ERROR));
        assertThat(resultEvent.get().getFirmwareUpdateResultInfo().getErrorMessage(), is(expectedEnglishMessage));
    }

    private void assertFirmwareStatusInfoEvent(ThingUID thingUID, Event event, FirmwareStatusInfo expectedInfo) {
        assertThat(event, is(instanceOf(FirmwareStatusInfoEvent.class)));
        FirmwareStatusInfoEvent firmwareStatusInfoEvent = (FirmwareStatusInfoEvent) event;

        assertThat(firmwareStatusInfoEvent.getTopic(), containsString(thingUID.getAsString()));
        assertThat(firmwareStatusInfoEvent.getFirmwareStatusInfo().getThingUID(), is(thingUID));
        assertThat(firmwareStatusInfoEvent.getFirmwareStatusInfo(), is(expectedInfo));
    }

    private void assertFailedFirmwareUpdate(ThingUID thingUID, Event event, String expectedErrorMessage) {
        assertThat(event, is(instanceOf(FirmwareUpdateResultInfoEvent.class)));
        FirmwareUpdateResultInfoEvent firmwareUpdateResultInfoEvent = (FirmwareUpdateResultInfoEvent) event;

        assertThat(firmwareUpdateResultInfoEvent.getTopic(), containsString(THING1_UID.getAsString()));
        assertThat(firmwareUpdateResultInfoEvent.getFirmwareUpdateResultInfo().getThingUID(), is(THING1_UID));
        assertThat(firmwareUpdateResultInfoEvent.getFirmwareUpdateResultInfo().getResult(),
                is(FirmwareUpdateResult.ERROR));
        assertThat(firmwareUpdateResultInfoEvent.getFirmwareUpdateResultInfo().getErrorMessage(),
                is(expectedErrorMessage));
    }

    private FirmwareUpdateHandler addHandler(Thing thing) {
        FirmwareUpdateHandler mockHandler = mock(FirmwareUpdateHandler.class);
        when(mockHandler.getThing()).thenReturn(thing);
        doReturn(true).when(mockHandler).isUpdateExecutable();
        doAnswer(invocation -> {
            Firmware firmware = (Firmware) invocation.getArguments()[0];
            thing.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, firmware.getVersion());
            return null;
        }).when(mockHandler).updateFirmware(any(Firmware.class), any(ProgressCallback.class));
        firmwareUpdateService.addFirmwareUpdateHandler(mockHandler);
        return mockHandler;
    }

    private void updateConfig(int period, int delay, TimeUnit timeUnit) throws IOException {
        Map<String, Object> properties = Map.ofEntries(entry(FirmwareUpdateServiceImpl.PERIOD_CONFIG_KEY, period),
                entry(FirmwareUpdateServiceImpl.DELAY_CONFIG_KEY, delay),
                entry(FirmwareUpdateServiceImpl.TIME_UNIT_CONFIG_KEY, timeUnit.name()));
        firmwareUpdateService.modified(properties);
    }
}
