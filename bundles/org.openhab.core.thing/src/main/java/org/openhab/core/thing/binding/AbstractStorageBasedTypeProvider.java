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
package org.openhab.core.thing.binding;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.internal.type.TriggerChannelTypeBuilderImpl;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;

/**
 * The {@link AbstractStorageBasedTypeProvider} is the base class for the implementation of a {@link Storage} based
 * {@link ThingTypeProvider}, {@link ChannelTypeProvider} and {@link ChannelGroupTypeProvider}
 *
 * It can be subclassed by bindings that create {@link ThingType}s and {@link ChannelType}s on-the-fly and need to
 * persist those for future thing initializations
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractStorageBasedTypeProvider
        implements ThingTypeProvider, ChannelTypeProvider, ChannelGroupTypeProvider {

    private final Storage<ThingTypeEntity> thingTypeEntityStorage;
    private final Storage<ChannelTypeEntity> channelTypeEntityStorage;
    private final Storage<ChannelGroupTypeEntity> channelGroupTypeEntityStorage;

    /**
     * Instantiate a new storage based type provider. The subclass needs to be a
     * {@link org.osgi.service.component.annotations.Component} and declare itself as {@link ThingTypeProvider} and/or
     * {@link ChannelTypeProvider} and/or {@link ChannelGroupTypeProvider}.
     *
     * @param storageService a persistent {@link StorageService}
     */
    public AbstractStorageBasedTypeProvider(StorageService storageService) {
        String thingTypeStorageName = getClass().getName() + "-ThingType";
        String channelTypeStorageName = getClass().getName() + "-ChannelType";
        String channelGroupTypeStorageName = getClass().getName() + "-ChannelGroupType";
        ClassLoader classLoader = getClass().getClassLoader();
        thingTypeEntityStorage = storageService.getStorage(thingTypeStorageName, classLoader);
        channelTypeEntityStorage = storageService.getStorage(channelTypeStorageName, classLoader);
        channelGroupTypeEntityStorage = storageService.getStorage(channelGroupTypeStorageName, classLoader);
    }

    /**
     * Add or update a {@link ThingType} to the storage
     *
     * @param thingType the {@link ThingType} that needs to be stored
     */
    public void putThingType(ThingType thingType) {
        thingTypeEntityStorage.put(thingType.getUID().toString(), mapToEntity(thingType));
    }

    /**
     * Remove a {@link ThingType} from the storage
     *
     * @param thingTypeUID the {@link ThingTypeUID} of the thing type
     */
    public void removeThingType(ThingTypeUID thingTypeUID) {
        thingTypeEntityStorage.remove(thingTypeUID.toString());
    }

    /**
     * Add or update a {@link ChannelType} to the storage
     *
     * @param channelType the {@link ChannelType} that needs to be stored
     */
    public void putChannelType(ChannelType channelType) {
        channelTypeEntityStorage.put(channelType.getUID().toString(), mapToEntity(channelType));
    }

    /**
     * Remove a {@link ChannelType} from the storage
     *
     * @param channelTypeUID the {@link ChannelTypeUID} of the channel type
     */
    public void removeChannelType(ChannelTypeUID channelTypeUID) {
        channelTypeEntityStorage.remove(channelTypeUID.toString());
    }

    /**
     * Add or update a {@link ChannelGroupType} to the storage
     *
     * @param channelGroupType the {@link ChannelType} that needs to be stored
     */
    public void putChannelGroupType(ChannelGroupType channelGroupType) {
        channelGroupTypeEntityStorage.put(channelGroupType.getUID().toString(), mapToEntity(channelGroupType));
    }

    /**
     * Remove a {@link ChannelGroupType} from the storage
     *
     * @param channelGroupTypeUID the {@link ChannelGroupTypeUID} of the channel type
     */
    public void removeChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID) {
        channelGroupTypeEntityStorage.remove(channelGroupTypeUID.toString());
    }

    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return thingTypeEntityStorage.stream().map(Map.Entry::getValue).filter(Objects::nonNull)
                .map(Objects::requireNonNull).map(AbstractStorageBasedTypeProvider::mapFromEntity).toList();
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        ThingTypeEntity entity = thingTypeEntityStorage.get(thingTypeUID.toString());
        if (entity != null) {
            return mapFromEntity(entity);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return channelTypeEntityStorage.stream().map(Map.Entry::getValue).filter(Objects::nonNull)
                .map(Objects::requireNonNull).map(AbstractStorageBasedTypeProvider::mapFromEntity).toList();
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        ChannelTypeEntity entity = channelTypeEntityStorage.get(channelTypeUID.toString());
        if (entity != null) {
            return mapFromEntity(entity);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return channelGroupTypeEntityStorage.stream().map(Map.Entry::getValue).filter(Objects::nonNull)
                .map(Objects::requireNonNull).map(AbstractStorageBasedTypeProvider::mapFromEntity).toList();
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        ChannelGroupTypeEntity entity = channelGroupTypeEntityStorage.get(channelGroupTypeUID.toString());
        if (entity != null) {
            return mapFromEntity(entity);
        } else {
            return null;
        }
    }

    static ThingTypeEntity mapToEntity(ThingType thingType) {
        ThingTypeEntity entity = new ThingTypeEntity();
        entity.uid = thingType.getUID();
        entity.label = thingType.getLabel();
        entity.description = thingType.getDescription();

        entity.supportedBridgeTypeRefs = thingType.getSupportedBridgeTypeUIDs();
        entity.configDescriptionUri = thingType.getConfigDescriptionURI();
        entity.category = thingType.getCategory();
        entity.channelGroupDefinitions = thingType.getChannelGroupDefinitions().stream()
                .map(AbstractStorageBasedTypeProvider::mapToEntity).collect(Collectors.toList());
        entity.channelDefinitions = thingType.getChannelDefinitions().stream()
                .map(AbstractStorageBasedTypeProvider::mapToEntity).toList();
        entity.representationProperty = thingType.getRepresentationProperty();
        entity.properties = thingType.getProperties();
        entity.isListed = thingType.isListed();
        entity.extensibleChannelTypeIds = thingType.getExtensibleChannelTypeIds();
        entity.isBridge = thingType instanceof BridgeType;

        return entity;
    }

    static ChannelDefinitionEntity mapToEntity(ChannelDefinition channelDefinition) {
        ChannelDefinitionEntity entity = new ChannelDefinitionEntity();
        entity.id = channelDefinition.getId();
        entity.uid = channelDefinition.getChannelTypeUID();
        entity.label = channelDefinition.getLabel();
        entity.description = channelDefinition.getDescription();
        entity.properties = channelDefinition.getProperties();
        entity.autoUpdatePolicy = channelDefinition.getAutoUpdatePolicy();
        return entity;
    }

    static ChannelGroupDefinitionEntity mapToEntity(ChannelGroupDefinition channelGroupDefinition) {
        ChannelGroupDefinitionEntity entity = new ChannelGroupDefinitionEntity();
        entity.id = channelGroupDefinition.getId();
        entity.typeUid = channelGroupDefinition.getTypeUID();
        entity.label = channelGroupDefinition.getLabel();
        entity.description = channelGroupDefinition.getDescription();
        return entity;
    }

    static ChannelTypeEntity mapToEntity(ChannelType channelType) {
        ChannelTypeEntity entity = new ChannelTypeEntity();
        entity.uid = channelType.getUID();
        entity.label = channelType.getLabel();
        entity.description = channelType.getDescription();
        entity.configDescriptionURI = channelType.getConfigDescriptionURI();
        entity.advanced = channelType.isAdvanced();
        entity.itemType = channelType.getItemType();
        entity.kind = channelType.getKind();
        entity.tags = channelType.getTags();
        entity.category = channelType.getCategory();
        StateDescription stateDescription = channelType.getState();
        entity.stateDescriptionFragment = stateDescription == null ? null
                : StateDescriptionFragmentBuilder.create(stateDescription).build();
        entity.commandDescription = channelType.getCommandDescription();
        entity.event = channelType.getEvent();
        entity.autoUpdatePolicy = channelType.getAutoUpdatePolicy();
        return entity;
    }

    static ChannelGroupTypeEntity mapToEntity(ChannelGroupType channelGroupType) {
        ChannelGroupTypeEntity entity = new ChannelGroupTypeEntity();
        entity.uid = channelGroupType.getUID();
        entity.label = channelGroupType.getLabel();
        entity.description = channelGroupType.getDescription();
        entity.category = channelGroupType.getCategory();
        entity.channelDefinitions = channelGroupType.getChannelDefinitions().stream()
                .map(AbstractStorageBasedTypeProvider::mapToEntity).toList();
        return entity;
    }

    static ThingType mapFromEntity(ThingTypeEntity entity) {
        ThingTypeBuilder builder = ThingTypeBuilder.instance(entity.uid, entity.label)
                .withSupportedBridgeTypeUIDs(entity.supportedBridgeTypeRefs).withProperties(entity.properties)
                .withChannelDefinitions(entity.channelDefinitions.stream()
                        .map(AbstractStorageBasedTypeProvider::mapFromEntity).toList())
                .withChannelGroupDefinitions(entity.channelGroupDefinitions.stream()
                        .map(AbstractStorageBasedTypeProvider::mapFromEntity).toList())
                .isListed(entity.isListed).withExtensibleChannelTypeIds(entity.extensibleChannelTypeIds);
        if (entity.description != null) {
            builder.withDescription(Objects.requireNonNull(entity.description));
        }
        if (entity.category != null) {
            builder.withCategory(Objects.requireNonNull(entity.category));
        }
        if (entity.configDescriptionUri != null) {
            builder.withConfigDescriptionURI(Objects.requireNonNull(entity.configDescriptionUri));
        }
        if (entity.representationProperty != null) {
            builder.withRepresentationProperty(Objects.requireNonNull(entity.representationProperty));
        }
        return entity.isBridge ? builder.buildBridge() : builder.build();
    }

    static ChannelDefinition mapFromEntity(ChannelDefinitionEntity entity) {
        return new ChannelDefinitionBuilder(entity.id, entity.uid).withLabel(entity.label)
                .withDescription(entity.description).withProperties(entity.properties)
                .withAutoUpdatePolicy(entity.autoUpdatePolicy).build();
    }

    static ChannelGroupDefinition mapFromEntity(ChannelGroupDefinitionEntity entity) {
        return new ChannelGroupDefinition(entity.id, entity.typeUid, entity.label, entity.description);
    }

    static ChannelType mapFromEntity(ChannelTypeEntity entity) {
        ChannelTypeBuilder<?> builder = (entity.kind == ChannelKind.STATE)
                ? ChannelTypeBuilder.state(entity.uid, entity.label, Objects.requireNonNull(entity.itemType))
                : ChannelTypeBuilder.trigger(entity.uid, entity.label);
        builder.isAdvanced(entity.advanced).withTags(entity.tags);
        if (entity.description != null) {
            builder.withDescription(Objects.requireNonNull(entity.description));
        }
        if (entity.configDescriptionURI != null) {
            builder.withConfigDescriptionURI(Objects.requireNonNull(entity.configDescriptionURI));
        }
        if (entity.category != null) {
            builder.withCategory(Objects.requireNonNull(entity.category));
        }
        if (builder instanceof StateChannelTypeBuilder stateBuilder) {
            if (entity.stateDescriptionFragment != null) {
                stateBuilder.withStateDescriptionFragment(Objects.requireNonNull(entity.stateDescriptionFragment));
            }
            if (entity.commandDescription != null) {
                stateBuilder.withCommandDescription(Objects.requireNonNull(entity.commandDescription));
            }
            if (entity.autoUpdatePolicy != null) {
                stateBuilder.withAutoUpdatePolicy(Objects.requireNonNull(entity.autoUpdatePolicy));
            }
        }
        if (builder instanceof TriggerChannelTypeBuilderImpl triggerBuilder) {
            if (entity.event != null) {
                triggerBuilder.withEventDescription(Objects.requireNonNull(entity.event));
            }
        }
        return builder.build();
    }

    static ChannelGroupType mapFromEntity(ChannelGroupTypeEntity entity) {
        ChannelGroupTypeBuilder builder = ChannelGroupTypeBuilder.instance(entity.uid, entity.label)
                .withChannelDefinitions(entity.channelDefinitions.stream()
                        .map(AbstractStorageBasedTypeProvider::mapFromEntity).toList());
        if (entity.description != null) {
            builder.withDescription(Objects.requireNonNull(entity.description));
        }
        if (entity.category != null) {
            builder.withCategory(Objects.requireNonNull(entity.category));
        }
        return builder.build();
    }

    static class ThingTypeEntity {
        public @NonNullByDefault({}) ThingTypeUID uid;
        public @NonNullByDefault({}) String label;
        public @Nullable String description;
        public @Nullable String category;
        public @Nullable String representationProperty;
        public List<String> supportedBridgeTypeRefs = List.of();
        public @Nullable URI configDescriptionUri;
        public List<String> extensibleChannelTypeIds = List.of();
        public List<ChannelGroupDefinitionEntity> channelGroupDefinitions = List.of();
        public List<ChannelDefinitionEntity> channelDefinitions = List.of();
        public Map<String, String> properties = Map.of();
        public boolean isListed = false;
        public boolean isBridge = false;
    }

    static class ChannelDefinitionEntity {
        public @NonNullByDefault({}) String id;
        public @NonNullByDefault({}) ChannelTypeUID uid;
        public @Nullable String label;
        public @Nullable String description;
        public Map<String, String> properties = Map.of();
        public @Nullable AutoUpdatePolicy autoUpdatePolicy;
    }

    static class ChannelGroupDefinitionEntity {
        public @NonNullByDefault({}) String id;
        public @NonNullByDefault({}) ChannelGroupTypeUID typeUid;
        public @Nullable String label;
        public @Nullable String description;
    }

    static class ChannelTypeEntity {
        public @NonNullByDefault({}) ChannelTypeUID uid;
        public @NonNullByDefault({}) String label;
        public @Nullable String description;
        public @Nullable URI configDescriptionURI;

        public boolean advanced;
        public @Nullable String itemType;
        public @NonNullByDefault({}) ChannelKind kind;
        public Set<String> tags = Set.of();
        public @Nullable String category;
        public @Nullable StateDescriptionFragment stateDescriptionFragment;
        public @Nullable CommandDescription commandDescription;
        public @Nullable EventDescription event;
        public @Nullable AutoUpdatePolicy autoUpdatePolicy;
    }

    static class ChannelGroupTypeEntity {
        public @NonNullByDefault({}) ChannelGroupTypeUID uid;
        public @NonNullByDefault({}) String label;
        public @Nullable String description;

        public List<ChannelDefinitionEntity> channelDefinitions = List.of();
        private @Nullable String category;
    }
}
