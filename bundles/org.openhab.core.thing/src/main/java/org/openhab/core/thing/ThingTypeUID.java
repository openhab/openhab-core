/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.thing;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link ThingTypeUID} represents a unique identifier for thing types.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Jochen Hiller - Bugfix 455434: added default constructor
 */
@NonNullByDefault
public class ThingTypeUID extends UID {

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    ThingTypeUID() {
        super();
    }

    public ThingTypeUID(String uid) {
        super(uid);
    }

    public ThingTypeUID(String bindingId, String thingTypeId) {
        super(bindingId, thingTypeId);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 2;
    }

    /**
     * Returns the binding id.
     * 
     * @return binding id
     */
    public String getBindingId() {
        return getSegment(0);
    }

    /**
     * Returns the id.
     *
     * @return id the id
     */
    public String getId() {
        return getSegment(1);
    }
}
