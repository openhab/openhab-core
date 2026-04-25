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
    public void getDisplayStateReturnsUnDefStateAsString() {
        State state = UnDefType.NULL;
        when(stateDescription.getPattern()).thenReturn("%s");

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("NULL"));
    }

    @Test
    public void getDisplayStateAppliesTransformation() throws Exception {
        State state = new StringType("ON");
        when(stateDescription.getPattern()).thenReturn("MAP(test.map):%s");
        when(mapTransformationService.transform("test.map", "ON")).thenReturn("Active");

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("Active"));
    }

    @Test
    public void getDisplayStateReturnsRawStateWhenTransformationReturnsNull() throws Exception {
        State state = new StringType("ON");
        when(stateDescription.getPattern()).thenReturn("MAP(test.map):%s");
        when(mapTransformationService.transform("test.map", "ON")).thenReturn(null);

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("ON"));
    }

    @Test
    public void getDisplayStateReturnsRawStateWhenTransformationServiceNotAvailable() {
        State state = new StringType("ON");
        when(stateDescription.getPattern()).thenReturn("UNKNOWN(test.map):%s");

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("ON"));
    }

    @Test
    public void getDisplayStateAppliesStateOptionLabel() {
        State state = new StringType("1");

        StateOption option = new StateOption("1", "Option Label");

        when(stateDescription.getOptions()).thenReturn(List.of(option));
        when(stateDescription.getPattern()).thenReturn("Prefix: %s");

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("Prefix: Option Label"));
    }

    @Test
    public void getDisplayStateAppliesStateOptionLabelWithoutPattern() {
        State state = new StringType("1");

        StateOption option = new StateOption("1", "Option Label");

        when(stateDescription.getOptions()).thenReturn(List.of(option));
        when(stateDescription.getPattern()).thenReturn(null);

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("Option Label"));
    }

    @Test
    public void getDisplayStateReturnsLabelWhenOptionPatternIsInvalid() {
        State state = new StringType("1");

        StateOption option = new StateOption("1", "Option Label");

        when(stateDescription.getOptions()).thenReturn(List.of(option));
        when(stateDescription.getPattern()).thenReturn("%d"); // Label is String, %d will fail

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("Option Label"));
    }

    @Test
    public void getDisplayStateConvertsQuantityTypeToPatternUnit() {
        QuantityType<?> state = new QuantityType<>("20 °C");
        when(stateDescription.getPattern()).thenReturn("%.1f °F");
        when(stateDescription.getOptions()).thenReturn(Collections.emptyList());

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        // 20°C = 68°F
        assertThat(result, is("68.0 °F"));
    }

    @Test
    public void getDisplayStateFormatsDateTimeTypeWithZoneId() {
        DateTimeType state = new DateTimeType("2023-01-01T10:00:00Z");
        when(stateDescription.getPattern()).thenReturn("%1$tH:%1$tM");
        when(stateDescription.getOptions()).thenReturn(Collections.emptyList());

        // UTC 10:00 is 11:00 in Europe/Berlin (CET)
        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("11:00"));
    }

    @Test
    public void getDisplayStateFormatsStateWithRegularPattern() {
        State state = new StringType("test");
        when(stateDescription.getPattern()).thenReturn("Result: %s");
        when(stateDescription.getOptions()).thenReturn(Collections.emptyList());

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("Result: test"));
    }

    @Test
    public void getDisplayStateReturnsRawStateWhenPatternIsInvalid() {
        State state = new StringType("test");
        when(stateDescription.getPattern()).thenReturn("%d"); // String cannot be formatted as %d
        when(stateDescription.getOptions()).thenReturn(Collections.emptyList());

        String result = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        assertThat(result, is("test"));
    }
}
