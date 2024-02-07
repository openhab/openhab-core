/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.link;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link YamlLinkDTO} is a data transfer object used to serialize an item
 * in a YAML configuration file.
 *
 * @author Jan N. Klug - Initial contribution
 */
@YamlElementName("links")
public class YamlLinkDTO extends ItemChannelLinkDTO implements YamlElement {
    private final Logger logger = LoggerFactory.getLogger(YamlLinkDTO.class);

    @Override
    public @NonNull String getId() {
        return channelUID + "|" + itemName;
    }

    @Override
    public boolean isValid() {
        if (channelUID == null) {
            logger.debug("channelUID missing");
            return false;
        }
        if (itemName == null) {
            logger.debug("itemName is missing for {}", channelUID);
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        YamlLinkDTO that = (YamlLinkDTO) o;
        return Objects.equals(channelUID, that.channelUID) && Objects.equals(itemName, that.itemName)
                && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelUID, itemName, configuration);
    }

    @Override
    public @NonNull String toString() {
        return "YamlLinkDTO{channelUID='" + channelUID + "', configuration=" + configuration + ", itemName='" + itemName
                + "'}";
    }
}
