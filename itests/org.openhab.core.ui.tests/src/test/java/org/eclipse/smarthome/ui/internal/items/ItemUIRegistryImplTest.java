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
package org.eclipse.smarthome.ui.internal.items;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.core.types.util.UnitUtils;
import org.eclipse.smarthome.model.sitemap.ColorArray;
import org.eclipse.smarthome.model.sitemap.Mapping;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapFactory;
import org.eclipse.smarthome.model.sitemap.Slider;
import org.eclipse.smarthome.model.sitemap.Switch;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.items.ItemUIProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ItemUIRegistryImplTest {

    static private ItemUIRegistryImpl uiRegistry;
    // we need to get the decimal separator of the default locale for our tests
    static private final char sep = (new DecimalFormatSymbols().getDecimalSeparator());

    @Mock
    static private ItemRegistry registry;

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
    }

    @Test
    public void getLabel_plainLabel() {
        String testLabel = "This is a plain text";

        when(widget.getLabel()).thenReturn(testLabel);
        String label = uiRegistry.getLabel(widget);
        assertEquals(testLabel, label);
    }

    @Test
    public void getLabel_labelWithStaticValue() {
        String testLabel = "Label [value]";

        when(widget.getLabel()).thenReturn(testLabel);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [value]", label);
    }

    @Test
    public void getLabel_labelWithStringValue() {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabel_labelWithIntegerValue() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [20]", label);
    }

    @Test
    public void getLabel_labelWithIntegerValueAndWidth() {
        String testLabel = "Label [%3d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [ 20]", label);
    }

    @Test
    public void getLabel_labelWithHexValueAndWidth() {
        String testLabel = "Label [%3x]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [ 14]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(10f / 3f));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + sep + "333]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + sep + "333 °C]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit2() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 °C]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit3() {
        String testLabel = "Label [%d %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit4() {
        String testLabel = "Label [%.0f %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit5() {
        String testLabel = "Label [%d " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("33 %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [33 %]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit6() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnit7() {
        String testLabel = "Label [%d %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("33 %"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [33 %]", label);
    }

    @Test
    public void getLabel_labelWithDecimalValueAndUnitConversion() {
        String testLabel = "Label [%.2f °F]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new QuantityType<>("22 °C"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [71" + sep + "60 °F]", label);
    }

    @Test
    public void getLabel_labelWithPercent() {
        String testLabel = "Label [%.1f %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DecimalType(10f / 3f));
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [3" + sep + "3 %]", label);
    }

    @Test
    public void getLabel_labelWithPercentType() {
        String testLabel = "Label [%d %%]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new PercentType(42));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [42 %]", label);
    }

    @Test
    public void getLabel_labelWithDate() {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [01.06.2011]", label);
    }

    @Test
    public void getLabel_labelWithZonedDate() throws ItemNotFoundException {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn("Item");
        when(registry.getItem("Item")).thenReturn(item);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00Z"));
        String label = uiRegistry.getLabel(w);
        assertEquals("Label [01.06.2011]", label);
    }

    @Test
    public void getLabel_labelWithTime() {
        String testLabel = "Label [%1$tT]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [15:30:59]", label);
    }

    @Test
    public void getLabel_labelWithZonedTime() throws ItemNotFoundException {
        String testLabel = "Label [%1$tT]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn("Item");
        when(registry.getItem("Item")).thenReturn(item);
        when(item.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59Z"));

        String label = uiRegistry.getLabel(w);
        assertEquals("Label [15:30:59]", label);
    }

    @Test
    public void getLabel_widgetWithoutLabelAndItem() {
        Widget w = mock(Widget.class);
        String label = uiRegistry.getLabel(w);
        assertEquals("", label);
    }

    @Test
    public void getLabel_widgetWithoutLabel() {
        String label = uiRegistry.getLabel(widget);
        assertEquals("Item", label);
    }

    @Test
    public void getLabel_labelFromUIProvider() {

        ItemUIProvider provider = mock(ItemUIProvider.class);
        uiRegistry.addItemUIProvider(provider);
        when(provider.getLabel(anyString())).thenReturn("ProviderLabel");
        String label = uiRegistry.getLabel(widget);
        assertEquals("ProviderLabel", label);
        uiRegistry.removeItemUIProvider(provider);
    }

    @Test
    public void getLabel_labelForUndefinedStringItemState() {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabel_labelForUndefinedIntegerItemState() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabel_labelForUndefinedDecimalItemState() {
        String testLabel = "Label [%.2f]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabel_labelForUndefinedDateItemState() {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-.-.-]", label);
    }

    @Test
    public void getLabel_labelForUndefinedQuantityItemState() {
        String testLabel = "Label [%.2f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [- -]", label);
    }

    @Test
    public void getLabel_itemNotFound() throws ItemNotFoundException {
        String testLabel = "Label [%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(widget.eClass()).thenReturn(SitemapFactory.eINSTANCE.createText().eClass());
        when(registry.getItem("Item")).thenThrow(new ItemNotFoundException("Item"));
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabel_labelWithFunctionValue() {
        String testLabel = "Label [MAP(de.map):%s]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabel_groupLabelWithValue() {
        String testLabel = "Label [%d]";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getState()).thenReturn(OnOffType.ON);
        when(item.getStateAs(DecimalType.class)).thenReturn(new DecimalType(5));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label [5]", label);
    }

    @Test
    public void getWidget_UnknownPageId() throws ItemNotFoundException {
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
        when(switchWidget.getMappings()).thenReturn(new BasicEList<Mapping>());

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
        BasicEList<Mapping> mappings = new BasicEList<Mapping>();
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
    public void getLabel_labelWithoutStateDescription() {
        String testLabel = "Label";

        when(widget.getLabel()).thenReturn(testLabel);
        when(item.getStateDescription()).thenReturn(null);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widget);
        assertEquals("Label", label);
    }

    @Test
    public void getLabel_labelWithoutPatternInStateDescription() {
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
    public void getLabel_labelWithPatternInStateDescription() {
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
    public void getLabel_labelWithEmptyPattern() {
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
    public void getLabel_labelWithMappedOption() {
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
    public void getLabel_labelWithUnmappedOption() {
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
    public void getLabel_transformationContainingPercentS() throws ItemNotFoundException {
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
    public void getLabelColor_labelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widget.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("21");
        when(colorArray.getCondition()).thenReturn("<");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<ColorArray>();
        colorArrays.add(colorArray);
        when(widget.getLabelColor()).thenReturn(colorArrays);

        when(item.getState()).thenReturn(new DecimalType(10f / 3f));

        String color = uiRegistry.getLabelColor(widget);
        assertEquals("yellow", color);
    }

    @Test
    public void getLabelColor_labelWithUnitValue() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widget.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("20");
        when(colorArray.getCondition()).thenReturn("==");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<ColorArray>();
        colorArrays.add(colorArray);
        when(widget.getLabelColor()).thenReturn(colorArrays);

        when(item.getState()).thenReturn(new QuantityType<>("20 °C"));

        String color = uiRegistry.getLabelColor(widget);
        assertEquals("yellow", color);
    }

}
