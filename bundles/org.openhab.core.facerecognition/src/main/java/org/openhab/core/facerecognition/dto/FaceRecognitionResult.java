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
package org.openhab.core.facerecognition.dto;

import org.eclipse.smarthome.core.library.types.RawType;

/**
 * FaceRecognitionResult carries the various result elements
 * of a face recognition process
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class FaceRecognitionResult {
    public boolean recognized = false;
    public String labelInfo;
    public double score;
    public RawType image;
}
