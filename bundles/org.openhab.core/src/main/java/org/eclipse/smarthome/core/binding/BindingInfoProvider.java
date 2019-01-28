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
package org.eclipse.smarthome.core.binding;

import java.util.Locale;
import java.util.Set;

/**
 * The {@link BindingInfoProvider} is a service interface providing {@link BindingInfo} objects. All registered
 * {@link BindingInfoProvider} services are tracked by the {@link BindingInfoRegistry} and provided as one common
 * collection.
 *
 * @author Michael Grammling - Initial Contribution
 *
 * @see BindingInfoRegistry
 */
public interface BindingInfoProvider {

    /**
     * Returns the binding information for the specified binding ID and locale (language),
     * or {@code null} if no binding information could be found.
     *
     * @param id the ID to be looked for (could be null or empty)
     * @param locale the locale to be used for the binding information (could be null)
     * @return a localized binding information object (could be null)
     */
    BindingInfo getBindingInfo(String id, Locale locale);

    /**
     * Returns all binding information in the specified locale (language) this provider contains.
     *
     * @param locale the locale to be used for the binding information (could be null)
     * @return a localized set of all binding information this provider contains
     *         (not null, could be empty)
     */
    Set<BindingInfo> getBindingInfos(Locale locale);

}
