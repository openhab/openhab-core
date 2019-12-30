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
package org.openhab.core.thing.util;

import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandler;

/**
 * This class provides utility methods related to the {@link ThingHandler} class.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Simon Kaufmann - added UNKNOWN
 */
public class ThingHandlerHelper {

    private ThingHandlerHelper() {
    }

    /**
     * Checks if the given state indicates that a thing handler has been initialized.
     *
     * @return true if the thing handler has been initialized, otherwise false.
     */
    public static boolean isHandlerInitialized(final ThingStatus thingStatus) {
        return thingStatus == ThingStatus.OFFLINE || thingStatus == ThingStatus.ONLINE
                || thingStatus == ThingStatus.UNKNOWN;
    }

    /**
     * Checks if the thing handler has been initialized.
     *
     * @return true if the thing handler has been initialized, otherwise false.
     */
    public static boolean isHandlerInitialized(final Thing thing) {
        return isHandlerInitialized(thing.getStatus());
    }

    /**
     * Checks if the thing handler has been initialized.
     *
     * @return true if the thing handler has been initialized, otherwise false.
     */
    public static boolean isHandlerInitialized(final ThingHandler handler) {
        return isHandlerInitialized(handler.getThing());
    }
}
