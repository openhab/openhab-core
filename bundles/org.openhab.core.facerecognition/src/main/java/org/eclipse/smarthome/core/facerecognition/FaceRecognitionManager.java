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
package org.eclipse.smarthome.core.facerecognition;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.RawType;
import org.openhab.core.facerecognition.dto.FaceRecognitionResult;

/**
 * This service provides functionality to detect a set of trained faces in an image
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public interface FaceRecognitionManager {

    public List<FaceRecognitionResult> recognize(byte[] imageData);

    public void train(String name, RawType picture);

}
