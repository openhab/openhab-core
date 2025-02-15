/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * Interface for a service that provides access to a {@link ThingHandler}.
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
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
    @Nullable
    ThingHandler getThingHandler();

    /**
     * This method is used by the framework during activation of the OSGi component.
     * It is called BEFORE the thing handler is set.
     *
     * See {@link #initialize()}, {@link #deactivate()}
     */
    default void activate() {
    }

    /**
     * This method is used by the framework during de-activation of the OSGi component.
     * It is NOT guaranteed that the thing handler is still valid.
     *
     * See {@link #dispose()}, {@link #activate()}
     */
    default void deactivate() {
    }

    /**
     * This method is used by the framework during activation of the service.
     * It is called AFTER the component is fully activated and thing handler has been set.
     *
     * Implementations should override this method to add additional initialization code. This method should call
     * <code>super.initialize()</code> to ensure background discovery is properly handled.
     *
     * See {@link #activate()}, {@link #dispose()}
     */
    default void initialize() {
    }

    /**
     * This method is used by the framework during de-activation of the service.
     * It is called while the component is still activated.
     *
     * Code depending on an activated service should go here. This method should call <code>super.dispose()</code> to
     * ensure background discovery is properly handled.
     *
     * See {@link #deactivate()}, {@link #initialize()}
     */
    default void dispose() {
    }
}
