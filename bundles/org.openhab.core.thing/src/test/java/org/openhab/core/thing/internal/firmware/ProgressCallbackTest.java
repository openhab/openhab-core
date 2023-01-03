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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareBuilder;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.openhab.core.thing.binding.firmware.ProgressCallback;
import org.openhab.core.thing.binding.firmware.ProgressStep;
import org.openhab.core.thing.firmware.FirmwareUpdateProgressInfoEvent;
import org.openhab.core.thing.firmware.FirmwareUpdateResult;
import org.openhab.core.thing.firmware.FirmwareUpdateResultInfoEvent;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;

/**
 * Testing the {@link ProgressCallback}.
 *
 * @author Christoph Knauf - Initial contribution
 * @author Dimitar Ivanov - Adapted the tests to use firmware instead of firmware UID
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public final class ProgressCallbackTest {

    private static final String BINDING_ID = "simpleBinding";
    private static final String THING_TYPE_ID1 = "simpleThingType1";
    private static final ThingTypeUID THING_TYPE_UID1 = new ThingTypeUID(BINDING_ID, THING_TYPE_ID1);
    private static final String THING1_ID = "simpleThing1";

    private static final String CANCEL_MESSAGE_KEY = "update-canceled";
    private static final ThingTypeUID THING_TYPE = new ThingTypeUID("thing:type");
    private static final ThingUID EXPECTED_THING_UID = new ThingUID(THING_TYPE, "thingid");

    private @NonNullByDefault({}) ProgressCallbackImpl sut;
    private @NonNullByDefault({}) Firmware expectedFirmware;
    private final List<Event> postedEvents = new LinkedList<>();
    private @Nullable String usedMessagedKey;

    @BeforeEach
    public void setUp() {
        expectedFirmware = FirmwareBuilder.create(THING_TYPE, "1").build();
        postedEvents.clear();

        EventPublisher publisher = new EventPublisher() {
            @Override
            public void post(Event event) throws IllegalArgumentException, IllegalStateException {
                postedEvents.add(event);
            }
        };

        TranslationProvider i18nProvider = new TranslationProvider() {
            @Override
            public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                    @Nullable Locale locale, @Nullable Object @Nullable... arguments) {
                usedMessagedKey = key;
                return "Dummy Message";
            }

            @Override
            public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                    @Nullable Locale locale) {
                usedMessagedKey = key;
                return "Dummy Message";
            }
        };

        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("");

        BundleResolver bundleResolver = mock(BundleResolver.class);
        when(bundleResolver.resolveBundle(any(Class.class))).thenReturn(bundle);

        sut = new ProgressCallbackImpl(new DummyFirmwareHandler(), publisher, i18nProvider, bundleResolver,
                EXPECTED_THING_UID, expectedFirmware, null);
    }

    @Test
    public void assertThatUpdateThrowsIllegalStateExceptionIfUpdateIsFinished() {
        sut.defineSequence(ProgressStep.DOWNLOADING);
        sut.next();
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(1), null, FirmwareUpdateResult.SUCCESS);
        assertThrows(IllegalStateException.class, () -> sut.update(100));
    }

    @Test
    public void assertThatDefineSequenceThrowsIllegalArguumentExceptionIfSequenceIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> sut.defineSequence());
    }

    @Test
    public void assertThatSuccessThrowsIllegalStateExceptionIfProgressIsNotAt100Percent() {
        sut.update(99);
        assertThrows(IllegalStateException.class, () -> sut.success());
    }

    @Test
    public void assertThatSuccessThrowsIllegalStateExceptionIfLastProgressStepIsNotReached() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.next();
        assertThrows(IllegalStateException.class, () -> sut.success());
    }

    @Test
    public void assertSuccessAt100PercentEvenIfThereIsARemainingProgressStep() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.next();
        sut.update(100);
        sut.success();
    }

    @Test
    public void assertThatUpdateThrowsIllegalArgumentExceptionIfProgressSmaller0() {
        assertThrows(IllegalArgumentException.class, () -> sut.update(-1));
    }

    @Test
    public void assertThatUpdateThrowsIllegalArgumentExceptionIfProgressGreater100() {
        assertThrows(IllegalArgumentException.class, () -> sut.update(101));
    }

    @Test
    public void assertThatUpdateThrowsIllegalArgumentExceptionIfNewProgressIsSmallerThanOldProgress() {
        sut.update(10);
        assertThrows(IllegalArgumentException.class, () -> sut.update(9));
    }

    @Test
    public void assertThatDefiningASequenceIsOptionalIfPercentageProgressIsUsed() {
        sut.update(5);
        assertThatProgressInfoEventIsValid(postedEvents.get(0), null, false, 5);
        sut.update(11);
        assertThatProgressInfoEventIsValid(postedEvents.get(1), null, false, 11);
        sut.update(22);
        assertThatProgressInfoEventIsValid(postedEvents.get(2), null, false, 22);
        sut.update(44);
        assertThatProgressInfoEventIsValid(postedEvents.get(3), null, false, 44);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(4), null, true, 44);
        sut.update(50);
        assertThatProgressInfoEventIsValid(postedEvents.get(5), null, false, 50);
        sut.update(70);
        assertThatProgressInfoEventIsValid(postedEvents.get(6), null, false, 70);
        sut.update(89);
        assertThatProgressInfoEventIsValid(postedEvents.get(7), null, false, 89);
        sut.update(100);
        assertThatProgressInfoEventIsValid(postedEvents.get(8), null, false, 100);
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(9), null, FirmwareUpdateResult.SUCCESS);
        assertThat(postedEvents.size(), is(10));
    }

    @Test
    public void assertThatUpdateResultsNotInFirmwareUpdateProgressInfoEventIfProgressNotChanged() {
        sut.defineSequence(ProgressStep.UPDATING);
        sut.update(10);
        sut.update(10);
        assertThatProgressInfoEventIsValid(postedEvents.get(0), ProgressStep.UPDATING, false, 10);
        assertThat(postedEvents.size(), is(1));
    }

    @Test
    public void assertThatSettingTheProgressToPendingResultsInAFirmwareUpdateProgressInfoEvent() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(0), ProgressStep.DOWNLOADING, true, null);
    }

    @Test
    public void assertThatPendingDoesNotChangeProgressStep() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(0), ProgressStep.DOWNLOADING, true, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(1), ProgressStep.DOWNLOADING, false, null);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(2), ProgressStep.DOWNLOADING, true, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(3), ProgressStep.DOWNLOADING, false, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(4), ProgressStep.TRANSFERRING, false, null);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(5), ProgressStep.TRANSFERRING, true, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(6), ProgressStep.TRANSFERRING, false, null);
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(7), null, FirmwareUpdateResult.SUCCESS);
        assertThat(postedEvents.size(), is(8));
    }

    @Test
    public void assertThatNextChangesProgressStepIfNotPending() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING, ProgressStep.UPDATING);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(0), ProgressStep.DOWNLOADING, false, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(1), ProgressStep.TRANSFERRING, false, null);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(2), ProgressStep.TRANSFERRING, true, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(3), ProgressStep.TRANSFERRING, false, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(4), ProgressStep.UPDATING, false, null);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(5), ProgressStep.UPDATING, true, null);
        sut.next();
        assertThatProgressInfoEventIsValid(postedEvents.get(6), ProgressStep.UPDATING, false, null);
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(7), null, FirmwareUpdateResult.SUCCESS);
        assertThat(postedEvents.size(), is(8));
    }

    @Test
    public void assertThatCancelThrowsIllegalStateExceptionIfUpdateIsFinished() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.next();
        sut.next();
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(2), null, FirmwareUpdateResult.SUCCESS);
        assertThrows(IllegalStateException.class, () -> sut.canceled());
    }

    @Test
    public void assertThatCallingCancelResultsInAFirmwareUpdateResultInfoEvent() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.canceled();
        assertThat(postedEvents.size(), is(1));
        assertThat(postedEvents.get(0), is(instanceOf(FirmwareUpdateResultInfoEvent.class)));
        FirmwareUpdateResultInfoEvent resultEvent = (FirmwareUpdateResultInfoEvent) postedEvents.get(0);
        assertThat(resultEvent.getFirmwareUpdateResultInfo().getThingUID(), is(EXPECTED_THING_UID));
        assertThat(resultEvent.getFirmwareUpdateResultInfo().getResult(), is(FirmwareUpdateResult.CANCELED));
        assertThat(usedMessagedKey, is(CANCEL_MESSAGE_KEY));
    }

    /*
     * Special behavior because of pending state:
     *
     * Before calling next the ProgressStep is null which means the update was not started
     * but a valid ProgressStep is needed to create a FirmwareUpdateProgressInfoEvent.
     * As workaround the first step is returned to provide a valid ProgressStep.
     * This could be the case if the update directly goes in PENDING state after trying to start it.
     */
    @Test
    public void assertThatGetProgressStepReturnsFirstStepIfNextWasNotCalledBefore() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        assertThat(sut.getCurrentStep(), is(ProgressStep.DOWNLOADING));
    }

    @Test
    public void assertThatGetProgressStepReturnsCurrentStepIfNextWasCalledBefore() {
        ProgressStep[] steps = new ProgressStep[] { ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING,
                ProgressStep.UPDATING, ProgressStep.REBOOTING };
        sut.defineSequence(steps);
        sut.next();
        for (int i = 0; i < steps.length - 1; i++) {
            assertThat(sut.getCurrentStep(), is(steps[i]));
            sut.next();
        }
    }

    @Test
    public void assertThatPendingThrowsIllegalArgumentExceptionIfStepSequenceIsNotDefinedAndNoProgressWasSet() {
        assertThrows(IllegalArgumentException.class, () -> sut.pending());
    }

    @Test
    public void assertThatPendingThrowsNoIllegalStateExceptionIfStepSequenceIsNotDefinedAndProgressWasSet() {
        sut.update(0);
        sut.pending();
        assertThatProgressInfoEventIsValid(postedEvents.get(1), null, true, 0);
    }

    @Test
    public void assertThatCancelThrowsNoIllegalStateExceptionIfStepSequenceIsNotDefined() {
        sut.canceled();
        assertThatUpdateResultEventIsValid(postedEvents.get(0), CANCEL_MESSAGE_KEY, FirmwareUpdateResult.CANCELED);
    }

    @Test
    public void assertThatFailedThrowsIllegalStateExceptionIfItsCalledMultipleTimes() {
        sut.failed("DummyMessageKey");
        assertThrows(IllegalStateException.class, () -> sut.failed("DummyMessageKey"));
    }

    @Test
    public void assertThatFailedThrowsIllegalStateExceptionForSuccessfulUpdates() {
        sut.update(100);
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(1), null, FirmwareUpdateResult.SUCCESS);
        assertThrows(IllegalStateException.class, () -> sut.failed("DummyMessageKey"));
    }

    @Test
    public void assertThatSuccessThrowsIllegalStateExceptionIfItsCalledMultipleTimes() {
        sut.update(100);
        sut.success();
        assertThatUpdateResultEventIsValid(postedEvents.get(1), null, FirmwareUpdateResult.SUCCESS);
        assertThrows(IllegalStateException.class, () -> sut.success());
    }

    @Test
    public void assertThatPendingThrowsIllegalStateExceptionIfUpdateFailed() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.failed("DummyMessageKey");
        assertThrows(IllegalStateException.class, () -> sut.pending());
    }

    @Test
    public void assertThatNextThrowsIllegalStateExceptionIfUpdateIsNotPendingAndNoFurtherStepsAvailable() {
        sut.defineSequence(ProgressStep.DOWNLOADING);
        sut.next();
        assertThrows(IllegalStateException.class, () -> sut.next());
    }

    @Test
    public void assertThatNextThrowsNoIllegalStateExceptionIfUpdateIsPendingAndNoFurtherStepsAvailable() {
        sut.defineSequence(ProgressStep.DOWNLOADING);
        sut.next();
        sut.pending();
        sut.next();
        assertThat(sut.getCurrentStep(), is(ProgressStep.DOWNLOADING));
    }

    @Test
    public void assertThatPendingThrowsIllegalStateExceptionIfUpdateWasSuccessful() {
        sut.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING);
        sut.next();
        sut.next();
        sut.success();
        assertThrows(IllegalStateException.class, () -> sut.pending());
    }

    private void assertThatProgressInfoEventIsValid(Event event, @Nullable ProgressStep expectedStep,
            boolean expectedPending, @Nullable Integer expectedProgress) {
        assertThat(event, is(instanceOf(FirmwareUpdateProgressInfoEvent.class)));
        FirmwareUpdateProgressInfoEvent fpiEvent = (FirmwareUpdateProgressInfoEvent) event;
        assertThat(fpiEvent.getProgressInfo().getThingUID(), is(EXPECTED_THING_UID));
        assertThat(fpiEvent.getProgressInfo().getFirmwareVersion(), is(expectedFirmware.getVersion()));
        assertThat(fpiEvent.getProgressInfo().getProgressStep(), is(expectedStep));
        assertThat(fpiEvent.getProgressInfo().getProgress(), is(expectedProgress));
        assertThat(fpiEvent.getProgressInfo().isPending(), (is(expectedPending)));
    }

    private void assertThatUpdateResultEventIsValid(Event event, @Nullable String expectedMessageKey,
            FirmwareUpdateResult expectedResult) {
        assertThat(event, is(instanceOf(FirmwareUpdateResultInfoEvent.class)));
        FirmwareUpdateResultInfoEvent fpiEvent = (FirmwareUpdateResultInfoEvent) event;
        assertThat(usedMessagedKey, is(expectedMessageKey));
        assertThat(fpiEvent.getFirmwareUpdateResultInfo().getThingUID(), is(EXPECTED_THING_UID));
        assertThat(fpiEvent.getFirmwareUpdateResultInfo().getResult(), is(expectedResult));
    }

    class DummyFirmwareHandler implements FirmwareUpdateHandler {

        @Override
        public Thing getThing() {
            return ThingBuilder.create(THING_TYPE_UID1, THING1_ID).build();
        }

        @Override
        public void updateFirmware(Firmware firmware, ProgressCallback progressCallback) {
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isUpdateExecutable() {
            return false;
        }
    }
}
