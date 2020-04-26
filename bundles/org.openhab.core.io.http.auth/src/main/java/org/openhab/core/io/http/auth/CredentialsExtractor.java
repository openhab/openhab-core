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
package org.openhab.core.io.http.auth;

import java.util.Optional;

import org.openhab.core.auth.Credentials;

/**
 * Provider of credentials which works in given context and can provide credentials out of it.
 *
 * @author Łukasz Dywicki - Initial contribution.
 *
 * @param <C> Context type.
 */
public interface CredentialsExtractor<C> {

    Optional<Credentials> retrieveCredentials(C requestContext);
}
