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
 * This is a data transfer object that is used to serialize firmware status information.
 *
 * @author Aoun Bukhari - Initial contribution
 */
@NonNullByDefault
public class FirmwareStatusDTO {
    public final String status;
    public final @Nullable String updatableVersion;

    public FirmwareStatusDTO(String status, @Nullable String updatableVersion) {
        this.status = status;
        this.updatableVersion = updatableVersion;
    }
}
