/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.openhab.core.thing.binding.firmware.ProgressCallback;
import org.openhab.core.thing.binding.firmware.ProgressStep;
import org.openhab.core.thing.firmware.FirmwareEventFactory;
import org.openhab.core.thing.firmware.FirmwareUpdateProgressInfo;
import org.openhab.core.thing.firmware.FirmwareUpdateResult;
import org.openhab.core.thing.firmware.FirmwareUpdateResultInfo;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;

/**
 * The callback implementation for the {@link ProgressCallback}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Christoph Knauf - Introduced pending, canceled, update and InternalState
 * @author Dimitar Ivanov - Callback contains firmware domain object
 */
final class ProgressCallbackImpl implements ProgressCallback {

    private static final String UPDATE_CANCELED_MESSAGE_KEY = "update-canceled";

    /**
     * Handler instance is needed to retrieve the error messages from the correct bundle.
     */
    private final FirmwareUpdateHandler firmwareUpdateHandler;
    private final EventPublisher eventPublisher;
    private final TranslationProvider i18nProvider;
    private final BundleResolver bundleResolver;
    private final ThingUID thingUID;
    private final Firmware firmware;
    private final Locale locale;

    private Collection<ProgressStep> sequence;
    private Iterator<ProgressStep> progressIterator;
    private ProgressStep current;
    private Integer progress;

    private enum InternalState {
        FINISHED,
        PENDING,
        RUNNING,
        INITIALIZED
    };

    private InternalState state;

    ProgressCallbackImpl(FirmwareUpdateHandler firmwareUpdateHandler, EventPublisher eventPublisher,
            TranslationProvider i18nProvider, BundleResolver bundleResolver, ThingUID thingUID, Firmware firmware,
            Locale locale) {
        this.firmwareUpdateHandler = firmwareUpdateHandler;
        ParameterChecks.checkNotNull(eventPublisher, "Event publisher");
        this.eventPublisher = eventPublisher;
        ParameterChecks.checkNotNull(i18nProvider, "i18n provider");
        this.i18nProvider = i18nProvider;
        this.bundleResolver = bundleResolver;
        this.thingUID = thingUID;
        ParameterChecks.checkNotNull(firmware, "Firmware");
        this.firmware = firmware;
        this.locale = locale;
        this.progress = null;
    }

    @Override
    public void defineSequence(ProgressStep... sequence) {
        if (sequence == null || sequence.length == 0) {
            throw new IllegalArgumentException("Sequence must not be null or empty.");
        }
        this.sequence = Collections.unmodifiableCollection(Arrays.asList(sequence));
        progressIterator = this.sequence.iterator();
        this.state = InternalState.INITIALIZED;
    }

    @Override
    public void next() {
        if (this.state == InternalState.FINISHED) {
            throw new IllegalStateException("Update is finished.");
        }
        if (this.state == InternalState.PENDING) {
            state = InternalState.RUNNING;
            postProgressInfoEvent();
        } else if (progressIterator.hasNext()) {
            state = InternalState.RUNNING;
            this.current = progressIterator.next();
            postProgressInfoEvent();
        } else {
            state = InternalState.FINISHED;
            throw new IllegalStateException("There is no further progress step to be executed.");
        }
    }

    @Override
    public void failed(String errorMessageKey, Object... arguments) {
        if (this.state == InternalState.FINISHED) {
            throw new IllegalStateException("Update is finished.");
        }
        if (errorMessageKey == null || errorMessageKey.isEmpty()) {
            throw new IllegalArgumentException("The error message key must not be null or empty.");
        }
        this.state = InternalState.FINISHED;
        String errorMessage = getMessage(firmwareUpdateHandler.getClass(), errorMessageKey, arguments);
        postResultInfoEvent(FirmwareUpdateResult.ERROR, errorMessage);
    }

    @Override
    public void success() {
        if (this.state == InternalState.FINISHED) {
            throw new IllegalStateException("Update is finished.");
        }
        if ((this.progress == null || this.progress < 100)
                && (this.progressIterator == null || progressIterator.hasNext())) {
            throw new IllegalStateException(
                    "Update can't be successfully finished until progress is 100% or last progress step is reached");
        }
        this.state = InternalState.FINISHED;
        postResultInfoEvent(FirmwareUpdateResult.SUCCESS, null);
    }

    @Override
    public void pending() {
        if (this.state == InternalState.FINISHED) {
            throw new IllegalStateException("Update is finished.");
        }
        this.state = InternalState.PENDING;
        postProgressInfoEvent();
    }

    @Override
    public void canceled() {
        if (this.state == InternalState.FINISHED) {
            throw new IllegalStateException("Update is finished.");
        }
        this.state = InternalState.FINISHED;
        String cancelMessage = getMessage(this.getClass(), UPDATE_CANCELED_MESSAGE_KEY);
        postResultInfoEvent(FirmwareUpdateResult.CANCELED, cancelMessage);
    }

    @Override
    public void update(int progress) {
        if (this.state == InternalState.FINISHED) {
            throw new IllegalStateException("Update is finished.");
        }
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("The progress must be between 0 and 100.");
        }
        if (this.progress == null) {
            updateProgress(progress);
        } else if (progress < this.progress) {
            throw new IllegalArgumentException("The new progress must not be smaller than the old progress.");
        } else if (this.progress != progress) {
            updateProgress(progress);
        }
    }

    private void updateProgress(int progress) {
        this.progress = progress;
        this.state = InternalState.RUNNING;
        postProgressInfoEvent();
    }

    void failedInternal(String errorMessageKey) {
        this.state = InternalState.FINISHED;
        String errorMessage = getMessage(ProgressCallbackImpl.class, errorMessageKey, new Object[] {});
        postResultInfoEvent(FirmwareUpdateResult.ERROR, errorMessage);
    }

    private String getMessage(Class<?> clazz, String errorMessageKey, Object... arguments) {
        Bundle bundle = bundleResolver.resolveBundle(clazz);
        String errorMessage = i18nProvider.getText(bundle, errorMessageKey, null, locale, arguments);
        return errorMessage;
    }

    private void postResultInfoEvent(FirmwareUpdateResult result, String message) {
        post(FirmwareEventFactory.createFirmwareUpdateResultInfoEvent(
                FirmwareUpdateResultInfo.createFirmwareUpdateResultInfo(thingUID, result, message)));
    }

    private void postProgressInfoEvent() {
        if (this.progress == null) {
            post(FirmwareEventFactory.createFirmwareUpdateProgressInfoEvent(
                    FirmwareUpdateProgressInfo.createFirmwareUpdateProgressInfo(thingUID, firmware.getThingTypeUID(),
                            firmware.getVersion(), getCurrentStep(), sequence, this.state == InternalState.PENDING)));
        } else {
            post(FirmwareEventFactory.createFirmwareUpdateProgressInfoEvent(
                    FirmwareUpdateProgressInfo.createFirmwareUpdateProgressInfo(thingUID, firmware.getVersion(),
                            getCurrentStep(), sequence, this.state == InternalState.PENDING, progress)));
        }
    }

    private void post(Event event) {
        eventPublisher.post(event);
    }

    ProgressStep getCurrentStep() {
        if (current != null) {
            return current;
        }
        if (sequence != null && progressIterator.hasNext()) {
            this.current = progressIterator.next();
            return current;
        }
        return null;
    }
}
