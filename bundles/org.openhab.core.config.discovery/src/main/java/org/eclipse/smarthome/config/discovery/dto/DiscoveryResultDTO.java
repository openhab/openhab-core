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
package org.eclipse.smarthome.config.discovery.dto;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;

/**
 * This is a data transfer object that is used to serialize discovery results.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Thomas Höfer - Added representation
 */
@NonNullByDefault
public class DiscoveryResultDTO {

    public @Nullable String bridgeUID;
    public @Nullable DiscoveryResultFlag flag;
    public @Nullable String label;
    public @Nullable Map<String, Object> properties;
    public @Nullable String representationProperty;
    public @Nullable String thingUID;
    public @Nullable String thingTypeUID;

    public DiscoveryResultDTO() {
    }

    public DiscoveryResultDTO(String thingUID, @Nullable String bridgeUID, @Nullable String thingTypeUID, String label,
            DiscoveryResultFlag flag, Map<String, Object> properties, @Nullable String representationProperty) {
        this.thingUID = thingUID;
        this.thingTypeUID = thingTypeUID;
        this.bridgeUID = bridgeUID;
        this.label = label;
        this.flag = flag;
        this.properties = properties;
        this.representationProperty = representationProperty;
    }

}
