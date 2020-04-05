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
package org.openhab.core.ui.internal.items;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.emf.common.util.BasicEList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.core.ui.items.ItemUIProvider;

/**
 * @author Kai Kreuzer - Initial contribution
 */
public class ItemUIRegistryImplTest {

    // we need to get the decimal separator of the default locale for our tests
    private static final char SEP = (new DecimalFormatSymbols().getDecimalSeparator());

    private ItemUIRegistryImpl uiRegistry;

    @Mock
    private ItemRegistry registry;

    @Mock
    private Widget widget;

    @Mock
    private Item item;

    @Mock
    private UnitProvider unitProvider;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        uiRegistry = new ItemUIRegistryImpl();
        uiRegistry.setItemRegistry(registry);

        when(widget.getItem()).thenReturn("Item");
        when(registry.getItem("Item")).thenReturn(item);

        // Set default time zone to GMT-6
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-6"));
    }

    @Test
    public void getLabelPlainLabel() {
        String testLabel = "This is a plain text";

        when(widget.getLabel()).thenReturn(testLabel);
        String label = uiRegistry.getLabel(widget);
        assertEquals(testLabel, label);
    }

    @Test
    public void getLabelLabelWithStaticValue() {
        String testLabel = "Label [value]";

        when(widget.getLabel()).thenReturn(testLabel);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [value]", label);
    }

    @Test
    public void getLabelLabelWithStringValue() {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelLabelWithStringValueFunction() {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new StringType("foo(x):y"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [foo(x):y]", label);
    }

    @Test
    public void getLabelLabelWithoutPatterAndIntegerValue() {
        String testLabel = "Label";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        when(item.getStateDescription())
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%d").build().toStateDescription());
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [20]", label);
    }

    @Test
    public void getLabelLabelWithoutPatterAndFractionalDigitsValue() {
        String testLabel = "Label";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20.5));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20.5));
        when(item.getStateDescription())
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%d").build().toStateDescription());
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [21]", label);
    }

    @Test
    public void getLabelLabelWithIntegerValue() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [20]", label);
    }

    @Test
    public void getLabelLabelWithFractionalDigitsValue() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20.5));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20.5));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [21]", label);
    }

    @Test
    public void getLabelLabelWithIntegerValueAndWidth() {
        String testLabel = "Label [%3d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [ 20]", label);
    }

    @Test
    public void getLabelLabelWithHexValueAndWidth() {
        String testLabel = "Label [%3x]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [ 14]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(10f / 3f));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + SEP + "333]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnitUpdatedWithQuantityType() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + SEP + "333 °C]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnitUpdatedWithDecimalType() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + SEP + "333]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit2() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 °C]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit3() {
        String testLabel = "Label [%d %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit4() {
        String testLabel = "Label [%.0f %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit5() {
        String testLabel = "Label [%d " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("33 %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [33 %]", label);
    }

    @Test
    public void getLabelLabelWithFractionalDigitsValueAndUnit5() {
        String testLabel = "Label [%d " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit6() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit7() {
        String testLabel = "Label [%d %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("33 %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [33 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnitConversion() {
        String testLabel = "Label [%.2f °F]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("22 °C"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [71" + SEP + "60 °F]", label);
    }

    @Test
    public void getLabelLabelWithPercent() {
        String testLabel = "Label [%.1f %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(10f / 3f));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + SEP + "3 %]", label);
    }

    @Test
    public void getLabelLabelWithPercentType() {
        String testLabel = "Label [%d %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new PercentType(42));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [42 %]", label);
    }

    @Test
    public void getLabelLabelWithDate() {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [01.06.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00Z"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [31.05.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00+02"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [31.05.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00-06"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [01.06.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00-07"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [01.06.2011]", label);
    }

    @Test
    public void getLabelLabelWithZonedDate() throws ItemNotFoundException {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn("Item");
        when(registry.getItem("Item")).thenReturn(item);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00"));
        String label = uiRegistry.getLabel(w);
        assertEquals("Label [01.06.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00Z"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [31.05.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00+02"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [31.05.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00-06"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [01.06.2011]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00-07"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [01.06.2011]", label);
    }

    @Test
    public void getLabelLabelWithTime() {
        String testLabel = "Label [%1$tT]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [15:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59Z"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [09:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59+02"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [07:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59-06"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [15:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59-07"));
        label = uiRegistry.getLabel(widget);
        assertEquals("Label [16:30:59]", label);
    }

    @Test
    public void getLabelLabelWithZonedTime() throws ItemNotFoundException {
        String testLabel = "Label [%1$tT]";

        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn("Item");
        when(registry.getItem("Item")).thenReturn(item);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59"));
        String label = uiRegistry.getLabel(w);
        assertEquals("Label [15:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59Z"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [09:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59+02"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [07:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59-06"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [15:30:59]", label);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59-07"));
        label = uiRegistry.getLabel(w);
        assertEquals("Label [16:30:59]", label);
    }

    @Test
    public void getLabelWidgetWithoutLabelAndItem() {
        Widget w = mock(Widget.class);
        String label = uiRegistry.getLabel(w);
        assertEquals("", label);
    }

    @Test
    public void getLabelWidgetWithoutLabel() {
        String label = uiRegistry.getLabel(widget);
        assertEquals("Item", label);
    }

    @Test
    public void getLabelLabelFromUIProvider() {
        ItemUIProvider provider = mock(ItemUIProvider.class);
        uiRegistry.addItemUIProvider(provider);
        when(provider.getLabel(anyString())).thenReturn("ProviderLabel");
        String label = uiRegistry.getLabel(widget);
        assertEquals("ProviderLabel", label);
        uiRegistry.removeItemUIProvider(provider);
    }

    @Test
    public void getLabelLabelForUndefinedStringItemState() {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedIntegerItemState() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedDecimalItemState() {
        String testLabel = "Label [%.2f]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedDateItemState() {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-.-.-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedQuantityItemState() {
        String testLabel = "Label [%.2f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [- -]", label);
    }

    @Test
    public void getLabelItemNotFound() throws ItemNotFoundException {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(widget.eClass()).thenReturn(SitemapFactory.eINSTANCE.createText().eClass());
        when(registry.getItem("Item")).thenThrow(new ItemNotFoundException("Item"));
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelWithFunctionValue() {
        String testLabel = "Label [MAP(de.map):%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelGroupLabelWithValue() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(OnOffType.ON);
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(5));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [5]", label);
    }

    @Test
    public void getWidgetUnknownPageId() throws ItemNotFoundException {
        Sitemap sitemap = SitemapFactory.eINSTANCE.createSitemap();
        when(registry.getItem("unknown")).thenThrow(new ItemNotFoundException("unknown"));
        Widget w = uiRegistry.getWidget(sitemap, "unknown");
        assertNull(w);
    }

    @Test
    public void testFormatDefault() {
        assertEquals("Server [(-)]", uiRegistry.formatUndefined("Server [(%d)]"));
        assertEquals("Anruf [von - an -]", uiRegistry.formatUndefined("Anruf [von %2$s an %1$s]"));
        assertEquals("Zeit [-.-.- -]", uiRegistry.formatUndefined("Zeit [%1$td.%1$tm.%1$tY %1$tT]"));
        assertEquals("Temperatur [- °C]", uiRegistry.formatUndefined("Temperatur [%.1f °C]"));
        assertEquals("Luftfeuchte [- %]", uiRegistry.formatUndefined("Luftfeuchte [%.1f %%]"));
    }

    @Test
    public void testStateConversionForSwitchWidgetThroughGetState() throws ItemNotFoundException {
        State colorState = new HSBType("23,42,50");

        ColorItem colorItem = new ColorItem("myItem");
        colorItem.setLabel("myItem");
        colorItem.setState(colorState);

        when(registry.getItem("myItem")).thenReturn(colorItem);

        Switch switchWidget = mock(Switch.class);
        when(switchWidget.getItem()).thenReturn("myItem");
        when(switchWidget.getMappings()).thenReturn(new BasicEList<>());

        State stateForSwitch = uiRegistry.getState(switchWidget);

        assertEquals(OnOffType.ON, stateForSwitch);
    }

    @Test
    public void testStateConversionForSwitchWidgetWithMappingThroughGetState() throws ItemNotFoundException {
        State colorState = new HSBType("23,42,50");

        ColorItem colorItem = new ColorItem("myItem");
        colorItem.setLabel("myItem");
        colorItem.setState(colorState);

        when(registry.getItem("myItem")).thenReturn(colorItem);

        Switch switchWidget = mock(Switch.class);
        when(switchWidget.getItem()).thenReturn("myItem");

        Mapping mapping = mock(Mapping.class);
        BasicEList<Mapping> mappings = new BasicEList<>();
        mappings.add(mapping);
        when(switchWidget.getMappings()).thenReturn(mappings);

        State stateForSwitch = uiRegistry.getState(switchWidget);

        assertEquals(colorState, stateForSwitch);
    }

    @Test
    public void testStateConversionForSliderWidgetThroughGetState() throws ItemNotFoundException {
        State colorState = new HSBType("23,42,75");

        ColorItem colorItem = new ColorItem("myItem");
        colorItem.setLabel("myItem");
        colorItem.setState(colorState);

        when(registry.getItem("myItem")).thenReturn(colorItem);

        Slider sliderWidget = mock(Slider.class);
        when(sliderWidget.getItem()).thenReturn("myItem");

        State stateForSlider = uiRegistry.getState(sliderWidget);

        assertTrue(stateForSlider instanceof PercentType);

        PercentType pt = (PercentType) stateForSlider;

        assertEquals(75, pt.longValue());
    }

    @Test
    public void getLabelLabelWithoutStateDescription() {
        String testLabel = "Label";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(null);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label", label);
    }

    @Test
    public void getLabelLabelWithoutPatternInStateDescription() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn(null);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label", label);
    }

    @Test
    public void getLabelLabelWithPatternInStateDescription() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelLabelWithEmptyPattern() {
        String testLabel = "Label []";

        StateDescription stateDescription = mock(StateDescription.class);
        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label", label);
    }

    @Test
    public void getLabelLabelWithMappedOption() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("State0", "This is the state 0"));
        options.add(new StateOption("State1", "This is the state 1"));
        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(options);
        when(item.getState()).thenReturn(new StringType("State1"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [This is the state 1]", label);
    }

    @Test
    public void getLabelLabelWithUnmappedOption() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("State0", "This is the state 0"));
        options.add(new StateOption("State1", "This is the state 1"));
        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(options);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelTransformationContainingPercentS() throws ItemNotFoundException {
        // It doesn't matter that "FOO" doesn't exist - this is to assert it doesn't fail before because of the two "%s"
        String testLabel = "Memory [FOO(echo %s):%s]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn("Item");
        when(registry.getItem("Item")).thenReturn(item);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(w);
        assertEquals("Memory [State]", label);
    }

    @Test
    public void getLabelColorLabelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widget.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("21");
        when(colorArray.getCondition()).thenReturn("<");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        when(widget.getLabelColor()).thenReturn(colorArrays);

        when(item.getState()).thenReturn(new DecimalType(10f / 3f));

        String color = uiRegistry.getLabelColor(widget);
        assertEquals("yellow", color);
    }

    @Test
    public void getLabelColorLabelWithUnitValue() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("20");
        when(colorArray.getCondition()).thenReturn("==");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        when(widget.getLabelColor()).thenReturn(colorArrays);

        when(item.getState()).thenReturn(new QuantityType<>("20 °C"));

        String color = uiRegistry.getLabelColor(widget);
        assertEquals("yellow", color);
    }

}
