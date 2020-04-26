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
package org.openhab.core.thing.type;

import org.openhab.core.thing.UID;

/**
 * The {@link ChannelTypeUID} represents a unique identifier for channel types.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Jochen Hiller - Bugfix 455434: added default constructor
 * @author Dennis Nobel - Javadoc added
 */
public class ChannelTypeUID extends UID {

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    ChannelTypeUID() {
        super();
    }

    /**
     * Creates a new instance of this class with the specified parameter.
     *
     * @param channelUid the UID for the channel
     */
    public ChannelTypeUID(String channelUid) {
        super(channelUid);
    }

    /**
     * Creates a new instance of this class with the specified parameter.
     *
     * @param bindingId the binding ID (must neither be null, nor empty)
     * @param id the identifier of the channel (must neither be null, nor empty)
     */
    public ChannelTypeUID(String bindingId, String id) {
        super(bindingId, id);
    }

    /**
     * Returns the identifier of the channel.
     *
     * @return the identifier of the channel (neither null, nor empty)
     */
    public String getId() {
        return getSegment(1);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 2;
    }
}
