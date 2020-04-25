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
package org.openhab.core.i18n;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.PointType;

/**
 * This interface describes a provider for a location.
 *
 * @author Stefan Triller - Initial contribution
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
