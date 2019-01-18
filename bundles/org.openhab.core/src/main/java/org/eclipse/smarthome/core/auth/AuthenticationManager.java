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
 * Authentication manager is main entry point for all places which are interested in securing requests and verifying
 * their originator.
 *
 * @author Łukasz Dywicki - Initial contribution and API
 */
public interface AuthenticationManager {

    /**
     * Authentication attempt with specified credentials.
     *
     * @param credentials Credentials to authenticate with.
     * @return Null value should never be returned. Any failed authentication (for whatever reason), should cause
     *         AuthenticationException.
     * @throws AuthenticationException - when none of available authentication methods succeeded.
     */
    Authentication authenticate(Credentials credentials) throws AuthenticationException;

}
