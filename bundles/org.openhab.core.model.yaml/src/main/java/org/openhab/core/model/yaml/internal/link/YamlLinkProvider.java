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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.AbstractYamlProvider;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link YamlLinkProvider} is an {@link ItemChannelLinkProvider} for item channel links in YAML files
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemChannelLinkProvider.class, YamlLinkProvider.class,
        YamlModelListener.class })
public class YamlLinkProvider extends AbstractYamlProvider<ItemChannelLink, String, YamlLinkDTO>
        implements ItemChannelLinkProvider, YamlModelListener<YamlLinkDTO> {

    @Activate
    public YamlLinkProvider() {
        super(YamlLinkDTO.class);
    }

    @Override
    protected @Nullable ItemChannelLink map(YamlLinkDTO yamlElement) {
        return new ItemChannelLink(yamlElement.itemName, new ChannelUID(yamlElement.channelUID),
                new Configuration(yamlElement.configuration));
    }
}
