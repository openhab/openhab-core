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
package org.openhab.core.items;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.library.items.NumberItem;

/**
 * The {@link GroupItemTest} contains tests for {@link GroupItem}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class GroupItemTest {
    private static final String ITEM_NAME = "test";

    private @Mock @NonNullByDefault({}) NumberItem baseItemMock;

    @Test
    public void testMetadataIsPropagatedToBaseItem() {
        GroupItem groupItem = new GroupItem(ITEM_NAME, baseItemMock, new GroupFunction.Equality());

        Metadata metadata = new Metadata(new MetadataKey("foo", ITEM_NAME), "foo", null);
        Metadata updatedMetadata = new Metadata(new MetadataKey("foo", ITEM_NAME), "bar", null);

        groupItem.addedMetadata(metadata);
        verify(baseItemMock).addedMetadata(eq(metadata));

        groupItem.updatedMetadata(metadata, updatedMetadata);
        verify(baseItemMock).updatedMetadata(eq(metadata), eq(updatedMetadata));

        groupItem.removedMetadata(updatedMetadata);
        verify(baseItemMock).removedMetadata(eq(updatedMetadata));
    }
}
