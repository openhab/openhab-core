/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.library;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("null")
public class CoreItemFactoryTest {

    private CoreItemFactory coreItemFactory;

    private List<String> itemTypeNames;

    @Before
    public void setup() {
        coreItemFactory = new CoreItemFactory();
        itemTypeNames = Arrays.asList(coreItemFactory.getSupportedItemTypes());
    }

    @Test
    public void shouldCreateItems() {
        for (String itemTypeName : itemTypeNames) {
            GenericItem item = coreItemFactory.createItem(itemTypeName, itemTypeName.toLowerCase());

            assertThat(item.getType(), is(itemTypeName));
            assertThat(item.getName(), is(itemTypeName.toLowerCase()));
        }
    }

    @Test
    public void createNumberItemWithDimension() {
        NumberItem numberItem = (NumberItem) coreItemFactory.createItem(CoreItemFactory.NUMBER + ":Temperature",
                "myNumberItem");

        assertThat(numberItem.getDimension(), equalTo(Temperature.class));
    }

    @Test
    public void shouldReturnNullForUnsupportedItemTypeName() {
        GenericItem item = coreItemFactory.createItem("NoValidItemTypeName", "IWantMyItem");

        assertThat(item, is(nullValue()));
    }

}
