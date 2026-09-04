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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Realizations of this type are responsible for checking validity of various credentials and giving back authentication
 * which defines access scope for authenticated user or system.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
public interface AuthenticationProvider {

    /**
     * Verify given credentials and give back authentication if they are valid.
     * <p>
     * <b>Note:</b> {@link Credentials#dispose()} is called on the specified {@link Credentials} after use, making
     * them suitable for one-time use only. The purpose is to clear sensitive data from memory. If not desirable, use
     * {@link #authenticate(Credentials, boolean)} instead.
     *
     * @param credentials the user credentials.
     * @return {@code null} if credentials were not valid for this provider
     * @throws AuthenticationException if authentication failed due to credentials mismatch.
     */
    default Authentication authenticate(Credentials credentials) throws AuthenticationException {
        return authenticate(credentials, true);
    }

    /**
     * Verify given credentials and give back authentication if they are valid.
     *
     * @param credentials the user credentials.
     * @param dispose if {@code true}, {@link Credentials#dispose()} is called after authentication, to clear
     *            sensitive data from memory.
     * @return {@code null} if credentials were not valid for this provider
     * @throws AuthenticationException if authentication failed due to credentials mismatch.
     */
    Authentication authenticate(Credentials credentials, boolean dispose) throws AuthenticationException;

    /**
     * Additional method to verify if given authentication provider can handle given type of credentials.
     *
     * @param type Type of credentials.
     * @return True if credentials of given type can be used for authentication attempt with provider.
     */
    boolean supports(Class<? extends Credentials> type);
}
