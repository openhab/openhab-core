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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * The {@link FirmwareUpdateResultInfo} contains information about the result of a firmware update.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Consolidated all the needed information for firmware status events
 */
@NonNullByDefault
public final class FirmwareUpdateResultInfo {

    private final FirmwareUpdateResult result;

    private @Nullable String errorMessage;

    private final ThingUID thingUID;

    private FirmwareUpdateResultInfo(ThingUID thingUID, FirmwareUpdateResult result, @Nullable String errorMessage) {
        Objects.requireNonNull(thingUID, "The thingUID must not be null.");
        this.thingUID = thingUID;

        Objects.requireNonNull(result, "Firmware update result must not be null");
        this.result = result;

        if (result != FirmwareUpdateResult.SUCCESS) {
            if (errorMessage == null || errorMessage.isEmpty()) {
                throw new IllegalArgumentException(
                        "Error message must not be null or empty for erroneous firmare updates");
            }
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Creates a new {@link FirmwareUpdateResultInfo}.
     *
     * @param thingUID thingUID of the thing being updated
     * @param result the result of the firmware update (must not be null)
     * @param errorMessage the error message in case of result is {@link FirmwareUpdateResult#ERROR} (must not be null
     *            or empty for erroneous firmware updates; ignored for successful firmware updates)
     * @return FirmwareUpdateResultInfo (not null)
     * @throws IllegalArgumentException if error message is null or empty for erroneous firmware updates
     */
    public static FirmwareUpdateResultInfo createFirmwareUpdateResultInfo(ThingUID thingUID,
            FirmwareUpdateResult result, String errorMessage) {
        return new FirmwareUpdateResultInfo(thingUID, result, errorMessage);
    }

    /**
     * Returns the result of the firmware update.
     *
     * @return the result of the firmware update
     */
    public FirmwareUpdateResult getResult() {
        return result;
    }

    /**
     * Returns the thing UID.
     *
     * @return the thing UID
     */
    public ThingUID getThingUID() {
        return thingUID;
    }

    /**
     * Returns the error message in case of result is {@link FirmwareUpdateResult#ERROR}.
     *
     * @return the error message in case of erroneous firmware updates (is null for successful firmware updates)
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
        result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        FirmwareUpdateResultInfo other = (FirmwareUpdateResultInfo) obj;
        if (errorMessage == null) {
            if (other.errorMessage != null) {
                return false;
            }
        } else if (!errorMessage.equals(other.errorMessage)) {
            return false;
        }
        if (result != other.result) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FirmwareUpdateResultInfo [result=" + result + ", errorMessage=" + errorMessage + "]";
    }

}
