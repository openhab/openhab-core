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
package org.openhab.core.ui.internal.items;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.measure.quantity.Temperature;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.CallItem;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.ImageItem;
import org.openhab.core.library.items.LocationItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.PlayerItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.Colorpicker;
import org.openhab.core.model.sitemap.sitemap.Group;
import org.openhab.core.model.sitemap.sitemap.Image;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Mapview;
import org.openhab.core.model.sitemap.sitemap.Selection;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Text;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;
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
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemUIRegistryImplTest {

    // we need to get the decimal separator of the default locale for our tests
    private static final char SEP = (new DecimalFormatSymbols().getDecimalSeparator());
    private static final String ITEM_NAME = "Item";

    private @NonNullByDefault({}) ItemUIRegistryImpl uiRegistry;

    private @Mock @NonNullByDefault({}) ItemRegistry registryMock;
    private @Mock @NonNullByDefault({}) Widget widgetMock;
    private @Mock @NonNullByDefault({}) Item itemMock;

    @BeforeEach
    public void setup() throws Exception {
        uiRegistry = new ItemUIRegistryImpl(registryMock);

        when(widgetMock.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(itemMock);

        // Set default time zone to GMT-6
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-6"));
    }

    @Test
    public void getLabelPlainLabel() {
        String testLabel = "This is a plain text";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals(testLabel, label);
    }

    @Test
    public void getLabelLabelWithStaticValue() {
        String testLabel = "Label [value]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [value]", label);
    }

    @Test
    public void getLabelLabelWithStringValue() {
        String testLabel = "Label [%s]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelLabelWithStringValueFunction() {
        String testLabel = "Label [%s]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new StringType("foo(x):y"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [foo(x):y]", label);
    }

    @Test
    public void getLabelLabelWithoutPatterAndIntegerValue() {
        String testLabel = "Label";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(20));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        when(itemMock.getStateDescription())
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%d").build().toStateDescription());
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [20]", label);
    }

    @Test
    public void getLabelLabelWithoutPatterAndFractionalDigitsValue() {
        String testLabel = "Label";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(20.5));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20.5));
        when(itemMock.getStateDescription())
                .thenReturn(StateDescriptionFragmentBuilder.create().withPattern("%d").build().toStateDescription());
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [21]", label);
    }

    @Test
    public void getLabelLabelWithIntegerValue() {
        String testLabel = "Label [%d]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(20));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [20]", label);
    }

    @Test
    public void getLabelLabelWithFractionalDigitsValue() {
        String testLabel = "Label [%d]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(20.5));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20.5));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [21]", label);
    }

    @Test
    public void getLabelLabelWithIntegerValueAndWidth() {
        String testLabel = "Label [%3d]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(20));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [ 20]", label);
    }

    @Test
    public void getLabelLabelWithHexValueAndWidth() {
        String testLabel = "Label [%3x]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(20));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(20));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [ 14]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(10f / 3f));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3" + SEP + "333]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnitUpdatedWithQuantityType() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3" + SEP + "333 °C]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnitUpdatedWithDecimalType() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3" + SEP + "333]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit2() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 °C]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit3() {
        String testLabel = "Label [%d %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit4() {
        String testLabel = "Label [%.0f %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit5() {
        String testLabel = "Label [%d " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("33 %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [33 %]", label);
    }

    @Test
    public void getLabelLabelWithFractionalDigitsValueAndUnit5() {
        String testLabel = "Label [%d " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit6() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("" + 10f / 3f + " %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit7() {
        String testLabel = "Label [%d %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("33 %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [33 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnitConversion() {
        String testLabel = "Label [%.2f °F]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>("22 °C"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [71" + SEP + "60 °F]", label);
    }

    @Test
    public void getLabelLabelWithPercent() {
        String testLabel = "Label [%.1f %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DecimalType(10f / 3f));
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(10f / 3f));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3" + SEP + "3 %]", label);
    }

    @Test
    public void getLabelLabelWithPercentType() {
        String testLabel = "Label [%d %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new PercentType(42));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [42 %]", label);
    }

    @Test
    public void getLabelLabelWithDate() {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [01.06.2011]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00Z"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [31.05.2011]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00+02"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [31.05.2011]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00-06"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [01.06.2011]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T00:00:00-07"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [01.06.2011]", label);
    }

    @Test
    public void getLabelLabelWithZonedDate() throws ItemNotFoundException {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);
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

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [15:30:59]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59Z"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [09:30:59]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59+02"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [07:30:59]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59-06"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [15:30:59]", label);
        when(itemMock.getState()).thenReturn(new DateTimeType("2011-06-01T15:30:59-07"));
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [16:30:59]", label);
    }

    @Test
    public void getLabelLabelWithZonedTime() throws ItemNotFoundException {
        String testLabel = "Label [%1$tT]";

        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);
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
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals(ITEM_NAME, label);
    }

    @Test
    public void getLabelLabelFromUIProvider() {
        ItemUIProvider provider = mock(ItemUIProvider.class);
        uiRegistry.addItemUIProvider(provider);
        when(provider.getLabel(anyString())).thenReturn("ProviderLabel");
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("ProviderLabel", label);
        uiRegistry.removeItemUIProvider(provider);
    }

    @Test
    public void getLabelLabelForUndefinedStringItemState() {
        String testLabel = "Label [%s]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedIntegerItemState() {
        String testLabel = "Label [%d]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedDecimalItemState() {
        String testLabel = "Label [%.2f]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedDateItemState() {
        String testLabel = "Label [%1$td.%1$tm.%1$tY]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [-.-.-]", label);
    }

    @Test
    public void getLabelLabelForUndefinedQuantityItemState() {
        String testLabel = "Label [%.2f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [- -]", label);
    }

    @Test
    public void getLabelItemNotFound() throws ItemNotFoundException {
        String testLabel = "Label [%s]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(widgetMock.eClass()).thenReturn(SitemapFactory.eINSTANCE.createText().eClass());
        when(registryMock.getItem(ITEM_NAME)).thenThrow(new ItemNotFoundException(ITEM_NAME));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [-]", label);
    }

    @Test
    public void getLabelLabelWithFunctionValue() {
        String testLabel = "Label [MAP(de.map):%s]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelGroupLabelWithValue() {
        String testLabel = "Label [%d]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(OnOffType.ON);
        when(itemMock.getStateAs(DecimalType.class)).thenReturn(new DecimalType(5));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [5]", label);
    }

    @Test
    public void getWidgetUnknownPageId() throws ItemNotFoundException {
        Sitemap sitemap = SitemapFactory.eINSTANCE.createSitemap();
        when(registryMock.getItem("unknown")).thenThrow(new ItemNotFoundException("unknown"));
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

        when(registryMock.getItem("myItem")).thenReturn(colorItem);

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

        when(registryMock.getItem("myItem")).thenReturn(colorItem);

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

        when(registryMock.getItem("myItem")).thenReturn(colorItem);

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

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(null);
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label", label);
    }

    @Test
    public void getLabelLabelWithoutPatternInStateDescription() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn(null);
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label", label);
    }

    @Test
    public void getLabelLabelWithPatternInStateDescription() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelLabelWithEmptyPattern() {
        String testLabel = "Label []";

        StateDescription stateDescription = mock(StateDescription.class);
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label", label);
    }

    @Test
    public void getLabelLabelWithMappedOption() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("State0", "This is the state 0"));
        options.add(new StateOption("State1", "This is the state 1"));
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(options);
        when(itemMock.getState()).thenReturn(new StringType("State1"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [This is the state 1]", label);
    }

    @Test
    public void getLabelLabelWithUnmappedOption() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("State0", "This is the state 0"));
        options.add(new StateOption("State1", "This is the state 1"));
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(options);
        when(itemMock.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [State]", label);
    }

    @Test
    public void getLabelTransformationContainingPercentS() throws ItemNotFoundException {
        // It doesn't matter that "FOO" doesn't exist - this is to assert it doesn't fail before because of the two "%s"
        String testLabel = "Memory [FOO(echo %s):%s]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getState()).thenReturn(new StringType("State"));
        String label = uiRegistry.getLabel(w);
        assertEquals("Memory [State]", label);
    }

    @Test
    public void getLabelColorLabelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widgetMock.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("21");
        when(colorArray.getCondition()).thenReturn("<");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        when(widgetMock.getLabelColor()).thenReturn(colorArrays);

        when(itemMock.getState()).thenReturn(new DecimalType(10f / 3f));

        String color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("yellow", color);
    }

    @Test
    public void getLabelColorLabelWithUnitValue() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("20");
        when(colorArray.getCondition()).thenReturn("==");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        when(widgetMock.getLabelColor()).thenReturn(colorArrays);

        when(itemMock.getState()).thenReturn(new QuantityType<>("20 °C"));

        String color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("yellow", color);
    }

    @Test
    public void getDefaultWidgets() {
        Widget defaultWidget = uiRegistry.getDefaultWidget(GroupItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Group.class)));

        defaultWidget = uiRegistry.getDefaultWidget(CallItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));

        defaultWidget = uiRegistry.getDefaultWidget(ColorItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Colorpicker.class)));

        defaultWidget = uiRegistry.getDefaultWidget(ContactItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));

        defaultWidget = uiRegistry.getDefaultWidget(DateTimeItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));

        defaultWidget = uiRegistry.getDefaultWidget(DimmerItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Slider.class)));
        assertThat(((Slider) defaultWidget).isSwitchEnabled(), is(true));

        defaultWidget = uiRegistry.getDefaultWidget(ImageItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Image.class)));

        defaultWidget = uiRegistry.getDefaultWidget(LocationItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Mapview.class)));

        defaultWidget = uiRegistry.getDefaultWidget(PlayerItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));
        assertThat(((Switch) defaultWidget).getMappings(), hasSize(4));

        defaultWidget = uiRegistry.getDefaultWidget(RollershutterItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));

        defaultWidget = uiRegistry.getDefaultWidget(SwitchItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));
    }

    @Test
    public void getDefaultWidgetsForNumberItem() {
        // NumberItem without CommandOptions or StateOptions should return Text element
        Widget defaultWidget = uiRegistry.getDefaultWidget(NumberItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));

        // NumberItem with one to four CommandOptions should return Switch element
        final CommandDescriptionBuilder builder = CommandDescriptionBuilder.create().withCommandOptions(
                List.of(new CommandOption("command1", "label1"), new CommandOption("command2", "label2"),
                        new CommandOption("command3", "label3"), new CommandOption("command4", "label4")));
        when(itemMock.getCommandDescription()).thenReturn(builder.build());
        defaultWidget = uiRegistry.getDefaultWidget(NumberItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));

        // NumberItem with more than four CommandOptions should return Selection element
        builder.withCommandOption(new CommandOption("command5", "label5"));
        when(itemMock.getCommandDescription()).thenReturn(builder.build());
        defaultWidget = uiRegistry.getDefaultWidget(NumberItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Selection.class)));

        // NumberItem with one or more StateOptions should return Selection element
        when(itemMock.getStateDescription()).thenReturn(StateDescriptionFragmentBuilder.create()
                .withOptions(List.of(new StateOption("value1", "label1"), new StateOption("value2", "label2"))).build()
                .toStateDescription());
        defaultWidget = uiRegistry.getDefaultWidget(NumberItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Selection.class)));

        // Read-only NumberItem with one or more StateOptions should return Text element
        when(itemMock.getStateDescription())
                .thenReturn(StateDescriptionFragmentBuilder.create().withReadOnly(Boolean.TRUE)
                        .withOptions(List.of(new StateOption("value1", "label1"), new StateOption("value2", "label2")))
                        .build().toStateDescription());
        defaultWidget = uiRegistry.getDefaultWidget(NumberItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));
    }

    @Test
    public void getDefaultWidgetsForStringItem() {
        // StringItem without CommandOptions or StateOptions should return Text element
        Widget defaultWidget = uiRegistry.getDefaultWidget(StringItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));

        // StringItem with one to four CommandOptions should return Switch element
        final CommandDescriptionBuilder builder = CommandDescriptionBuilder.create().withCommandOptions(
                List.of(new CommandOption("command1", "label1"), new CommandOption("command2", "label2"),
                        new CommandOption("command3", "label3"), new CommandOption("command4", "label4")));
        when(itemMock.getCommandDescription()).thenReturn(builder.build());
        defaultWidget = uiRegistry.getDefaultWidget(StringItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));

        // StringItem with more than four CommandOptions should return Selection element
        builder.withCommandOption(new CommandOption("command5", "label5"));
        when(itemMock.getCommandDescription()).thenReturn(builder.build());
        defaultWidget = uiRegistry.getDefaultWidget(StringItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Selection.class)));

        // StringItem with one or more StateOptions should return Selection element
        when(itemMock.getStateDescription()).thenReturn(StateDescriptionFragmentBuilder.create()
                .withOptions(List.of(new StateOption("value1", "label1"), new StateOption("value2", "label2"))).build()
                .toStateDescription());
        defaultWidget = uiRegistry.getDefaultWidget(StringItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Selection.class)));

        // Read-only StringItem with one or more StateOptions should return Text element
        when(itemMock.getStateDescription())
                .thenReturn(StateDescriptionFragmentBuilder.create().withReadOnly(Boolean.TRUE)
                        .withOptions(List.of(new StateOption("value1", "label1"), new StateOption("value2", "label2")))
                        .build().toStateDescription());
        defaultWidget = uiRegistry.getDefaultWidget(StringItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Text.class)));
    }

    @Test
    public void getUnitForWidgetForNonNumberItem() throws Exception {
        String unit = uiRegistry.getUnitForWidget(widgetMock);

        assertThat(unit, is(""));
    }

    @Test
    public void getUnitForWidgetWithWidgetLabel() throws Exception {
        // a NumberItem having a Dimension must be returned
        NumberItem item = mock(NumberItem.class);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);

        doReturn(Temperature.class).when(item).getDimension();

        // we set the Label on the widget itself
        when(widgetMock.getLabel()).thenReturn("Label [%.1f °C]");

        String unit = uiRegistry.getUnitForWidget(widgetMock);

        assertThat(unit, is(equalTo("°C")));
    }

    @Test
    public void getUnitForWidgetWithItemLabelAndWithoutWidgetLabel() throws Exception {
        // a NumberItem having a Dimension must be returned
        NumberItem item = mock(NumberItem.class);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);

        doReturn(Temperature.class).when(item).getDimension();

        // we set the UnitSymbol on the item, this must be used as a fallback if no Widget label was used
        when(item.getUnitSymbol()).thenReturn("°C");

        String unit = uiRegistry.getUnitForWidget(widgetMock);

        assertThat(unit, is(equalTo("°C")));
    }

    @Test
    public void getLabelColorDefaultColor() {
        String testLabel = "Label [%.3f]";

        when(widgetMock.getLabel()).thenReturn(testLabel);

        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("21");
        when(colorArray.getCondition()).thenReturn("<");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        ColorArray colorArray2 = mock(ColorArray.class);
        when(colorArray2.getState()).thenReturn(null);
        when(colorArray2.getCondition()).thenReturn(null);
        when(colorArray2.getArg()).thenReturn("blue");
        colorArrays.add(colorArray2);
        when(widgetMock.getLabelColor()).thenReturn(colorArrays);

        when(itemMock.getState()).thenReturn(new DecimalType(21.0));

        String color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("blue", color);
    }

    @Test
    public void getValueColor() {
        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("21");
        when(colorArray.getCondition()).thenReturn("<");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        ColorArray colorArray2 = mock(ColorArray.class);
        when(colorArray2.getState()).thenReturn("24");
        when(colorArray2.getCondition()).thenReturn("<");
        when(colorArray2.getArg()).thenReturn("red");
        colorArrays.add(colorArray2);
        ColorArray colorArray3 = mock(ColorArray.class);
        when(colorArray3.getState()).thenReturn(null);
        when(colorArray3.getCondition()).thenReturn(null);
        when(colorArray3.getArg()).thenReturn("blue");
        colorArrays.add(colorArray3);
        when(widgetMock.getValueColor()).thenReturn(colorArrays);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        String color = uiRegistry.getValueColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("red", color);

        when(itemMock.getState()).thenReturn(new DecimalType(30.0));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("blue", color);
    }

    @Test
    public void getIconColor() {
        ColorArray colorArray = mock(ColorArray.class);
        when(colorArray.getState()).thenReturn("21");
        when(colorArray.getCondition()).thenReturn("<");
        when(colorArray.getArg()).thenReturn("yellow");
        BasicEList<ColorArray> colorArrays = new BasicEList<>();
        colorArrays.add(colorArray);
        ColorArray colorArray2 = mock(ColorArray.class);
        when(colorArray2.getState()).thenReturn("24");
        when(colorArray2.getCondition()).thenReturn("<");
        when(colorArray2.getArg()).thenReturn("red");
        colorArrays.add(colorArray2);
        ColorArray colorArray3 = mock(ColorArray.class);
        when(colorArray3.getState()).thenReturn(null);
        when(colorArray3.getCondition()).thenReturn(null);
        when(colorArray3.getArg()).thenReturn("blue");
        colorArrays.add(colorArray3);
        when(widgetMock.getIconColor()).thenReturn(colorArrays);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        String color = uiRegistry.getIconColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("red", color);

        when(itemMock.getState()).thenReturn(new DecimalType(30.0));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("blue", color);
    }
}
