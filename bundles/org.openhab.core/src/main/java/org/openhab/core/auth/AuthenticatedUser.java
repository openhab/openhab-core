/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.auth;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link User} that is authenticated by the system itself, i.e., session state and API tokens are managed by the
 * system.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface AuthenticatedUser extends User {
    /**
     * Gets the pending token information for this user, if any.
     *
     * @return the pending token information or null if there is none
     */
    @Nullable
    PendingToken getPendingToken();

    /**
     * Sets or clears the pending token information for this user.
     *
     * @param pendingToken the pending token information or null to clear it
     */
    void setPendingToken(@Nullable PendingToken pendingToken);

    /**
     * Gets the current persistent sessions for this user.
     *
     * @return the list of sessions
     */
    List<UserSession> getSessions();

    /**
     * Replaces the list of sessions by a new one.
     *
     * @param sessions the new list of sessions
     */
    void setSessions(List<UserSession> sessions);

    /**
     * Gets the long-term API tokens for this user
     *
     * @return the API tokens
     */
    List<UserApiToken> getApiTokens();

    /**
     * Replaces the list of API tokens by a new one.
     *
     * @param apiTokens the new API tokens
     */
    void setApiTokens(List<UserApiToken> apiTokens);
}
