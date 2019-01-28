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
package org.eclipse.smarthome.config.core;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Can be implemented to point one config description URI to another.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@NonNullByDefault
public interface ConfigDescriptionAliasProvider {

    /**
     * Get an alias for the given config description URI (if applicable)
     *
     * @param configDescriptionURI the original config description URI
     * @return the alias or {@code null} if this handler does not apply to the given URI.
     */
    @Nullable
    URI getAlias(URI configDescriptionURI);

}
