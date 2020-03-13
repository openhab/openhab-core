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
package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Provider;

/**
 * A interface for a {@link Provider} of {@link User} entities
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
public interface UserProvider extends Provider<User> {

}
