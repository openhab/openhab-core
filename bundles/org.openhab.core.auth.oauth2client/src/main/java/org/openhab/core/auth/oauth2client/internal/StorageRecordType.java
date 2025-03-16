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
package org.openhab.core.auth.oauth2client.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Enum of types being used in the store
 *
 * @author Gary Tse - Initial contribution
 * @author Andrew Fiddian-Green - added RFC-8628 support
 */
@NonNullByDefault
public enum StorageRecordType {

    LAST_USED(".LastUsed"),
    ACCESS_TOKEN_RESPONSE(".AccessTokenResponse"),
    SERVICE_CONFIGURATION(".ServiceConfiguration"),
    DEVICE_CODE_RESPONSE(".DeviceCodeResponse");

    private String suffix;

    StorageRecordType(String suffix) {
        this.suffix = suffix;
    }

    public String getKey(@Nullable String handle) {
        return (handle == null) ? this.suffix : (handle + this.suffix);
    }
}
