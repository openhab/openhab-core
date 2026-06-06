/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ItemPermissionResolver}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = ItemPermissionResolver.class, immediate = true)
@NonNullByDefault
public class ItemPermissionResolverImpl implements ItemPermissionResolver {
    private final Logger logger = LoggerFactory.getLogger(ItemPermissionResolverImpl.class);

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ConcurrentHashMap<String, ItemPermission> cache = new ConcurrentHashMap<>();
    private final Set<Runnable> listeners = ConcurrentHashMap.newKeySet();

    private ItemPermission implicitPermission = ItemPermission.READ_WRITE;

    private final RegistryChangeListener<Item> itemRegistryChangeListener = new RegistryChangeListener<>() {
        @Override
        public void added(Item element) {
            invalidate();
        }

        @Override
        public void removed(Item element) {
            invalidate();
        }

        @Override
        public void updated(Item oldElement, Item element) {
            invalidate();
        }
    };

    private final RegistryChangeListener<Metadata> voiceSystemMetadataChangeListener = new RegistryChangeListener<>() {
        @Override
        public void added(Metadata element) {
            invalidateIfVoiceSystemMetadata(element);
        }

        @Override
        public void removed(Metadata element) {
            invalidateIfVoiceSystemMetadata(element);
        }

        @Override
        public void updated(Metadata oldElement, Metadata element) {
            invalidateIfVoiceSystemMetadata(element);
        }

        private void invalidateIfVoiceSystemMetadata(Metadata metadata) {
            if (metadata.getUID().getNamespace().equals(VOICE_SYSTEM_NAMESPACE)) {
                invalidate();
            }
        }
    };

    @Activate
    public ItemPermissionResolverImpl(final @Reference ItemRegistry itemRegistry,
            final @Reference MetadataRegistry metadataRegistry) {
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemRegistry.addRegistryChangeListener(itemRegistryChangeListener);
        this.metadataRegistry.addRegistryChangeListener(voiceSystemMetadataChangeListener);
    }

    @Deactivate
    public void dispose() {
        itemRegistry.removeRegistryChangeListener(itemRegistryChangeListener);
        metadataRegistry.removeRegistryChangeListener(voiceSystemMetadataChangeListener);
    }

    private void invalidate() {
        cache.clear();
        listeners.forEach(Runnable::run);
    }

    @Override
    public void addItemAccessChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    @Override
    public void removeItemAccessChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    @Override
    public void setImplicitPermission(ItemPermission implicitPermission) {
        this.implicitPermission = implicitPermission;
        invalidate();
    }

    @Override
    public ItemPermission getPermission(Item item) {
        return Objects.requireNonNull(
                cache.computeIfAbsent(item.getUID(), (k) -> getItemPermissionDetails(item).permission()));
    }

    @Override
    public ItemPermissionDetails getItemPermissionDetails(Item item) {
        ItemPermission permission = getPermissionMetadata(item);
        if (permission != null) {
            return new ItemPermissionDetails(permission, item.getName());
        }

        Set<String> visitedGroups = new HashSet<>();
        if (item instanceof GroupItem) {
            visitedGroups.add(item.getUID());
        }
        ItemPermissionDetails inherited = resolveInheritedAccess(item, visitedGroups);
        return inherited != null ? inherited : new ItemPermissionDetails(implicitPermission, SYSTEM_DEFAULT_SOURCE);
    }

    private @Nullable ItemPermissionDetails resolveInheritedAccess(Item item, Set<String> visitedGroups) {
        Set<String> currentLayer = new HashSet<>();
        for (String groupName : item.getGroupNames()) {
            if (visitedGroups.add(groupName)) {
                currentLayer.add(groupName);
            }
        }

        while (!currentLayer.isEmpty()) {
            ItemPermissionDetails layerPermission = null;
            Set<String> nextLayer = new HashSet<>();

            for (String groupName : currentLayer) {
                Item group = itemRegistry.get(groupName);
                if (group == null) {
                    continue;
                }
                ItemPermission permission = getPermissionMetadata(group);
                if (permission != null) {
                    if (permission == ItemPermission.NO_ACCESS) {
                        return new ItemPermissionDetails(ItemPermission.NO_ACCESS, groupName); // NO_ACCESS has priority
                                                                                               // in same layer
                    }
                    if (layerPermission == null || permission.ordinal() < layerPermission.permission().ordinal()) {
                        layerPermission = new ItemPermissionDetails(permission, groupName);
                    }
                }
                for (String parentGroupName : group.getGroupNames()) {
                    if (visitedGroups.add(parentGroupName)) {
                        nextLayer.add(parentGroupName);
                    }
                }
            }

            if (layerPermission != null) {
                return layerPermission;
            }
            currentLayer = nextLayer;
        }

        return null;
    }

    private @Nullable ItemPermission getPermissionMetadata(Item item) {
        Metadata metadata = metadataRegistry.get(new MetadataKey(VOICE_SYSTEM_NAMESPACE, item.getName()));
        if (metadata != null) {
            Object permissionValue = metadata.getConfiguration().get(PERMISSION_PROPERTY);
            if (permissionValue instanceof String s) {
                try {
                    return ItemPermission.valueOf(s);
                } catch (IllegalArgumentException e) {
                    logger.warn(
                            "Invalid value '{}' for 'permission' configuration of 'voiceSystem' metadata of item '{}'.",
                            s, item.getName());
                }
            }
        }
        return null;
    }
}
