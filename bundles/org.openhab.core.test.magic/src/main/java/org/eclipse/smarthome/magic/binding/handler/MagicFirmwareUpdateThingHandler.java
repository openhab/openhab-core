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
package org.eclipse.smarthome.magic.binding.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.eclipse.smarthome.core.thing.binding.firmware.ProgressCallback;
import org.eclipse.smarthome.core.thing.binding.firmware.ProgressStep;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.magic.binding.MagicBindingConstants;

/**
 * Handler for firmware updatable magic things. Defines full progress sequence and simulates firmware update with small
 * delays between the steps.
 *
 * @author Dimitar Ivanov - Initial contribution
 *
 */
@NonNullByDefault
public class MagicFirmwareUpdateThingHandler extends BaseThingHandler implements FirmwareUpdateHandler {

    private static final int STEP_DELAY = 100;

    public MagicFirmwareUpdateThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Nothing to do
    }

    @Override
    public void initialize() {
        String updateModel = (String) getThing().getConfiguration().get(MagicBindingConstants.UPDATE_MODEL_PROPERTY);
        switch (updateModel) {
            case MagicBindingConstants.MODEL_ALOHOMORA:
            case MagicBindingConstants.MODEL_COLLOPORTUS:
            case MagicBindingConstants.MODEL_LUMOS:
            case MagicBindingConstants.MODEL_NOX:
                getThing().setProperty(Thing.PROPERTY_MODEL_ID, updateModel);
        }

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void updateFirmware(Firmware firmware, ProgressCallback progressCallback) {
        progressCallback.defineSequence(ProgressStep.DOWNLOADING, ProgressStep.TRANSFERRING, ProgressStep.UPDATING,
                ProgressStep.REBOOTING, ProgressStep.WAITING);

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.FIRMWARE_UPDATING, "Firmware is updating");

        progressCallback.next();
        for (int percent = 1; percent < 100; percent++) {
            try {
                Thread.sleep(STEP_DELAY);
            } catch (InterruptedException e) {
                progressCallback.failed("Magic firmware update progress callback interrupted while sleeping", e);
            }
            progressCallback.update(percent);
            if (percent % 20 == 0) {
                progressCallback.next();
            }
        }

        getThing().setProperty(Thing.PROPERTY_FIRMWARE_VERSION, firmware.getVersion());

        progressCallback.success();

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void cancel() {
        // not needed for now
    }

    @Override
    public boolean isUpdateExecutable() {
        return true;
    }
}
