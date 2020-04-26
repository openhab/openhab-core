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
package org.openhab.core.thing.firmware;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link FirmwareStatusInfo} represents the {@link FirmwareStatus} of a {@link Thing}. If the firmware status is
 * {@link FirmwareStatus#UPDATE_EXECUTABLE} then the information object will also provide the thing UID and the
 * version of the latest updatable firmware for the thing.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Consolidated all the needed information for firmware status events
 */
@NonNullByDefault
public final class FirmwareStatusInfo {

    private final ThingUID thingUID;

    private final FirmwareStatus firmwareStatus;

    private final @Nullable String firmwareVersion;

    /**
     * Package protected default constructor to allow reflective instantiation.
     *
     * !!! DO NOT REMOVE - Gson needs it !!!
     */
    FirmwareStatusInfo() {
        thingUID = new ThingUID("internal:reflective:constructor");
        firmwareStatus = FirmwareStatus.UNKNOWN;
        firmwareVersion = null;
    }

    private FirmwareStatusInfo(ThingUID thingUID, FirmwareStatus firmwareStatus, @Nullable String firmwareVersion) {
        Objects.requireNonNull(thingUID, "Thing UID must not be null.");
        this.thingUID = thingUID;

        Objects.requireNonNull(firmwareStatus, "Firmware status must not be null.");
        this.firmwareStatus = firmwareStatus;

        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Creates a new {@link FirmwareStatusInfo} having {@link FirmwareStatus#UNKNOWN) as firmware status.
     *
     * @return the firmware status info (not null)
     */
    public static FirmwareStatusInfo createUnknownInfo(ThingUID thingUID) {
        return new FirmwareStatusInfo(thingUID, FirmwareStatus.UNKNOWN, null);
    }

    /**
     * Creates a new {@link FirmwareStatusInfo} having {@link FirmwareStatus#UP_TO_DATE) as firmware status.
     *
     * @return the firmware status info (not null)
     */
    public static FirmwareStatusInfo createUpToDateInfo(ThingUID thingUID) {
        return new FirmwareStatusInfo(thingUID, FirmwareStatus.UP_TO_DATE, null);
    }

    /**
     * Creates a new {@link FirmwareStatusInfo} having {@link FirmwareStatus#UPDATE_AVAILABLE) as firmware status.
     *
     * @return the firmware status info (not null)
     */
    public static FirmwareStatusInfo createUpdateAvailableInfo(ThingUID thingUID) {
        return new FirmwareStatusInfo(thingUID, FirmwareStatus.UPDATE_AVAILABLE, null);
    }

    /**
     * Creates a new {@link FirmwareStatusInfo} having {@link FirmwareStatus#UPDATE_EXECUTBALE) as firmware status. The
     * given firmware version represents the version of the latest updatable firmware for the thing.
     *
     * @param firmwareVersion the version of the latest updatable firmware for the thing (must not be null)
     * @return the firmware status info (not null)
     */
    public static FirmwareStatusInfo createUpdateExecutableInfo(ThingUID thingUID, @Nullable String firmwareVersion) {
        return new FirmwareStatusInfo(thingUID, FirmwareStatus.UPDATE_EXECUTABLE, firmwareVersion);
    }

    /**
     * Returns the firmware status.
     *
     * @return the firmware status (not null)
     */
    public FirmwareStatus getFirmwareStatus() {
        return firmwareStatus;
    }

    /**
     * Returns the firmware version of the latest updatable firmware for the thing.
     *
     * @return the firmware version (only set if firmware status is {@link FirmwareStatus#UPDATE_EXECUTABLE})
     */
    public @Nullable String getUpdatableFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Returns the thing UID.
     *
     * @return the thing UID of the thing, whose status is updated (not null)
     */
    public ThingUID getThingUID() {
        return thingUID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((thingUID == null) ? 0 : thingUID.hashCode());
        result = prime * result + ((firmwareStatus == null) ? 0 : firmwareStatus.hashCode());
        result = prime * result + ((firmwareVersion == null) ? 0 : firmwareVersion.hashCode());
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
        FirmwareStatusInfo other = (FirmwareStatusInfo) obj;
        if (firmwareStatus != other.firmwareStatus) {
            return false;
        }
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
        return true;
    }

    @Override
    public String toString() {
        return "FirmwareStatusInfo [firmwareStatus=" + firmwareStatus + ", firmwareVersion=" + firmwareVersion
                + ", thingUID=" + thingUID + "]";
    }
}
