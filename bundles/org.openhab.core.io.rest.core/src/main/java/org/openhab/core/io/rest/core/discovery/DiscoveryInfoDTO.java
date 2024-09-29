/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data transfer object that is used to serialize the information about binding discovery.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class DiscoveryInfoDTO {

    public boolean inputSupported;
    public @Nullable String inputLabel;
    public @Nullable String inputDescription;

    public DiscoveryInfoDTO(boolean inputSupported, @Nullable String inputLabel, @Nullable String inputDescription) {
        this.inputSupported = inputSupported;
        this.inputLabel = inputLabel;
        this.inputDescription = inputDescription;
    }
}
