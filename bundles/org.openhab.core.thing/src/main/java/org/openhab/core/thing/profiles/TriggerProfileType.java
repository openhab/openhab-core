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
package org.openhab.core.thing.profiles;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * Describes a {@link TriggerProfile} type.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface TriggerProfileType extends ProfileType {

    /**
     *
     * @return a collection of ChannelTypeUIDs (may be empty if all are supported).
     */
    Collection<ChannelTypeUID> getSupportedChannelTypeUIDs();
}
