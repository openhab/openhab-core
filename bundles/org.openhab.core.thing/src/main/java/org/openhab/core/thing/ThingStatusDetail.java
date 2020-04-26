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
package org.openhab.core.thing;

/**
 * {@link ThingStatusDetail} defines possible status details of a {@link ThingStatusInfo}.
 *
 * @author Stefan Bußweiler - Initial contribution, added new status details
 * @author Chris Jackson - Added GONE status
 */
public enum ThingStatusDetail {
    NONE,
    HANDLER_MISSING_ERROR,
    HANDLER_REGISTERING_ERROR,
    HANDLER_INITIALIZING_ERROR,
    HANDLER_CONFIGURATION_PENDING,
    CONFIGURATION_PENDING,
    COMMUNICATION_ERROR,
    CONFIGURATION_ERROR,
    BRIDGE_OFFLINE,
    FIRMWARE_UPDATING,
    DUTY_CYCLE,
    BRIDGE_UNINITIALIZED,
    /**
     * Device has been removed. Used for example when the device has been removed from its bridge and
     * the thing handler should be removed.
     */
    GONE,
    DISABLED;

    public static final UninitializedStatus UNINITIALIZED = new UninitializedStatus();
    public static final NoneOnlyStatus INITIALIZING = new NoneOnlyStatus();
    public static final NoneOnlyStatus UNKNOWN = new NoneOnlyStatus();
    public static final OnlineStatus ONLINE = new OnlineStatus();
    public static final OfflineStatus OFFLINE = new OfflineStatus();
    public static final NoneOnlyStatus REMOVING = new NoneOnlyStatus();
    public static final NoneOnlyStatus REMOVED = new NoneOnlyStatus();

    public static final class NoneOnlyStatus {
        private NoneOnlyStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
    }

    public static final class UninitializedStatus {
        private UninitializedStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
        public ThingStatusDetail HANDLER_MISSING_ERROR = ThingStatusDetail.HANDLER_MISSING_ERROR;
        public ThingStatusDetail HANDLER_REGISTERING_ERROR = ThingStatusDetail.HANDLER_REGISTERING_ERROR;
        public ThingStatusDetail HANDLER_CONFIGURATION_PENDING = ThingStatusDetail.HANDLER_CONFIGURATION_PENDING;
        public ThingStatusDetail HANDLER_INITIALIZING_ERROR = ThingStatusDetail.HANDLER_INITIALIZING_ERROR;
        public ThingStatusDetail BRIDGE_UNINITIALIZED = ThingStatusDetail.BRIDGE_UNINITIALIZED;
    };

    public static final class OnlineStatus {
        private OnlineStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
        public ThingStatusDetail CONFIGURATION_PENDING = ThingStatusDetail.CONFIGURATION_PENDING;
    };

    public static final class OfflineStatus {
        private OfflineStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
        public ThingStatusDetail COMMUNICATION_ERROR = ThingStatusDetail.COMMUNICATION_ERROR;
        public ThingStatusDetail CONFIGURATION_ERROR = ThingStatusDetail.CONFIGURATION_ERROR;
        public ThingStatusDetail BRIDGE_OFFLINE = ThingStatusDetail.BRIDGE_OFFLINE;
        public ThingStatusDetail FIRMWARE_UPDATING = ThingStatusDetail.FIRMWARE_UPDATING;
        public ThingStatusDetail DUTY_CYCLE = ThingStatusDetail.DUTY_CYCLE;
        public ThingStatusDetail GONE = ThingStatusDetail.GONE;
    };
}
