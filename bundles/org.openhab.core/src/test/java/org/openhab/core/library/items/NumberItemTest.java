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
package org.openhab.core.library.items;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 *
 * @author Stefan Triller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class NumberItemTest {

    private static final String ITEM_NAME = "test";

    private NumberItem getItem(@Nullable String unitString) {
        NumberItem numberItem = new NumberItem(ITEM_NAME);
        if (unitString != null) {
            MetadataKey metadataKey = new MetadataKey("unit", ITEM_NAME);
            Metadata metadata = new Metadata(metadataKey, unitString, null);
            numberItem.addedMetadata(metadata);
        }
        return numberItem;
    }

    @Test
    public void setDecimalType() {
        NumberItem item = getItem(null);
        State decimal = new DecimalType("23");
        item.setState(decimal);
        assertEquals(decimal, item.getState());
    }

    @Test
    public void setPercentType() {
        NumberItem item = getItem(null);
        State percent = new PercentType(50);
        item.setState(percent);
        assertEquals(percent, item.getState());
    }

    @Test
    public void setHSBType() {
        NumberItem item = getItem(null);
        State hsb = new HSBType("5,23,42");
        item.setState(hsb);
        assertEquals(hsb, item.getState());
    }

    @Test
    public void testUndefType() {
        NumberItem item = getItem(null);
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        NumberItem item = getItem(null);
        StateUtil.testAcceptedStates(item);
    }

    @Test
    public void testSetQuantityTypeAccepted() {
        NumberItem item = getItem("°C");
        item.setState(new QuantityType<>("20 °C"));

        assertThat(item.getState(), is(new QuantityType<>("20 °C")));
    }

    @Test
    public void testSetQuantityOnPlainNumberStripsUnit() {
        NumberItem item = getItem(null);
        item.setState(new QuantityType<>("20 °C"));

        assertThat(item.getState(), is(new DecimalType("20")));
    }

    @Test
    public void testSetQuantityTypeConverted() {
        NumberItem item = getItem("°C");
        item.setState(new QuantityType<>(68, ImperialUnits.FAHRENHEIT));

        assertThat(item.getState(), is(new QuantityType<>("20 °C")));
    }

    @Test
    public void testSetQuantityTypeUnconverted() {
        NumberItem item = getItem("°C");
        item.setState(new QuantityType<>("10 A")); // should not be accepted as valid state

        assertThat(item.getState(), is(UnDefType.NULL));
    }

    @Test
    public void testCommandUnitIsPassedForDimensionItem() {
        NumberItem item = getItem("°C");

        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        QuantityType<?> command = new QuantityType<>("15 °C");
        item.send(command);

        ArgumentCaptor<ItemCommandEvent> captor = ArgumentCaptor.forClass(ItemCommandEvent.class);
        verify(eventPublisher).post(captor.capture());

        ItemCommandEvent event = captor.getValue();
        assertThat(event.getItemCommand(), is(command));
    }

    @Test
    public void testCommandUnitIsStrippedForDimensionlessItem() {
        NumberItem item = getItem(null);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        item.setEventPublisher(eventPublisher);

        item.send(new QuantityType<>("15 °C"));

        ArgumentCaptor<ItemCommandEvent> captor = ArgumentCaptor.forClass(ItemCommandEvent.class);
        verify(eventPublisher).post(captor.capture());

        ItemCommandEvent event = captor.getValue();
        assertThat(event.getItemCommand(), is(new DecimalType("15")));
    }

    @SuppressWarnings("null")
    @Test
    public void testMiredToKelvin() {
        NumberItem item = getItem("K");
        item.setState(new QuantityType<>("370 mired"));

        assertThat(item.getState().format("%.0f K"), is("2703 K"));
    }

    @SuppressWarnings("null")
    @Test
    public void testKelvinToMired() {
        NumberItem item = getItem("mired");
        item.setState(new QuantityType<>("2700 K"));

        assertThat(item.getState().format("%.0f mired"), is("370 mired"));
    }

    @Test
    public void testRegistryChangeListenerIsRegistered() {
    }
}
