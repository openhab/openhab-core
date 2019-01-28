/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.firmware;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.firmware.ProgressStep;

/**
 * The {@link FirmwareUpdateProgressInfo} represents the progress indicator for a firmware update.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Christoph Knauf - Added progress and pending
 * @author Dimitar Ivanov - Consolidated all the needed information for firmware status events
 */
@NonNullByDefault
public final class FirmwareUpdateProgressInfo {

    private final ThingUID thingUID;

    private final String firmwareVersion;

    private final ProgressStep progressStep;

    private final Collection<ProgressStep> sequence;

    private final boolean pending;

    private final @Nullable Integer progress;

    private FirmwareUpdateProgressInfo(ThingUID thingUID, String firmwareVersion, ProgressStep progressStep,
            Collection<ProgressStep> sequence, boolean pending, int progress) {
        Objects.requireNonNull(thingUID, "ThingUID must not be null.");
        Objects.requireNonNull(firmwareVersion, "Firmware version must not be null.");

        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("The progress must be between 0 and 100.");
        }

        this.thingUID = thingUID;
        this.firmwareVersion = firmwareVersion;
        this.progressStep = progressStep;
        this.sequence = sequence;
        this.pending = pending;
        this.progress = progress;
    }

    /**
     * Creates a new {@link FirmwareUpdateProgressInfo}.
     *
     * @param thingUID the thing UID of the thing that is updated (must not be null)
     * @param firmwareVersion the version of the firmware that is updated (must not be null)
     * @param progressStep the current progress step (must not be null)
     * @param sequence the collection of progress steps describing the sequence of the firmware update process
     *            (must not be null)
     * @param pending the flag indicating if the update is pending
     * @param progress the progress of the update in percent
     * @return FirmwareUpdateProgressInfo object (not null)
     * @throws IllegalArgumentException if sequence is null or empty or progress is not between 0 and 100
     */
    public static FirmwareUpdateProgressInfo createFirmwareUpdateProgressInfo(ThingUID thingUID, String firmwareVersion,
            ProgressStep progressStep, Collection<ProgressStep> sequence, boolean pending, int progress) {
        return new FirmwareUpdateProgressInfo(thingUID, firmwareVersion, progressStep, sequence, pending, progress);
    }

    private FirmwareUpdateProgressInfo(ThingUID thingUID, String firmwareVersion, ProgressStep progressStep,
            Collection<ProgressStep> sequence, boolean pending) {
        Objects.requireNonNull(thingUID, "ThingUID must not be null.");
        Objects.requireNonNull(firmwareVersion, "Firmware version must not be null.");

        if (sequence == null || sequence.isEmpty()) {
            throw new IllegalArgumentException("Sequence must not be null or empty.");
        }
        Objects.requireNonNull(progressStep, "Progress step must not be null.");

        this.thingUID = thingUID;
        this.firmwareVersion = firmwareVersion;
        this.progressStep = progressStep;
        this.sequence = sequence;
        this.pending = pending;
        this.progress = null;
    }

    /**
     * Creates a new {@link FirmwareUpdateProgressInfo}.
     *
     * @param thingUID the thing UID of the thing that is updated (must not be null)
     * @param firmwareVersion the version of the firmware that is updated (must not be null)
     * @param progressStep the current progress step (must not be null)
     * @param sequence the collection of progress steps describing the sequence of the firmware update process
     *            (must not be null)
     * @param pending the flag indicating if the update is pending
     * @return FirmwareUpdateProgressInfo object (not null)
     * @throws IllegalArgumentException if sequence is null or empty
     */
    @NonNull
    public static FirmwareUpdateProgressInfo createFirmwareUpdateProgressInfo(ThingUID thingUID,
            ThingTypeUID thingTypeUID, String firmwareVersion, ProgressStep progressStep,
            Collection<ProgressStep> sequence, boolean pending) {
        return new FirmwareUpdateProgressInfo(thingUID, firmwareVersion, progressStep, sequence, pending);
    }

    /**
     * Returns the firmware version of the firmware that is updated.
     *
     * @return the firmware version of the firmware that is updated (not null)
     */
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Returns the current progress step.
     *
     * @return the current progress step (not null)
     */
    public ProgressStep getProgressStep() {
        return progressStep;
    }

    /**
     * Returns the sequence of the firmware update process.
     *
     * @return the sequence (not null)
     */
    public Collection<ProgressStep> getSequence() {
        return sequence;
    }

    /**
     * Returns true if the firmware update is pending, false otherwise
     *
     * @return true if pending, false otherwise
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * Returns the percentage progress of the firmware update.
     *
     * @return the progress between 0 and 100 or null if no progress was set
     */
    @Nullable
    public Integer getProgress() {
        return progress;
    }

    /**
     * Returns the thing UID.
     *
     * @return the thing UID
     */
    public ThingUID getThingUID() {
        return thingUID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((thingUID == null) ? 0 : thingUID.hashCode());
        result = prime * result + ((firmwareVersion == null) ? 0 : firmwareVersion.hashCode());
        result = prime * result + (pending ? 1231 : 1237);
        result = prime * result + progress;
        result = prime * result + ((progressStep == null) ? 0 : progressStep.hashCode());
        result = prime * result + ((sequence == null) ? 0 : sequence.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FirmwareUpdateProgressInfo)) {
            return false;
        }
        FirmwareUpdateProgressInfo other = (FirmwareUpdateProgressInfo) obj;
        if (thingUID == null) {
            if (other.thingUID != null) {
                return false;
            }
        } else if (!thingUID.equals(other.thingUID)) {
            return false;
        }
        if (firmwareVersion == null) {
            if (other.firmwareVersion != null) {
                return false;
            }
        } else if (!firmwareVersion.equals(other.firmwareVersion)) {
            return false;
        }
        if (pending != other.pending) {
            return false;
        }
        if (!progress.equals(other.progress)) {
            return false;
        }
        if (progressStep != other.progressStep) {
            return false;
        }
        if (sequence == null) {
            if (other.sequence != null) {
                return false;
            }
        } else if (!sequence.equals(other.sequence)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FirmwareUpdateProgressInfo [thingUID=" + thingUID + ", firmwareVersion=" + firmwareVersion
                + ", progressStep=" + progressStep + ", sequence=" + sequence + ", pending=" + pending + ", progress="
                + progress + "]";
    }

}
