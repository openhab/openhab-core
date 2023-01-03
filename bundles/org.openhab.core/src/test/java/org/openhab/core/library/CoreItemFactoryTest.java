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
package org.openhab.core.library;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.items.NumberItem;

/**
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class CoreItemFactoryTest {

    @Test
    public void shouldCreateItems() {
        CoreItemFactory coreItemFactory = new CoreItemFactory();
        List<String> itemTypeNames = List.of(coreItemFactory.getSupportedItemTypes());
        for (String itemTypeName : itemTypeNames) {
            GenericItem item = coreItemFactory.createItem(itemTypeName, itemTypeName.toLowerCase());

            assertThat(item.getType(), is(itemTypeName));
            assertThat(item.getName(), is(itemTypeName.toLowerCase()));
        }
    }

    @Test
    public void createNumberItemWithDimension() {
        CoreItemFactory coreItemFactory = new CoreItemFactory();
        NumberItem numberItem = (NumberItem) coreItemFactory.createItem(CoreItemFactory.NUMBER + ":Temperature",
                "myNumberItem");

        assertThat(numberItem.getDimension(), equalTo(Temperature.class));
    }

    @Test
    public void shouldReturnNullForUnsupportedItemTypeName() {
        CoreItemFactory coreItemFactory = new CoreItemFactory();
        GenericItem item = coreItemFactory.createItem("NoValidItemTypeName", "IWantMyItem");

        assertThat(item, is(nullValue()));
    }
}
