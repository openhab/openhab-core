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
package org.eclipse.smarthome.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;

/**
 * A {@link BridgeHandler} handles the communication between the Eclipse SmartHome framework and
 * a <i>bridge</i> (a device that acts as a gateway to enable the communication with other devices)
 * represented by a {@link Bridge} instance.
 * <p>
 * A {@link BridgeHandler} is a {@link ThingHandler} as well.
 * <p>
 *
 * @author Stefan Bußweiler - Initial contribution and API
 */
@NonNullByDefault
public interface BridgeHandler extends ThingHandler {

    /**
     * Informs the bridge handler that a child handler has been initialized.
     *
     * @param childHandler the initialized child handler
     * @param childThing the thing of the initialized child handler
     */
    void childHandlerInitialized(ThingHandler childHandler, Thing childThing);

    /**
     * Informs the bridge handler that a child handler has been disposed.
     *
     * @param childHandler the disposed child handler
     * @param childThing the thing of the disposed child handler
     */
    void childHandlerDisposed(ThingHandler childHandler, Thing childThing);

}
