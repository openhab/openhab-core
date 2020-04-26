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
package org.openhab.core.config.core;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Can be implemented to point one config description URI to another.
 *
 * @author Simon Kaufmann - Initial contribution
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
