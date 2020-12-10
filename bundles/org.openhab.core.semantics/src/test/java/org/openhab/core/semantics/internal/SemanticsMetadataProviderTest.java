/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.semantics.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.library.items.SwitchItem;

/**
 * @author Simon Lamon - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class SemanticsMetadataProviderTest {

    private static final String ITEM_NAME = "switchItem";

    private @NonNullByDefault({}) @Mock ItemRegistry itemRegistry;
    private @NonNullByDefault({}) @Mock ProviderChangeListener<@NonNull Metadata> changeListener;

    private @NonNullByDefault({}) SemanticsMetadataProvider semanticsMetadataProvider;

    @BeforeEach
    public void beforeEach() throws Exception {
        semanticsMetadataProvider = new SemanticsMetadataProvider(itemRegistry) {
            {
                addProviderChangeListener(changeListener);
            }
        };
    }

    private void assertCorrectAddedEvents(Metadata expected) {
        ArgumentCaptor<Metadata> eventCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(changeListener, atLeastOnce()).added(eq(semanticsMetadataProvider), eventCaptor.capture());
        Metadata event = eventCaptor.getAllValues().stream().findFirst().get();
        assertEquals(expected, event);
    }

    private void assertCorrectUpdatedEvents(Metadata oldExpected, Metadata expected) {
        ArgumentCaptor<Metadata> eventCaptor = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<Metadata> oldEventCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(changeListener, atLeastOnce()).updated(eq(semanticsMetadataProvider), oldEventCaptor.capture(),
                eventCaptor.capture());
        Metadata oldEvent = oldEventCaptor.getAllValues().stream().findFirst().get();
        Metadata event = eventCaptor.getAllValues().stream().findFirst().get();
        assertEquals(oldExpected, oldEvent);
        assertEquals(expected, event);
    }

    private void assertCorrectRemoveEvents(Metadata expected) {
        ArgumentCaptor<Metadata> eventCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(changeListener, atLeastOnce()).removed(eq(semanticsMetadataProvider), eventCaptor.capture());
        Metadata event = eventCaptor.getAllValues().stream().findFirst().get();
        assertEquals(expected, event);
    }

    @Test
    public void testItemAdded() {
        GenericItem item = new SwitchItem(ITEM_NAME);
        item.addTag("Door");
        semanticsMetadataProvider.added(item);

        Metadata metadata = getMetadata(item);
        assertNotNull(metadata);
        if (metadata != null) {
            assertEquals("Equipment_Door", metadata.getValue());
            assertCorrectAddedEvents(metadata);
        }
    }

    @Test
    public void testItemUpdatedToAnotherTag() {
        GenericItem oldItem = new SwitchItem(ITEM_NAME);
        oldItem.addTag("Door");
        semanticsMetadataProvider.added(oldItem);

        Metadata oldMetadata = getMetadata(oldItem);
        assertNotNull(oldMetadata);

        GenericItem newItem = new SwitchItem(ITEM_NAME);
        newItem.addTag("Indoor");
        semanticsMetadataProvider.updated(oldItem, newItem);

        Metadata metadata = getMetadata(newItem);
        assertNotNull(metadata);
        if (oldMetadata != null && metadata != null) {
            assertEquals("Location_Indoor", metadata.getValue());
            assertCorrectUpdatedEvents(oldMetadata, metadata);
        }
    }

    @Test
    public void testItemUpdatedToNoTag() {
        GenericItem oldItem = new SwitchItem(ITEM_NAME);
        oldItem.addTag("Door");
        semanticsMetadataProvider.added(oldItem);

        Metadata oldMetadata = getMetadata(oldItem);
        assertNotNull(oldMetadata);

        GenericItem newItem = new SwitchItem(ITEM_NAME);
        semanticsMetadataProvider.updated(oldItem, newItem);

        Metadata metadata = getMetadata(newItem);
        assertNull(metadata);
        if (oldMetadata != null) {
            assertCorrectRemoveEvents(oldMetadata);
        }
    }

    @Test
    public void testItemRemoved() {
        GenericItem item = new SwitchItem(ITEM_NAME);
        item.addTag("Door");
        semanticsMetadataProvider.added(item);

        Metadata oldMetadata = getMetadata(item);
        assertNotNull(oldMetadata);

        semanticsMetadataProvider.removed(item);

        Metadata metadata = getMetadata(item);
        assertNull(metadata);
        if (oldMetadata != null) {
            assertCorrectRemoveEvents(oldMetadata);
        }
    }

    private @Nullable Metadata getMetadata(Item item) {
        Collection<Metadata> metadataCollection = semanticsMetadataProvider.getAll();
        Optional<Metadata> foundMetadata = metadataCollection.stream()
                .filter(metadata -> metadata.getUID().getItemName().equals(item.getName())).findFirst();
        if (foundMetadata.isPresent()) {
            return foundMetadata.get();
        } else {
            return null;
        }
    }
}
