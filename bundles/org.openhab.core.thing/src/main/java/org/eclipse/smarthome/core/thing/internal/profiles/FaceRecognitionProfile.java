/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.facerecognition.FaceRecognitionManager;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.core.facerecognition.dto.FaceRecognitionResult;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the given parameter "offset" to a QuantityType or DecimalType state
 *
 * @author Stefan Triller - initial contribution
 *
 */
@NonNullByDefault
public class FaceRecognitionProfile implements StateProfile {
    private final Logger logger = LoggerFactory.getLogger(FaceRecognitionProfile.class);
    private final ProfileCallback callback;
    private @NonNullByDefault({}) FaceRecognitionManager faceRecognitionManager;

    public FaceRecognitionProfile(ProfileCallback callback) {
        this.callback = callback;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.FACE_RECOGNITION;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        logger.debug("Received state update from Item, sending timestamp to callback");
        callback.handleUpdate(recognize(state));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        logger.debug("Received state update from Handler, sending timestamp to callback");
        callback.sendUpdate(recognize(state));
    }

    private State recognize(Type state) {
        List<FaceRecognitionResult> identifications = null;
        if (state instanceof RawType) {
            identifications = faceRecognitionManager.recognize(((RawType) state).getBytes());
            if (!identifications.isEmpty()) {
                return new StringType(identifications.get(0).labelInfo);
            }
        } else {
            logger.warn(
                    "Face recognition cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                    state);
        }

        return UnDefType.UNDEF;
    }

    @Reference
    protected void setFaceRecognitionManager(FaceRecognitionManager faceRecognitionManager) {
        this.faceRecognitionManager = faceRecognitionManager;
    }

    protected void unsetFaceRecognitionManager(FaceRecognitionManager faceRecognitionManager) {
        this.faceRecognitionManager = null;
    }

    @Override
    public void onCommandFromItem(Command command) {
        // no-op
    }

    @Override
    public void onCommandFromHandler(Command command) {
        // no-op
    }

}
