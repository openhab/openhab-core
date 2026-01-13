/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.rest.transform;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.transform.Transformation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@link TransformationDTO} wraps a {@link Transformation}
 *
 * @author Jan N. Klug - Initial contribution
 */
@Schema(name = "Transformation")
@NonNullByDefault
public class TransformationDTO {
    public String uid;
    public String label;
    public String type;
    public Map<String, String> configuration;
    public boolean editable = false;

    public TransformationDTO(Transformation transformation) {
        this.uid = transformation.getUID();
        this.label = transformation.getLabel();
        this.type = transformation.getType();
        this.configuration = transformation.getConfiguration();
    }
}
