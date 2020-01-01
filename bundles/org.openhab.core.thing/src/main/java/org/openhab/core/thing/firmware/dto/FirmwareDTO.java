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
package org.openhab.core.thing.firmware.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data transfer object that is used to serialize firmware information.
 *
 * @author Aoun Bukhari - Initial contribution
 * @author Dimitar Ivanov - enriched with thing type UID
 */
@NonNullByDefault
public class FirmwareDTO {
    public final String thingTypeUID;
    public final @Nullable String vendor;
    public final @Nullable String model;
    public final boolean modelRestricted;
    public final @Nullable String description;
    public final String version;
    public final @Nullable String changelog;
    public final @Nullable String prerequisiteVersion;

    public FirmwareDTO(String thingTypeUID, @Nullable String vendor, @Nullable String model, boolean modelRestricted,
            @Nullable String description, String version, @Nullable String prerequisiteVersion,
            @Nullable String changelog) {
        this.thingTypeUID = thingTypeUID;
        this.vendor = vendor;
        this.model = model;
        this.modelRestricted = modelRestricted;
        this.description = description;
        this.version = version;
        this.prerequisiteVersion = prerequisiteVersion;
        this.changelog = changelog;
    }
}
