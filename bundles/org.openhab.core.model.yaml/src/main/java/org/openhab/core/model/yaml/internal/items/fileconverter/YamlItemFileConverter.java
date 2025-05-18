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
package org.openhab.core.model.yaml.internal.items.fileconverter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.openhab.core.items.GroupFunction.Equality;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.fileconverter.AbstractItemFileGenerator;
import org.openhab.core.items.fileconverter.ItemFileGenerator;
import org.openhab.core.items.fileconverter.ItemFileParser;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.items.YamlChannelLinkProvider;
import org.openhab.core.model.yaml.internal.items.YamlGroupDTO;
import org.openhab.core.model.yaml.internal.items.YamlItemDTO;
import org.openhab.core.model.yaml.internal.items.YamlItemProvider;
import org.openhab.core.model.yaml.internal.items.YamlMetadataDTO;
import org.openhab.core.model.yaml.internal.items.YamlMetadataProvider;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlItemFileConverter} is the YAML file converter for {@link Item} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemFileGenerator.class, ItemFileParser.class })
public class YamlItemFileConverter extends AbstractItemFileGenerator implements ItemFileParser {

    private final YamlModelRepository modelRepository;
    private final YamlItemProvider itemProvider;
    private final YamlMetadataProvider metadataProvider;
    private final YamlChannelLinkProvider channelLinkProvider;
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    @Activate
    public YamlItemFileConverter(final @Reference YamlModelRepository modelRepository,
            final @Reference YamlItemProvider itemProvider, final @Reference YamlMetadataProvider metadataProvider,
            final @Reference YamlChannelLinkProvider channelLinkProvider,
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        super();
        this.modelRepository = modelRepository;
        this.itemProvider = itemProvider;
        this.metadataProvider = metadataProvider;
        this.channelLinkProvider = channelLinkProvider;
        this.configDescriptionRegistry = configDescRegistry;
    }

    @Override
    public String getFileFormatGenerator() {
        return "YAML";
    }

    @Override
    public void setItemsToBeGenerated(String id, List<Item> items, Collection<Metadata> metadata,
            boolean hideDefaultParameters) {
        List<YamlElement> elements = new ArrayList<>();
        items.forEach(item -> {
            elements.add(buildItemDTO(item, getChannelLinks(metadata, item.getName()),
                    getMetadata(metadata, item.getName()), hideDefaultParameters));
        });
        modelRepository.addElementsToBeGenerated(id, elements);
    }

    @Override
    public void generateFileFormat(String id, OutputStream out) {
        modelRepository.generateFileFormat(id, out);
    }

    private YamlItemDTO buildItemDTO(Item item, List<Metadata> channelLinks, List<Metadata> metadata,
            boolean hideDefaultParameters) {
        YamlItemDTO dto = new YamlItemDTO();
        dto.name = item.getName();

        String label = item.getLabel();
        boolean patternSet = false;
        String defaultPattern = getDefaultStatePattern(item);
        if (label != null && !label.isEmpty()) {
            dto.label = item.getLabel();
            StateDescription stateDescr = item.getStateDescription();
            String statePattern = stateDescr == null ? null : stateDescr.getPattern();
            String patterToSet = statePattern != null && !statePattern.equals(defaultPattern) ? statePattern : null;
            dto.format = patterToSet;
            patternSet = patterToSet != null;
        }

        dto.type = item.getType();
        String mainType = ItemUtil.getMainItemType(item.getType());
        String dimension = ItemUtil.getItemTypeExtension(item.getType());
        if (CoreItemFactory.NUMBER.equals(mainType) && dimension != null) {
            dto.type = mainType;
            dto.dimension = dimension;
        }
        if (item instanceof GroupItem groupItem) {
            Item baseItem = groupItem.getBaseItem();
            if (baseItem != null) {
                dto.group = new YamlGroupDTO();
                dto.group.type = baseItem.getType();
                mainType = ItemUtil.getMainItemType(baseItem.getType());
                dimension = ItemUtil.getItemTypeExtension(baseItem.getType());
                if (CoreItemFactory.NUMBER.equals(mainType) && dimension != null) {
                    dto.group.type = mainType;
                    dto.group.dimension = dimension;
                }
                GroupFunction function = groupItem.getFunction();
                if (function != null && !(function instanceof Equality)) {
                    dto.group.function = function.getClass().getSimpleName();
                    List<String> params = new ArrayList<>();
                    State[] parameters = function.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        params.add(parameters[i].toString());
                    }
                    dto.group.parameters = params.isEmpty() ? null : params;
                }
            }
        }

        String category = item.getCategory();
        if (category != null && !category.isEmpty()) {
            dto.icon = category;
        }

        if (!item.getGroupNames().isEmpty()) {
            dto.groups = new ArrayList<>();
            item.getGroupNames().forEach(group -> {
                dto.groups.add(group);
            });
        }
        if (!item.getTags().isEmpty()) {
            dto.tags = new LinkedHashSet<>();
            item.getTags().stream().sorted().collect(Collectors.toList()).forEach(tag -> {
                dto.tags.add(tag);
            });
        }

        if (channelLinks.size() == 1 && channelLinks.getFirst().getConfiguration().isEmpty()) {
            dto.channel = channelLinks.getFirst().getValue();
        } else if (!channelLinks.isEmpty()) {
            dto.channels = new LinkedHashMap<>();
            channelLinks.forEach(md -> {
                Map<String, Object> configuration = new LinkedHashMap<>();
                getConfigurationParameters(md, hideDefaultParameters).forEach(param -> {
                    configuration.put(param.name(), param.value());
                });
                dto.channels.put(md.getValue(), configuration);
            });
        }

        Map<String, YamlMetadataDTO> metadataDto = new LinkedHashMap<>();
        for (Metadata md : metadata) {
            String namespace = md.getUID().getNamespace();
            if ("autoupdate".equals(namespace)) {
                dto.autoupdate = Boolean.valueOf(md.getValue());
            } else if ("unit".equals(namespace)) {
                dto.unit = md.getValue();
            } else {
                YamlMetadataDTO mdDto = new YamlMetadataDTO();
                mdDto.value = md.getValue().isEmpty() ? null : md.getValue();
                Map<String, Object> configuration = new LinkedHashMap<>();
                String statePattern = null;
                for (ConfigParameter param : getConfigurationParameters(md)) {
                    configuration.put(param.name(), param.value());
                    if ("stateDescription".equals(namespace) && "pattern".equals(param.name())) {
                        statePattern = param.value().toString();
                    }
                }
                // Ignore state description in case it contains only a state pattern and state pattern was injected
                // in field format or is the default pattern
                if (!(statePattern != null && configuration.size() == 1
                        && (patternSet || statePattern.equals(defaultPattern)))) {
                    mdDto.config = configuration.isEmpty() ? null : configuration;
                    metadataDto.put(namespace, mdDto);
                    if (patternSet && statePattern != null) {
                        dto.format = null;
                    }
                }
            }
        }
        dto.metadata = metadataDto.isEmpty() ? null : metadataDto;

        return dto;
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

    @Override
    public String getFileFormatParser() {
        return "YAML";
    }

    @Override
    public @Nullable String startParsingFileFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel(inputStream, errors, warnings);
    }

    @Override
    public Collection<Item> getParsedItems(String modelName) {
        return itemProvider.getAllFromModel(modelName);
    }

    @Override
    public Collection<Metadata> getParsedMetadata(String modelName) {
        return metadataProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFileFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
