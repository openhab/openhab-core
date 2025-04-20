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
package org.openhab.core.model.item.internal.fileconverter;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.fileconverter.AbstractItemFileGenerator;
import org.openhab.core.items.fileconverter.ItemFileGenerator;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.items.ItemModel;
import org.openhab.core.model.items.ItemsFactory;
import org.openhab.core.model.items.ModelBinding;
import org.openhab.core.model.items.ModelGroupFunction;
import org.openhab.core.model.items.ModelGroupItem;
import org.openhab.core.model.items.ModelItem;
import org.openhab.core.model.items.ModelProperty;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DslItemFileConverter} is the DSL file converter for {@link Item} object
 * with the capabilities of parsing and generating file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = ItemFileGenerator.class)
public class DslItemFileConverter extends AbstractItemFileGenerator {

    private final Logger logger = LoggerFactory.getLogger(DslItemFileConverter.class);

    private final ModelRepository modelRepository;
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    @Activate
    public DslItemFileConverter(final @Reference ModelRepository modelRepository,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry) {
        this.modelRepository = modelRepository;
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    @Override
    public String getFileFormatGenerator() {
        return "DSL";
    }

    @Override
    public synchronized void generateFileFormat(OutputStream out, List<Item> items, Collection<Metadata> metadata,
            boolean hideDefaultParameters) {
        ItemModel model = ItemsFactory.eINSTANCE.createItemModel();
        for (Item item : items) {
            model.getItems().add(buildModelItem(item, getChannelLinks(metadata, item.getName()),
                    getMetadata(metadata, item.getName()), hideDefaultParameters));
        }
        modelRepository.generateSyntaxFromModel(out, "items", model);
    }

    private ModelItem buildModelItem(Item item, List<Metadata> channelLinks, List<Metadata> metadata,
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

        for (Metadata md : channelLinks) {
            String namespace = md.getUID().getNamespace();
            ModelBinding binding = ItemsFactory.eINSTANCE.createModelBinding();
            binding.setType(namespace);
            binding.setConfiguration(md.getValue());
            for (ConfigParameter param : getConfigurationParameters(md, hideDefaultParameters)) {
                ModelProperty property = buildModelProperty(param.name(), param.value());
                if (property != null) {
                    binding.getProperties().add(property);
                } else {
                    logger.warn(
                            "Item \"{}\": configuration parameter \"{}\" for channel link \"{}\" is ignored because its value type is not supported!",
                            item.getName(), param.name(), md.getValue());
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
                } else {
                    logger.warn(
                            "Item \"{}\": configuration parameter \"{}\" for metadata namespace \"{}\" is ignored because its value type is not supported!",
                            item.getName(), param.name(), namespace);
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
                // Avoid injecting the state pattern in label if already present in stateDescription metadata
                if (patternInjected && statePattern != null) {
                    model.setLabel(label);
                }
            }
        }

        return model;
    }

    private @Nullable ModelProperty buildModelProperty(String key, Object value) {
        ModelProperty property = ItemsFactory.eINSTANCE.createModelProperty();
        property.setKey(key);
        if (value instanceof List<?> list) {
            if (!list.isEmpty()) {
                for (Object val : list) {
                    if (val instanceof String || val instanceof BigDecimal || val instanceof Boolean) {
                        property.getValue().add(val);
                    } else if (val instanceof Double doubleValue) {
                        property.getValue().add(BigDecimal.valueOf(doubleValue));
                    } else {
                        property = null;
                        break;
                    }
                }
            } else {
                property = null;
            }
        } else if (value instanceof String || value instanceof BigDecimal || value instanceof Boolean) {
            property.getValue().add(value);
        } else if (value instanceof Double doubleValue) {
            // It was discovered that configuration parameter value of an item metadata can be of type Double
            // when the metadata is added through Main UI.
            // A conversion to a BigDecimal is then required to avoid an exception later when generating DSL
            property.getValue().add(BigDecimal.valueOf(doubleValue));
        } else {
            property = null;
        }
        return property;
    }

    /*
     * Get the list of configuration parameters for a channel link.
     *
     * If a profile is set and a configuration description is found for this profile, the parameters are provided
     * in the same order as in this configuration description, and any parameter having the default value is ignored.
     * If no profile is set, the parameters are provided sorted by natural order of their names.
     */
    private List<ConfigParameter> getConfigurationParameters(Metadata metadata, boolean hideDefaultParameters) {
        List<ConfigParameter> parameters = new ArrayList<>();
        Set<String> handledNames = new HashSet<>();
        Map<String, Object> configParameters = metadata.getConfiguration();
        Object profile = configParameters.get("profile");
        List<ConfigDescriptionParameter> configDescriptionParameter = List.of();
        if (profile instanceof String profileStr) {
            parameters.add(new ConfigParameter("profile", profileStr));
            handledNames.add("profile");
            try {
                ConfigDescription configDesc = configDescriptionRegistry
                        .getConfigDescription(new URI("profile:" + profileStr));
                if (configDesc != null) {
                    configDescriptionParameter = configDesc.getParameters();
                }
            } catch (URISyntaxException e) {
                // Ignored; in practice this will never be thrown
            }
        }
        for (ConfigDescriptionParameter param : configDescriptionParameter) {
            String paramName = param.getName();
            if (handledNames.contains(paramName)) {
                continue;
            }
            Object value = configParameters.get(paramName);
            Object defaultValue = ConfigUtil.getDefaultValueAsCorrectType(param);
            if (value != null && (!hideDefaultParameters || !value.equals(defaultValue))) {
                parameters.add(new ConfigParameter(paramName, value));
            }
            handledNames.add(paramName);
        }
        for (String paramName : configParameters.keySet().stream().sorted().collect(Collectors.toList())) {
            if (handledNames.contains(paramName)) {
                continue;
            }
            Object value = configParameters.get(paramName);
            if (value != null) {
                parameters.add(new ConfigParameter(paramName, value));
            }
            handledNames.add(paramName);
        }
        return parameters;
    }
}
