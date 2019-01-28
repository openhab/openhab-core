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
package org.eclipse.smarthome.auth.oauth2client.internal;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Enum of types being used in the store
 *
 * @author Gary Tse - Initial Contribution
 *
 */
public enum StorageRecordType {

    LAST_USED(".LastUsed"),
    ACCESS_TOKEN_RESPONSE(".AccessTokenResponse"),
    SERVICE_CONFIGURATION(".ServiceConfiguration");

    private String suffix;

    private StorageRecordType(String suffix) {
        this.suffix = suffix;
    }

    public @NonNull String getKey(String handle) {
        return (handle == null) ? this.suffix : (handle + this.suffix);
    }

}
