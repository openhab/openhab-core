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
package org.eclipse.smarthome.core.i18n;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.PointType;

/**
 * This interface describes a provider for a location.
 *
 * @author Stefan Triller - Initial contribution and API
 */
@NonNullByDefault
public interface LocationProvider {

    /**
     * Provides access to the location of the installation
     *
     * @return location of the current installation or null if the location is not set
     */
    @Nullable
    PointType getLocation();

}
