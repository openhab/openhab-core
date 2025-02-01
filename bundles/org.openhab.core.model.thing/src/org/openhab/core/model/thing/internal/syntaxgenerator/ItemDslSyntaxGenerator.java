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
package org.openhab.core.model.thing.internal.syntaxgenerator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.items.ItemModel;
import org.openhab.core.model.items.ItemsFactory;
import org.openhab.core.model.items.ModelBinding;
import org.openhab.core.model.items.ModelGroupFunction;
import org.openhab.core.model.items.ModelGroupItem;
import org.openhab.core.model.items.ModelItem;
import org.openhab.core.model.items.ModelProperty;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.syntaxgenerator.AbstractItemSyntaxGenerator;
import org.openhab.core.thing.syntaxgenerator.ItemSyntaxGenerator;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ItemDslSyntaxGenerator} is the DSL syntax generator for {@link Item} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = ItemSyntaxGenerator.class)
public class ItemDslSyntaxGenerator extends AbstractItemSyntaxGenerator {

    private final Logger logger = LoggerFactory.getLogger(ItemDslSyntaxGenerator.class);

    private final ModelRepository modelRepository;

    @Activate
    public ItemDslSyntaxGenerator(final @Reference ModelRepository modelRepository,
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        super(configDescRegistry);
        this.modelRepository = modelRepository;
    }

    @Override
    public String getFormat() {
        return "DSL";
    }

    @Override
    public synchronized String generateSyntax(List<Item> items, Collection<ItemChannelLink> channelLinks,
            Collection<Metadata> metadata, boolean hideDefaultParameters) {
        ItemModel model = ItemsFactory.eINSTANCE.createItemModel();
        for (Item item : items) {
            model.getItems().add(buildModelItem(item, getChannelLinks(channelLinks, item.getName()),
                    getMetadata(metadata, item.getName()), hideDefaultParameters));
        }
        String syntax = modelRepository.generateSyntaxFromModelContent("items", model);
        logger.debug("Generated syntax:\n{}", syntax);
        return syntax;
    }

    private ModelItem buildModelItem(Item item, List<ItemChannelLink> channelLinks, List<Metadata> metadata,
            boolean hideDefaultParameters) {
        ModelItem model;
        if (item instanceof GroupItem groupItem) {
            ModelGroupItem modelGroup = ItemsFactory.eINSTANCE.createModelGroupItem();
            model = modelGroup;
            Item baseItem = groupItem.getBaseItem();
            if (baseItem != null) {
                modelGroup.setType(baseItem.getType());
                GroupFunction function = groupItem.getFunction();
                if (function != null) {
                    ModelGroupFunction modelFunction = ModelGroupFunction
                            .getByName(function.getClass().getSimpleName().toUpperCase());
                    modelGroup.setFunction(modelFunction);
                    State[] parameters = function.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        modelGroup.getArgs().add(parameters[i].toString());
                    }
                }
            }
        } else {
            model = ItemsFactory.eINSTANCE.createModelNormalItem();
            model.setType(item.getType());
        }

        model.setName(item.getName());
        String label = item.getLabel();
        boolean patternInjected = false;
        String defaultPattern = getDefaultStatePattern(item);
        if (label != null && !label.isEmpty()) {
            StateDescription stateDescr = item.getStateDescription();
            String statePattern = stateDescr == null ? null : stateDescr.getPattern();
            String patterToInject = statePattern != null && !statePattern.equals(defaultPattern) ? statePattern : null;
            if (patterToInject != null) {
                // Inject the pattern in the label
                patternInjected = true;
                model.setLabel("%s [%s]".formatted(label, patterToInject));
            } else {
                model.setLabel(label);
            }
        }

        String category = item.getCategory();
        if (category != null && !category.isEmpty()) {
            model.setIcon(category);
        }
        for (String group : item.getGroupNames()) {
            model.getGroups().add(group);
        }
        for (String tag : item.getTags().stream().sorted().collect(Collectors.toList())) {
            model.getTags().add(tag);
        }

        for (ItemChannelLink channelLink : channelLinks) {
            ModelBinding binding = ItemsFactory.eINSTANCE.createModelBinding();
            binding.setType("channel");
            binding.setConfiguration(channelLink.getLinkedUID().getAsString());
            for (ConfigParameter param : getConfigurationParameters(channelLink, hideDefaultParameters)) {
                ModelProperty property = buildModelProperty(param.name(), param.value());
                if (property != null) {
                    binding.getProperties().add(property);
                }
            }
            model.getBindings().add(binding);
        }

        for (Metadata md : metadata) {
            String namespace = md.getUID().getNamespace();
            ModelBinding binding = ItemsFactory.eINSTANCE.createModelBinding();
            binding.setType(namespace);
            binding.setConfiguration(md.getValue());
            String statePattern = null;
            for (ConfigParameter param : getConfigurationParameters(md)) {
                ModelProperty property = buildModelProperty(param.name(), param.value());
                if (property != null) {
                    binding.getProperties().add(property);
                }
                if ("stateDescription".equals(namespace) && "pattern".equals(param.name())) {
                    statePattern = param.value().toString();
                }
            }
            // Ignore state description in case it contains only a state pattern and state pattern was injected
            // in the item label or is the default pattern
            if (!(statePattern != null && binding.getProperties().size() == 1
                    && (patternInjected || statePattern.equals(defaultPattern)))) {
                model.getBindings().add(binding);
            }
        }

        return model;
    }

    private @Nullable ModelProperty buildModelProperty(String key, Object value) {
        ModelProperty property = ItemsFactory.eINSTANCE.createModelProperty();
        property.setKey(key);
        if (value instanceof List<?> list) {
            if (!list.isEmpty()) {
                property.getValue().addAll(list);
            } else {
                property = null;
            }
        } else {
            property.getValue().add(value);
        }
        return property;
    }
}
