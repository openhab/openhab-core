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
package org.openhab.core.io.rest.auth.internal;

import java.util.Date;

/**
 * A DTO representing a user API token, without the sensible information.
 *
 * @author Yannick Schaus - initial contribution
 */
public class UserApiTokenDTO {
    String name;
    Date createdTime;
    String scope;

    public UserApiTokenDTO(String name, Date createdTime, String scope) {
        super();
        this.name = name;
        this.createdTime = createdTime;
        this.scope = scope;
    }
}
