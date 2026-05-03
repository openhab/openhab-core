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
package org.openhab.core.transform.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Unit tests for {@link ItemDisplayStateUtil}.
 *
 * @author Florian Hotze - Initial contribution
 */
public class ItemDisplayStateUtilTest {

    private Item item;
    private StateDescription stateDescription;
    private final Locale locale = Locale.ENGLISH;
    private final ZoneId zoneId = ZoneId.of("Europe/Berlin");

    private TransformationHelper transformationHelper;
    private TransformationService mapTransformationService;

    @BeforeEach
    public void setUp() {
        item = mock(Item.class);
        stateDescription = mock(StateDescription.class);
        when(item.getStateDescription(locale)).thenReturn(stateDescription);

        // Setup TransformationHelper
        BundleContext bundleContext = mock(BundleContext.class);
        transformationHelper = new TransformationHelper(bundleContext);

        mapTransformationService = mock(TransformationService.class);
        @SuppressWarnings("unchecked")
        ServiceReference<TransformationService> serviceRef = mock(ServiceReference.class);
        when(serviceRef.getProperty(TransformationService.SERVICE_PROPERTY_NAME)).thenReturn("MAP");
        when(bundleContext.getService(serviceRef)).thenReturn(mapTransformationService);

        transformationHelper.setTransformationService(serviceRef);
    }

    @AfterEach
    public void tearDown() {
        transformationHelper.deactivate();
    }

    @Test
    public void getDisplayStateReturnsRawStateWhenNoStateDescription() {
        when(item.getStateDescription(locale)).thenReturn(null);
        State state = new StringType("someState");

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("someState"));
    }

    @Test
    public void getDisplayStateCallsFormatState() throws Exception {
        State state = new StringType("ON");
        when(item.getName()).thenReturn("testItem");
        when(stateDescription.getPattern()).thenReturn("MAP(test.map):%s");
        when(stateDescription.getOptions()).thenReturn(Collections.emptyList());
        when(mapTransformationService.transform("test.map", "ON")).thenReturn("Active");

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("Active"));
    }

    @Test
    public void formatStateReturnsUnDefStateAsString() {
        State state = UnDefType.NULL;
        String result = ItemDisplayStateUtil.formatState("item", "%s", Collections.emptyList(), state, zoneId);
        assertThat(result, is("NULL"));
    }

    @Test
    public void formatStateAppliesTransformation() throws Exception {
        State state = new StringType("ON");
        when(mapTransformationService.transform("test.map", "ON")).thenReturn("Active");

        String result = ItemDisplayStateUtil.formatState("item", "MAP(test.map):%s", Collections.emptyList(), state,
                zoneId);

        assertThat(result, is("Active"));
    }

    @Test
    public void formatStateReturnsRawStateWhenTransformationReturnsNull() throws Exception {
        State state = new StringType("ON");
        when(mapTransformationService.transform("test.map", "ON")).thenReturn(null);

        String result = ItemDisplayStateUtil.formatState("item", "MAP(test.map):%s", Collections.emptyList(), state,
                zoneId);

        assertThat(result, is("ON"));
    }

    @Test
    public void formatStateReturnsRawStateWhenTransformationServiceNotAvailable() {
        State state = new StringType("ON");

        String result = ItemDisplayStateUtil.formatState("item", "UNKNOWN(test.map):%s", Collections.emptyList(), state,
                zoneId);

        assertThat(result, is("ON"));
    }

    @Test
    public void formatStateAppliesStateOptionLabel() {
        State state = new StringType("1");
        StateOption option = new StateOption("1", "Option Label");

        String result = ItemDisplayStateUtil.formatState("item", "Prefix: %s", List.of(option), state, zoneId);

        assertThat(result, is("Prefix: Option Label"));
    }

    @Test
    public void formatStateAppliesStateOptionLabelWithoutPattern() {
        State state = new StringType("1");
        StateOption option = new StateOption("1", "Option Label");

        String result = ItemDisplayStateUtil.formatState("item", null, List.of(option), state, zoneId);

        assertThat(result, is("Option Label"));
    }

    @Test
    public void formatStateReturnsLabelWhenOptionPatternIsInvalid() {
        State state = new StringType("1");
        StateOption option = new StateOption("1", "Option Label");

        String result = ItemDisplayStateUtil.formatState("item", "%d", List.of(option), state, zoneId);

        assertThat(result, is("Option Label"));
    }

    @Test
    public void formatStateConvertsQuantityTypeToPatternUnit() {
        QuantityType<?> state = new QuantityType<>("20 °C");

        String result = ItemDisplayStateUtil.formatState("item", "%.1f °F", Collections.emptyList(), state, zoneId);

        // 20°C = 68°F
        assertThat(result, is("68.0 °F"));
    }

    @Test
    public void formatStateFormatsDateTimeTypeWithZoneId() {
        DateTimeType state = new DateTimeType("2023-01-01T10:00:00Z");

        // UTC 10:00 is 11:00 in Europe/Berlin (CET)
        String result = ItemDisplayStateUtil.formatState("item", "%1$tH:%1$tM", Collections.emptyList(), state, zoneId);

        assertThat(result, is("11:00"));
    }

    @Test
    public void formatStateFormatsStateWithRegularPattern() {
        State state = new StringType("test");

        String result = ItemDisplayStateUtil.formatState("item", "Result: %s", Collections.emptyList(), state, zoneId);

        assertThat(result, is("Result: test"));
    }

    @Test
    public void formatStateReturnsRawStateWhenPatternIsInvalid() {
        State state = new StringType("test");

        String result = ItemDisplayStateUtil.formatState("item", "%d", Collections.emptyList(), state, zoneId);

        assertThat(result, is("test"));
    }

    @Test
    public void transformValueSucceeds() throws Exception {
        when(mapTransformationService.transform("test.map", "input")).thenReturn("output");
        String result = ItemDisplayStateUtil.transform("MAP", "test.map", "input");
        assertThat(result, is("output"));
    }

    @Test
    public void transformValueThrowsWhenServiceNotFound() {
        TransformationException exception = assertThrows(TransformationException.class, () -> {
            ItemDisplayStateUtil.transform("UNKNOWN", "test.map", "input");
        });
        assertThat(exception.getMessage(), is("Transformation service of type 'UNKNOWN' is not available."));
    }

    @Test
    public void transformValueThrowsWhenServiceFails() throws Exception {
        when(mapTransformationService.transform("test.map", "input")).thenThrow(new RuntimeException("error"));
        TransformationException exception = assertThrows(TransformationException.class, () -> {
            ItemDisplayStateUtil.transform("MAP", "test.map", "input");
        });
        assertThat(exception.getMessage(), is("Transformation service of type 'MAP' threw an exception: error"));
    }

    @Test
    public void transformStateSucceeds() throws Exception {
        State state = new StringType("ON");
        when(mapTransformationService.transform("test.map", "ON")).thenReturn("Active");
        String result = ItemDisplayStateUtil.transform("MAP", "test.map", "%s", state, zoneId);
        assertThat(result, is("Active"));
    }

    @Test
    public void transformStateFormatsDateTimeWithZone() throws Exception {
        DateTimeType state = new DateTimeType("2023-01-01T10:00:00Z");
        // UTC 10:00 is 11:00 in Europe/Berlin
        when(mapTransformationService.transform("test.map", "11:00")).thenReturn("Eleven");
        String result = ItemDisplayStateUtil.transform("MAP", "test.map", "%1$tH:%1$tM", state, zoneId);
        assertThat(result, is("Eleven"));
    }

    @Test
    public void transformStateUsesDefaultFormatForUnDef() throws Exception {
        State state = UnDefType.NULL;
        when(mapTransformationService.transform("test.map", "NULL")).thenReturn("Missing");
        String result = ItemDisplayStateUtil.transform("MAP", "test.map", "invalid-format", state, zoneId);
        assertThat(result, is("Missing"));
    }

    @Test
    public void transformStateThrowsOnInvalidFormat() {
        State state = new StringType("ON");
        TransformationException exception = assertThrows(TransformationException.class, () -> {
            ItemDisplayStateUtil.transform("MAP", "test.map", "%d", state, zoneId);
        });
        assertThat(exception.getMessage(), is("Cannot format state 'ON' to format '%d'"));
    }
}
