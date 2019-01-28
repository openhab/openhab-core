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

/**
 *
 * Interface for a service that provides access to a {@link ThingHandler}
 *
 * @author Stefan Triller - initial contribution
 *
 */
public interface ThingHandlerService {

    /**
     * Sets the ThingHandler on which the actions (methods) should be called
     *
     * @param handler the {@link ThingHandler}
     */
    void setThingHandler(ThingHandler handler);

    /**
     * Gets the ThingHandler on which the actions (methods) should be called
     *
     * @return the {@link ThingHandler}
     */
    ThingHandler getThingHandler();

    /**
     * Method that will be called if this service will be activated
     */
    default void activate() {
    }

    /**
     * Method that will be called if this service will be deactivated
     */
    default void deactivate() {
    }

}
