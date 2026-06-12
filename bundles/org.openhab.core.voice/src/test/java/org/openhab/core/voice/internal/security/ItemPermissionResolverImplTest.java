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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.voice.security.ItemPermissionResolver.PERMISSION_PROPERTY;
import static org.openhab.core.voice.security.ItemPermissionResolver.SYSTEM_DEFAULT_SOURCE;
import static org.openhab.core.voice.security.ItemPermissionResolver.VOICE_SYSTEM_NAMESPACE;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.voice.internal.VoiceConfigurationConstants;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;

/**
 * Tests for {@link ItemPermissionResolver}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class ItemPermissionResolverImplTest {
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistry;

    private @NonNullByDefault({}) ItemPermissionResolverImpl itemAccessResolver;
    private SwitchItem item = new SwitchItem("TestItem");

    @BeforeEach
    public void setUp() {
        itemAccessResolver = new ItemPermissionResolverImpl(itemRegistry, metadataRegistry,
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION,
                        VoiceConfigurationConstants.DEFAULT_IMPLICIT_ITEM_ACCESS.name()));
        item = new SwitchItem("TestItem");
    }

    @AfterEach
    public void tearDown() {
        itemAccessResolver.dispose();
        itemAccessResolver = null;
    }

    @Test
    public void testCacheWorks() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        assertTrue(itemAccessResolver.isAccessible(item));

        // Metadata registry should only be queried once
        verify(metadataRegistry, times(1)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheInvalidatedOnItemAdded() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(itemRegistry).addRegistryChangeListener(argThat(l -> {
            l.added(item);
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        verify(metadataRegistry, times(2)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheInvalidatedOnItemRemoved() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(itemRegistry).addRegistryChangeListener(argThat(l -> {
            l.removed(item);
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        verify(metadataRegistry, times(2)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheInvalidatedOnItemUpdated() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(itemRegistry).addRegistryChangeListener(argThat(l -> {
            l.updated(item, item);
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        verify(metadataRegistry, times(2)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheInvalidatedOnVoiceSystemMetadataAdded() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(metadataRegistry).addRegistryChangeListener(argThat(l -> {
            l.added(new Metadata(new MetadataKey(VOICE_SYSTEM_NAMESPACE, item.getName()), "", Map.of()));
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        verify(metadataRegistry, times(2)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheInvalidatedOnVoiceSystemMetadataRemoved() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(metadataRegistry).addRegistryChangeListener(argThat(l -> {
            l.removed(new Metadata(new MetadataKey(VOICE_SYSTEM_NAMESPACE, item.getName()), "", Map.of()));
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        verify(metadataRegistry, times(2)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheInvalidatedOnVoiceSystemMetadataUpdated() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(metadataRegistry).addRegistryChangeListener(argThat(l -> {
            Metadata metadata = new Metadata(new MetadataKey(VOICE_SYSTEM_NAMESPACE, item.getName()), "", Map.of());
            l.updated(metadata, metadata);
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        verify(metadataRegistry, times(2)).get(any(MetadataKey.class));
    }

    @Test
    public void testCacheNotInvalidatedOnOtherMetadataNamespace() {
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        verify(metadataRegistry).addRegistryChangeListener(argThat(l -> {
            l.added(new Metadata(new MetadataKey("otherNamespace", item.getName()), "", Map.of()));
            return true;
        }));
        assertTrue(itemAccessResolver.isAccessible(item));

        // Metadata registry should still only be queried once
        verify(metadataRegistry, times(1)).get(any(MetadataKey.class));
    }

    @Test
    public void testAccessChangeListenerNotifiedOnImplicitAccessChange() {
        int[] notifications = new int[1];
        itemAccessResolver.addItemAccessChangeListener(() -> notifications[0]++);

        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        assertEquals(1, notifications[0]);
    }

    @Test
    public void testAccessChangeListenerNotifiedOnVoiceSystemMetadataChange() {
        int[] notifications = new int[1];
        itemAccessResolver.addItemAccessChangeListener(() -> notifications[0]++);
        Metadata metadata = new Metadata(new MetadataKey(VOICE_SYSTEM_NAMESPACE, item.getName()), "", Map.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RegistryChangeListener<Metadata>> listenerCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        verify(metadataRegistry).addRegistryChangeListener(listenerCaptor.capture());
        listenerCaptor.getValue().updated(metadata, metadata);

        assertEquals(1, notifications[0]);
    }

    @Test
    public void testExplicitAllowOnItem() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));
        stubMetadata(item.getName(), ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(ItemPermission.READ_WRITE, itemAccessResolver.getPermission(item));
        assertEquals(item.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testExplicitDenyOnItem() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.READ_WRITE.name()));
        stubMetadata(item.getName(), ItemPermission.NO_ACCESS);

        assertFalse(itemAccessResolver.isAccessible(item));
        assertEquals(ItemPermission.NO_ACCESS, itemAccessResolver.getPermission(item));
        assertEquals(item.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testInheritAllowFromParentGroup() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));
        item.addGroupName("ParentGroup");
        GroupItem parentGroup = new GroupItem("ParentGroup");
        when(itemRegistry.get("ParentGroup")).thenReturn(parentGroup);
        stubMetadata("ParentGroup", ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(parentGroup));
        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(ItemPermission.READ_WRITE, itemAccessResolver.getPermission(item));
        assertEquals(parentGroup.getName(), itemAccessResolver.getItemPermissionDetails(parentGroup).source());
        assertEquals(parentGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testInheritDenyFromParentGroup() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.READ_WRITE.name()));
        item.addGroupName("ParentGroup");
        GroupItem parentGroup = new GroupItem("ParentGroup");
        when(itemRegistry.get("ParentGroup")).thenReturn(parentGroup);
        stubMetadata("ParentGroup", ItemPermission.NO_ACCESS);

        assertFalse(itemAccessResolver.isAccessible(parentGroup));
        assertFalse(itemAccessResolver.isAccessible(item));
        assertEquals(ItemPermission.NO_ACCESS, itemAccessResolver.getPermission(item));
        assertEquals(parentGroup.getName(), itemAccessResolver.getItemPermissionDetails(parentGroup).source());
        assertEquals(parentGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testMergingDenyHasPriorityOverAllow() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        item.addGroupName("DenyGroup");
        item.addGroupName("AllowGroup");

        GroupItem denyGroup = new GroupItem("DenyGroup");
        GroupItem allowGroup = new GroupItem("AllowGroup");

        lenient().when(itemRegistry.get("DenyGroup")).thenReturn(denyGroup);
        lenient().when(itemRegistry.get("AllowGroup")).thenReturn(allowGroup);

        stubMetadata("DenyGroup", ItemPermission.NO_ACCESS);
        stubMetadata("AllowGroup", ItemPermission.READ_WRITE);

        // Even though one group allows, the other denies, and no-access has priority.
        assertFalse(itemAccessResolver.isAccessible(item));
        assertEquals(ItemPermission.NO_ACCESS, itemAccessResolver.getPermission(item));
        assertEquals(denyGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testMergingReadOnlyPriority() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        item.addGroupName("ReadOnlyGroup");
        item.addGroupName("ReadWriteGroup");

        GroupItem readOnlyGroup = new GroupItem("ReadOnlyGroup");
        GroupItem readWriteGroup = new GroupItem("ReadWriteGroup");

        lenient().when(itemRegistry.get("ReadOnlyGroup")).thenReturn(readOnlyGroup);
        lenient().when(itemRegistry.get("ReadWriteGroup")).thenReturn(readWriteGroup);

        stubMetadata("ReadOnlyGroup", ItemPermission.READ_ONLY);
        stubMetadata("ReadWriteGroup", ItemPermission.READ_WRITE);

        // READ_ONLY has priority over READ_WRITE in same layer.
        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(ItemPermission.READ_ONLY, itemAccessResolver.getPermission(item));
        assertEquals(readOnlyGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testMultiLevelInheritance() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        item.addGroupName("ParentGroup");
        GroupItem parentGroup = new GroupItem("ParentGroup");
        parentGroup.addGroupName("GrandparentGroup");
        GroupItem grandparentGroup = new GroupItem("GrandparentGroup");

        lenient().when(itemRegistry.get("ParentGroup")).thenReturn(parentGroup);
        lenient().when(itemRegistry.get("GrandparentGroup")).thenReturn(grandparentGroup);

        // Parent has no metadata, grandparent allows
        stubMetadata("GrandparentGroup", ItemPermission.READ_WRITE);

        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(grandparentGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testFallbackToSystemDefaultTrue() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.READ_WRITE.name()));

        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(SYSTEM_DEFAULT_SOURCE, itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testFallbackToSystemDefaultFalse() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        assertFalse(itemAccessResolver.isAccessible(item));
        assertEquals(SYSTEM_DEFAULT_SOURCE, itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testCircularGroupMembership() {
        item.addGroupName("GroupA");
        GroupItem groupA = new GroupItem("GroupA");
        groupA.addGroupName("GroupB");
        GroupItem groupB = new GroupItem("GroupB");
        groupB.addGroupName("GroupA"); // Circular

        lenient().when(itemRegistry.get("GroupA")).thenReturn(groupA);
        lenient().when(itemRegistry.get("GroupB")).thenReturn(groupB);

        // No explicit allow/deny, should fallback to default
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.READ_WRITE.name()));
        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(SYSTEM_DEFAULT_SOURCE, itemAccessResolver.getItemPermissionDetails(item).source());
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));
        assertFalse(itemAccessResolver.isAccessible(item));
        assertEquals(SYSTEM_DEFAULT_SOURCE, itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testGrandparentAllowParentDeny() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.READ_WRITE.name()));

        item.addGroupName("ParentGroup");
        GroupItem parentGroup = new GroupItem("ParentGroup");
        parentGroup.addGroupName("GrandparentGroup");
        GroupItem grandparentGroup = new GroupItem("GrandparentGroup");

        lenient().when(itemRegistry.get("ParentGroup")).thenReturn(parentGroup);
        lenient().when(itemRegistry.get("GrandparentGroup")).thenReturn(grandparentGroup);

        stubMetadata("ParentGroup", ItemPermission.NO_ACCESS);
        stubMetadata("GrandparentGroup", ItemPermission.READ_WRITE);

        // Parent denies, which should have priority over grandparent allowing.
        assertFalse(itemAccessResolver.isAccessible(item));
        assertEquals(parentGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    @Test
    public void testGrandparentDenyParentAllow() {
        itemAccessResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.READ_WRITE.name()));

        item.addGroupName("ParentGroup");
        GroupItem parentGroup = new GroupItem("ParentGroup");
        parentGroup.addGroupName("GrandparentGroup");
        GroupItem grandparentGroup = new GroupItem("GrandparentGroup");

        lenient().when(itemRegistry.get("ParentGroup")).thenReturn(parentGroup);
        lenient().when(itemRegistry.get("GrandparentGroup")).thenReturn(grandparentGroup);

        stubMetadata("ParentGroup", ItemPermission.READ_WRITE);
        stubMetadata("GrandparentGroup", ItemPermission.NO_ACCESS);

        // Parent allows, which should have priority over grandparent denying.
        assertTrue(itemAccessResolver.isAccessible(item));
        assertEquals(parentGroup.getName(), itemAccessResolver.getItemPermissionDetails(item).source());
    }

    private void stubMetadata(String itemName, ItemPermission permission) {
        MetadataKey key = new MetadataKey(VOICE_SYSTEM_NAMESPACE, itemName);
        Map<String, Object> config = new HashMap<>();
        config.put(PERMISSION_PROPERTY, permission.name());
        Metadata metadata = new Metadata(key, "", config);
        lenient().when(metadataRegistry.get(key)).thenReturn(metadata);
    }
}
