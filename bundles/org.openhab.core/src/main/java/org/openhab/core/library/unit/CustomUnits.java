/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.library.unit;

import org.eclipse.jdt.annotation.NonNullByDefault;

import tech.units.indriya.AbstractSystemOfUnits;

/**
 * Base class for all custom unit classes added in openHAB.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
class CustomUnits extends AbstractSystemOfUnits {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
