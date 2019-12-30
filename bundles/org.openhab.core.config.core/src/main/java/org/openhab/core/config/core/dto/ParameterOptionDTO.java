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
package org.openhab.core.config.core.dto;

/**
 * This is a data transfer object that is used to serialize options of a
 * parameter.
 *
 * @author Alex Tugarev - Initial contribution
 */
public class ParameterOptionDTO {

    public String label;
    public String value;

    public ParameterOptionDTO() {
    }

    public ParameterOptionDTO(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
