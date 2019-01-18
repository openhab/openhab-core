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
package org.eclipse.smarthome.core.auth;

/**
 * Realizations of this type are responsible for checking validity of various credentials and giving back authentication
 * which defines access scope for authenticated user or system.
 *
 * @author Łukasz Dywicki - Initial contribution and API
 *
 */
public interface AuthenticationProvider {

    /**
     * Verify given credentials and give back authentication if they are valid.
     *
     * @param credentials User credentials.
     * @return null if credentials were not valid for this provider
     * @throws AuthenticationException if authentication failed due to credentials mismatch.
     */
    Authentication authenticate(Credentials credentials) throws AuthenticationException;

    /**
     * Additional method to verify if given authentication provider can handle given type of credentials.
     *
     * @param type Type of credentials.
     * @return True if credentials of given type can be used for authentication attempt with provider.
     */
    boolean supports(Class<? extends Credentials> type);

}
