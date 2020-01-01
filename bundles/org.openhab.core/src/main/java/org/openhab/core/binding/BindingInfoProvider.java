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
package org.openhab.core.binding;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link BindingInfoProvider} is a service interface providing {@link BindingInfo} objects. All registered
 * {@link BindingInfoProvider} services are tracked by the {@link BindingInfoRegistry} and provided as one common
 * collection.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @see BindingInfoRegistry
 */
@NonNullByDefault
public interface BindingInfoProvider {

    /**
     * Returns the binding information for the specified binding ID and locale (language),
     * or {@code null} if no binding information could be found.
     *
     * @param id the ID to be looked for (could be null or empty)
     * @param locale the locale to be used for the binding information (could be null)
     * @return a localized binding information object (could be null)
     */
    @Nullable
    BindingInfo getBindingInfo(@Nullable String id, @Nullable Locale locale);

    /**
     * Returns all binding information in the specified locale (language) this provider contains.
     *
     * @param locale the locale to be used for the binding information (could be null)
     * @return a localized set of all binding information this provider contains
     *         (could be empty)
     */
    Set<BindingInfo> getBindingInfos(@Nullable Locale locale);
}
