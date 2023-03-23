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
package org.openhab.core.thing.xml.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.thing.type.ChannelType;

/**
 * The {@link ChannelTypeXmlResult} is an intermediate XML conversion result object which
 * contains a {@link ChannelType} object.
 * <p>
 * If a {@link ConfigDescription} object exists, it must be added to the according {@link ConfigDescriptionProvider}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Ivan Iliev - Added support for system wide channel types
 */
@NonNullByDefault
public class ChannelTypeXmlResult {

    private ChannelType channelType;
    private ConfigDescription configDescription;
    private boolean system;

    public ChannelTypeXmlResult(ChannelType channelType, ConfigDescription configDescription) {
        this(channelType, configDescription, false);
    }

    public ChannelTypeXmlResult(ChannelType channelType, ConfigDescription configDescription, boolean system) {
        this.channelType = channelType;
        this.configDescription = configDescription;
        this.system = system;
    }

    public ChannelType toChannelType() {
        return channelType;
    }

    public ConfigDescription getConfigDescription() {
        return configDescription;
    }

    public boolean isSystem() {
        return system;
    }

    @Override
    public String toString() {
        return "ChannelTypeXmlResult [channelType=" + channelType + ", configDescription=" + configDescription + "]";
    }
}
