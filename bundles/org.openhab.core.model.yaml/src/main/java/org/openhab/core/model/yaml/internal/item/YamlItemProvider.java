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
package org.openhab.core.model.yaml.internal.item;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.AbstractYamlProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link YamlItemProvider} is an {@link ItemProvider} for items in YAML files
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemProvider.class, YamlItemProvider.class, YamlModelListener.class })
public class YamlItemProvider extends AbstractYamlProvider<Item, String, YamlItemDTO>
        implements ItemProvider, YamlModelListener<YamlItemDTO> {
    private final ItemBuilderFactory itemBuilderFactory;

    @Activate
    public YamlItemProvider(@Reference ItemBuilderFactory itemBuilderFactory) {
        super(YamlItemDTO.class);
        this.itemBuilderFactory = itemBuilderFactory;
    }

    @Override
    protected @Nullable Item map(YamlItemDTO yamlElement) {
        Item item = ItemDTOMapper.map(yamlElement, itemBuilderFactory);
        if (item == null) {
            logger.warn("Failed to create an item for {}, discarding it", yamlElement);
        }
        return item;
    }
}
