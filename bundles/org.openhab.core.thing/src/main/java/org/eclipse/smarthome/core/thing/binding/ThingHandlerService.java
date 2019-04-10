/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.binding;

/**
 * The {@link ThingHandler#getServices()} function allows you to return a list of classes,
 * implementing this interface, that will be started as OSGi services. 
 * <p>
 * Use this for example to start thing specific discovery services (extending {@link AbstractDiscoveryService}).
 * Example:
 * <pre>
 * @NonNullByDefault
 * public class MyThingDiscovery extends AbstractDiscoveryService implements ThingHandlerService {
 *     private @Nullable ThingHandler thingHandler; // A reference to your thing handler
 *
 *     public void activate() {
 *         super(Collections.emptyMap()); // start AbstractDiscoveryService
 *     }
 *     public void setThingHandler(ThingHandler handler) { this.thingHandler = handler; }
 *     public ThingHandler getThingHandler() { return this.thingHandler; }
 * }
 * </pre>
 *
 * @author Stefan Triller - initial contribution
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
