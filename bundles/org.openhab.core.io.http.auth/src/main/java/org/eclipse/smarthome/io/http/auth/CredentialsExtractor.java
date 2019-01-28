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
package org.eclipse.smarthome.io.http.auth;

import java.util.Optional;

import org.eclipse.smarthome.core.auth.Credentials;

/**
 * Provider of credentials which works in given context and can provide credentials out of it.
 *
 * @author Łukasz Dywicki - inital contribution.
 *
 * @param <C> Context type.
 */
public interface CredentialsExtractor<C> {

    Optional<Credentials> retrieveCredentials(C requestContext);

}
