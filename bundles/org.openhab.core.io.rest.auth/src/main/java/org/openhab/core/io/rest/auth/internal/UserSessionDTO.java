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
 * A DTO representing a user session, without the sensible information.
 *
 * @author Yannick Schaus - initial contribution
 */
public class UserSessionDTO {
    String sessionId;
    Date createdTime;
    Date lastRefreshTime;
    String clientId;
    String scope;

    public UserSessionDTO(String sessionId, Date createdTime, Date lastRefreshTime, String clientId, String scope) {
        super();
        this.sessionId = sessionId;
        this.createdTime = createdTime;
        this.lastRefreshTime = lastRefreshTime;
        this.clientId = clientId;
        this.scope = scope;
    }
}
