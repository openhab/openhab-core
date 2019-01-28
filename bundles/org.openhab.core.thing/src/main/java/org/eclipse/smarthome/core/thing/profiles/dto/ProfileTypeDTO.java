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
package org.eclipse.smarthome.core.thing.profiles.dto;

import java.util.Collection;

/**
 * DTO for profile types
 *
 * @author Stefan Triller - initial contribution
 *
 */
public class ProfileTypeDTO {

    public String uid;
    public String label;
    public String kind;
    public Collection<String> supportedItemTypes;

    public ProfileTypeDTO() {
    }

    public ProfileTypeDTO(String uid, String label, String kind, Collection<String> supportedItemTypes) {
        this.uid = uid;
        this.label = label;
        this.kind = kind;
        this.supportedItemTypes = supportedItemTypes;
    }

}
