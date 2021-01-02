/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.EventDescription;

/**
 * Interface for builders for {@link ChannelType}s of kind TRIGGER
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public interface TriggerChannelTypeBuilder extends ChannelTypeBuilder<TriggerChannelTypeBuilder> {

    /**
     * Sets the EventDescription for the ChannelType
     *
     * @param eventDescription EventDescription for the ChannelType
     * @return this Builder
     */
    TriggerChannelTypeBuilder withEventDescription(EventDescription eventDescription);
}
