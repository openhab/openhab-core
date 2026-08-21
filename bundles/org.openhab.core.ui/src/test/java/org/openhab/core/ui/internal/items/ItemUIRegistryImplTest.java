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
package org.openhab.core.ui.internal.items;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
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
import org.openhab.core.sitemap.Button;
import org.openhab.core.sitemap.Buttongrid;
import org.openhab.core.sitemap.Chart;
import org.openhab.core.sitemap.Colorpicker;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Frame;
import org.openhab.core.sitemap.Group;
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Mapping;
import org.openhab.core.sitemap.Mapview;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Selection;
import org.openhab.core.sitemap.Setpoint;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Slider;
import org.openhab.core.sitemap.Switch;
import org.openhab.core.sitemap.Text;
import org.openhab.core.sitemap.Webview;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.core.ui.items.ItemUIProvider;
import org.openhab.core.ui.items.ItemUIRegistry.WidgetLabelSource;

/**
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Tests updated to consider multiple AND conditions + tests added for getVisiblity
 * @author Laurent Garnier - Tests added for getCategory
 * @author Mark Herwege - Implement sitemap registry
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemUIRegistryImplTest {

    @Nullable
    private static TimeZone initialTimeZone;

    // we need to get the decimal separator of the default locale for our tests
    private static final char SEP = (new DecimalFormatSymbols().getDecimalSeparator());
    private static final String ITEM_NAME = "Item";
    private static final String GROUP_ITEM_NAME = "GroupItem";
    private static final String SITEMAP_NAME = "Sitemap";
    private static final String DEFAULT_TIME_ZONE = "GMT-6";

    private @NonNullByDefault({}) ItemUIRegistryImpl uiRegistry;

    private @Mock @NonNullByDefault({}) ItemRegistry registryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @Mock @NonNullByDefault({}) SitemapFactory sitemapFactoryMock;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProviderMock;
    private @Mock @NonNullByDefault({}) Sitemap sitemapMock;
    private @Mock @NonNullByDefault({}) Widget widgetMock;
    private @Mock @NonNullByDefault({}) Item itemMock;

    private @Mock @NonNullByDefault({}) GroupItem groupItemMock;
    private @Mock @NonNullByDefault({}) SwitchItem switchItemMock; // Will have Switch as default widget
    private @Mock @NonNullByDefault({}) DimmerItem dimmerItemMock; // Will have Slider as default widget
    private @Mock @NonNullByDefault({}) ContactItem contactItemMock; // Will have Text as default widget

    @BeforeAll
    public static void setUpClass() {
        initialTimeZone = TimeZone.getDefault();
    }

    @AfterAll
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public static void tearDownClass() {
        // Set the default time zone to its initial value.
        TimeZone.setDefault(initialTimeZone);
    }

    private @Mock @NonNullByDefault({}) Frame frameMock;
    private @Mock @NonNullByDefault({}) Group groupMock;
    private @Mock @NonNullByDefault({}) Text textMock;
    private @Mock @NonNullByDefault({}) Colorpicker colorpickerMock;
    private @Mock @NonNullByDefault({}) Image imageMock;
    private @Mock @NonNullByDefault({}) Mapview mapviewMock;
    private @Mock @NonNullByDefault({}) Slider sliderMock;
    private @Mock @NonNullByDefault({}) Switch switchMock;
    private @Mock @NonNullByDefault({}) Selection selectionMock;
    private @Mock @NonNullByDefault({}) Setpoint setpointMock;
    private @Mock @NonNullByDefault({}) Chart chartMock;
    private @Mock @NonNullByDefault({}) Webview webviewMock;
    private @Mock @NonNullByDefault({}) Buttongrid buttongridMock;
    private @Mock @NonNullByDefault({}) Button buttonMock;
    private @Mock @NonNullByDefault({}) Mapping mappingMock;

    @BeforeEach
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public void setup() throws Exception {
        uiRegistry = spy(
                new ItemUIRegistryImpl(registryMock, metadataRegistryMock, sitemapFactoryMock, timeZoneProviderMock));

        when(widgetMock.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(itemMock);
        when(timeZoneProviderMock.getTimeZone()).thenReturn(ZoneId.of(DEFAULT_TIME_ZONE));

        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));

        setupSitemapFactoryMock();
        setupGroupItemMock();
    }

    private void setupSitemapFactoryMock() {
        when(sitemapFactoryMock.createWidget("Group")).thenReturn(groupMock);
        when(sitemapFactoryMock.createWidget("Text")).thenReturn(textMock);
        when(sitemapFactoryMock.createWidget("Colorpicker")).thenReturn(colorpickerMock);
        when(sitemapFactoryMock.createWidget("Image")).thenReturn(imageMock);
        when(sitemapFactoryMock.createWidget("Mapview")).thenReturn(mapviewMock);
        when(sitemapFactoryMock.createWidget("Slider")).thenReturn(sliderMock);
        when(sitemapFactoryMock.createWidget("Switch")).thenReturn(switchMock);
        when(sitemapFactoryMock.createWidget("Selection")).thenReturn(selectionMock);

        when(sitemapFactoryMock.createMapping()).thenReturn(mappingMock);
    }

    private void setupGroupItemMock() throws Exception {
        when(switchItemMock.getName()).thenReturn("Zebra");
        when(switchItemMock.getLabel()).thenReturn("Alpha");
        when(switchMock.getItem()).thenReturn("Zebra");
        when(dimmerItemMock.getName()).thenReturn("Alpha");
        when(dimmerItemMock.getLabel()).thenReturn("Zebra");
        when(sliderMock.getItem()).thenReturn("Alpha");
        when(contactItemMock.getName()).thenReturn("Mango"); // No label
        when(textMock.getItem()).thenReturn("Mango");
        when(groupItemMock.getMembers()).thenReturn(Set.of(switchItemMock, dimmerItemMock, contactItemMock));
        when(registryMock.getItem(GROUP_ITEM_NAME)).thenReturn(groupItemMock);
        when(groupMock.getItem()).thenReturn(GROUP_ITEM_NAME);
    }

    @Test
    public void getLabelPlainLabel() {
        String testLabel = "This is a plain text";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        assertEquals(testLabel, uiRegistry.getLabel(widgetMock));
        assertEquals(WidgetLabelSource.SITEMAP_WIDGET, uiRegistry.getLabelSource(widgetMock));
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
        when(itemMock.getState()).thenReturn(new QuantityType<>(10f / 3f + " °C"));
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
        when(itemMock.getState()).thenReturn(new QuantityType<>(10f / 3f + " °C"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 °C]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit3() {
        String testLabel = "Label [%d %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>(10f / 3f + " %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit4() {
        String testLabel = "Label [%.0f %%]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>(10f / 3f + " %"));
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
        when(itemMock.getState()).thenReturn(new QuantityType<>(10f / 3f + " %"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [3 %]", label);
    }

    @Test
    public void getLabelLabelWithDecimalValueAndUnit6() {
        String testLabel = "Label [%.0f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getState()).thenReturn(new QuantityType<>(10f / 3f + " %"));
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
        assertEquals("", uiRegistry.getLabel(w));
        assertEquals(WidgetLabelSource.NONE, uiRegistry.getLabelSource(w));
    }

    @Test
    public void getLabelWidgetWithoutLabel() {
        assertEquals(ITEM_NAME, uiRegistry.getLabel(widgetMock));
        assertEquals(WidgetLabelSource.ITEM_NAME, uiRegistry.getLabelSource(widgetMock));
    }

    @Test
    public void getLabelLabelFromUIProvider() {
        ItemUIProvider provider = mock(ItemUIProvider.class);
        uiRegistry.addItemUIProvider(provider);
        when(provider.getLabel(anyString())).thenReturn("ProviderLabel");
        assertEquals("ProviderLabel", uiRegistry.getLabel(widgetMock));
        assertEquals(WidgetLabelSource.ITEM_LABEL, uiRegistry.getLabelSource(widgetMock));
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
        when(widgetMock.getWidgetType()).thenReturn("Text");
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
        when(sitemapFactoryMock.createSitemap(SITEMAP_NAME)).thenReturn(sitemapMock);
        when(sitemapMock.getWidgets()).thenReturn(List.of());

        Sitemap sitemap = sitemapFactoryMock.createSitemap(SITEMAP_NAME);
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
        when(switchWidget.getMappings()).thenReturn(new ArrayList<>());

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
        List<Mapping> mappings = new ArrayList<>();
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

        assertInstanceOf(PercentType.class, stateForSlider);

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
    public void getLabelStringItemLabelWithMappedOption() {
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
    public void getLabelStringItemLabelWithUnmappedOption() {
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
    public void getLabelStringItemLabelWithMappedOptionButInappropriatePattern() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("State0", "This is the state 0"));
        options.add(new StateOption("State1", "This is the state 1"));
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("Value: %f");
        when(stateDescription.getOptions()).thenReturn(options);
        when(itemMock.getState()).thenReturn(new StringType("State0"));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [This is the state 0]", label);
    }

    @Test
    public void getLabelNumberItemLabelWithMappedOption() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("0", "This is the state number 0"));
        options.add(new StateOption("1", "This is the state number 1"));
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(options);
        when(itemMock.getState()).thenReturn(new DecimalType(1));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [This is the state number 1]", label);
    }

    @Test
    public void getLabelNumberItemLabelWithUnmappedOption() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("0", "This is the state number 0"));
        options.add(new StateOption("1", "This is the state number 1"));
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(options);
        when(itemMock.getState()).thenReturn(new DecimalType(2));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [2]", label);
    }

    @Test
    public void getLabelNumberItemLabelWithMappedOptionButInappropriatePattern() {
        String testLabel = "Label";

        StateDescription stateDescription = mock(StateDescription.class);
        List<StateOption> options = new ArrayList<>();
        options.add(new StateOption("0", "This is the state number 0"));
        options.add(new StateOption("1", "This is the state number 1"));
        when(widgetMock.getLabel()).thenReturn(testLabel);
        when(itemMock.getStateDescription()).thenReturn(stateDescription);
        when(stateDescription.getPattern()).thenReturn("Value: %f");
        when(stateDescription.getOptions()).thenReturn(options);
        when(itemMock.getState()).thenReturn(new DecimalType(0));
        String label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [This is the state number 0]", label);

        when(stateDescription.getPattern()).thenReturn("Value: %d");
        label = uiRegistry.getLabel(widgetMock);
        assertEquals("Label [This is the state number 0]", label);
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
    public void getLabelFailingTransformation() throws ItemNotFoundException {
        String testLabel = "Memory [FOO(echo %s):__%d__]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getState()).thenReturn(new DecimalType(11));
        String label = uiRegistry.getLabel(w);
        assertEquals("Memory [11]", label);
    }

    @Test
    public void getLabelFailingTransformationWithNullState() throws ItemNotFoundException {
        String testLabel = "Memory [FOO(echo %s):__%d__]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getState()).thenReturn(UnDefType.NULL);
        String label = uiRegistry.getLabel(w);
        assertEquals("Memory [-]", label);
    }

    @Test
    public void getLabelFailingTransformationWithUndefState() throws ItemNotFoundException {
        String testLabel = "Memory [FOO(echo %s):__%d__]";
        Widget w = mock(Widget.class);
        Item item = mock(Item.class);
        when(w.getLabel()).thenReturn(testLabel);
        when(w.getItem()).thenReturn(ITEM_NAME);
        when(registryMock.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getState()).thenReturn(UnDefType.UNDEF);
        String label = uiRegistry.getLabel(w);
        assertEquals("Memory [-]", label);
    }

    @Test
    public void getLabelColorLabelWithDecimalValue() {
        String testLabel = "Label [%.3f]";

        when(widgetMock.getLabel()).thenReturn(testLabel);

        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("21");
        when(condition.getCondition()).thenReturn("<");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn("yellow");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        when(widgetMock.getLabelColor()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new DecimalType(10f / 3f));

        String color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new DecimalType(21f));

        color = uiRegistry.getLabelColor(widgetMock);
        assertNull(color);
    }

    @Test
    public void getLabelColorLabelWithUnitValue() {
        String testLabel = "Label [%.3f " + UnitUtils.UNIT_PLACEHOLDER + "]";

        when(widgetMock.getLabel()).thenReturn(testLabel);

        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("20");
        when(condition.getCondition()).thenReturn("==");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn("yellow");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        when(widgetMock.getLabelColor()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new QuantityType<>("20 °C"));

        String color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new QuantityType<>("20.1 °C"));

        color = uiRegistry.getLabelColor(widgetMock);
        assertNull(color);
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
        verify(sliderMock).setSwitchEnabled(true);
        verify(sliderMock).setReleaseOnly(true);

        defaultWidget = uiRegistry.getDefaultWidget(ImageItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Image.class)));

        defaultWidget = uiRegistry.getDefaultWidget(LocationItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Mapview.class)));

        defaultWidget = uiRegistry.getDefaultWidget(RollershutterItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));

        defaultWidget = uiRegistry.getDefaultWidget(SwitchItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));

        when(switchMock.getMappings()).thenReturn(new ArrayList<Mapping>());
        defaultWidget = uiRegistry.getDefaultWidget(PlayerItem.class, ITEM_NAME);
        assertThat(defaultWidget, is(instanceOf(Switch.class)));
        assertThat(((Switch) defaultWidget).getMappings(), hasSize(4));
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

        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("18");
        when(condition.getCondition()).thenReturn(">=");
        Condition condition2 = mock(Condition.class);
        when(condition2.getValue()).thenReturn("21");
        when(condition2.getCondition()).thenReturn("<");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        conditions.add(condition2);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn("yellow");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        Condition condition3 = mock(Condition.class);
        when(condition3.getValue()).thenReturn("21");
        when(condition3.getCondition()).thenReturn(">=");
        Condition condition4 = mock(Condition.class);
        when(condition4.getValue()).thenReturn("24");
        when(condition4.getCondition()).thenReturn("<");
        List<Condition> conditions2 = new ArrayList<>();
        conditions2.add(condition3);
        conditions2.add(condition4);
        Rule rule2 = mock(Rule.class);
        when(rule2.getConditions()).thenReturn(conditions2);
        when(rule2.getArgument()).thenReturn("red");
        rules.add(rule2);
        List<Condition> conditions5 = new ArrayList<>();
        Rule rule3 = mock(Rule.class);
        when(rule3.getConditions()).thenReturn(conditions5);
        when(rule3.getArgument()).thenReturn("blue");
        rules.add(rule3);
        when(widgetMock.getLabelColor()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        String color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("red", color);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(17.5));

        color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(17.5));

        color = uiRegistry.getLabelColor(widgetMock);
        assertEquals("blue", color);
    }

    @Test
    public void getValueColor() {
        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("18");
        when(condition.getCondition()).thenReturn(">=");
        Condition condition2 = mock(Condition.class);
        when(condition2.getValue()).thenReturn("21");
        when(condition2.getCondition()).thenReturn("<");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        conditions.add(condition2);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn("yellow");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        Condition condition3 = mock(Condition.class);
        when(condition3.getValue()).thenReturn("21");
        when(condition3.getCondition()).thenReturn(">=");
        Condition condition4 = mock(Condition.class);
        when(condition4.getValue()).thenReturn("24");
        when(condition4.getCondition()).thenReturn("<");
        List<Condition> conditions2 = new ArrayList<>();
        conditions2.add(condition3);
        conditions2.add(condition4);
        Rule rule2 = mock(Rule.class);
        when(rule2.getConditions()).thenReturn(conditions2);
        when(rule2.getArgument()).thenReturn("red");
        rules.add(rule2);
        List<Condition> conditions5 = new ArrayList<>();
        Rule rule3 = mock(Rule.class);
        when(rule3.getConditions()).thenReturn(conditions5);
        when(rule3.getArgument()).thenReturn("blue");
        rules.add(rule3);
        when(widgetMock.getValueColor()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        String color = uiRegistry.getValueColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("red", color);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(17.5));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(17.5));

        color = uiRegistry.getValueColor(widgetMock);
        assertEquals("blue", color);
    }

    @Test
    public void getIconColor() {
        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("18");
        when(condition.getCondition()).thenReturn(">=");
        Condition condition2 = mock(Condition.class);
        when(condition2.getValue()).thenReturn("21");
        when(condition2.getCondition()).thenReturn("<");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        conditions.add(condition2);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn("yellow");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        Condition condition3 = mock(Condition.class);
        when(condition3.getValue()).thenReturn("21");
        when(condition3.getCondition()).thenReturn(">=");
        Condition condition4 = mock(Condition.class);
        when(condition4.getValue()).thenReturn("24");
        when(condition4.getCondition()).thenReturn("<");
        List<Condition> conditions2 = new ArrayList<>();
        conditions2.add(condition3);
        conditions2.add(condition4);
        Rule rule2 = mock(Rule.class);
        when(rule2.getConditions()).thenReturn(conditions2);
        when(rule2.getArgument()).thenReturn("red");
        rules.add(rule2);
        List<Condition> conditions5 = new ArrayList<>();
        Rule rule3 = mock(Rule.class);
        when(rule3.getConditions()).thenReturn(conditions5);
        when(rule3.getArgument()).thenReturn("blue");
        rules.add(rule3);
        when(widgetMock.getIconColor()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        String color = uiRegistry.getIconColor(widgetMock);
        assertEquals("yellow", color);

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("red", color);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(17.5));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("blue", color);

        when(itemMock.getState()).thenReturn(new DecimalType(17.5));

        color = uiRegistry.getIconColor(widgetMock);
        assertEquals("blue", color);
    }

    @Test
    public void getVisibility() {
        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("21");
        when(condition.getCondition()).thenReturn(">=");
        Condition condition2 = mock(Condition.class);
        when(condition2.getValue()).thenReturn("24");
        when(condition2.getCondition()).thenReturn("<");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        conditions.add(condition2);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        when(widgetMock.getVisibility()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        assertFalse(uiRegistry.getVisiblity(widgetMock));

        when(itemMock.getState()).thenReturn(new DecimalType(21.0));

        assertTrue(uiRegistry.getVisiblity(widgetMock));

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        assertTrue(uiRegistry.getVisiblity(widgetMock));

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        assertFalse(uiRegistry.getVisiblity(widgetMock));
    }

    @Test
    public void getConfirmCmd() {
        when(widgetMock.getConfirmCmd()).thenReturn(true);
        assertEquals(ItemUIRegistryImpl.DEFAULT_CONFIRM_CMD_MESSAGE, uiRegistry.getConfirmCmdMessage(widgetMock));

        when(widgetMock.getConfirmCmd()).thenReturn(false);
        assertNull(uiRegistry.getConfirmCmdMessage(widgetMock));

        String message = "Are you absolutely sure?";
        Rule rule = mock(Rule.class);
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        List<Condition> conditions = new ArrayList<>();
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn(message);
        when(widgetMock.getConfirmCmdRules()).thenReturn(rules);
        assertEquals(message, uiRegistry.getConfirmCmdMessage(widgetMock));

        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("21");
        when(condition.getCondition()).thenReturn(">=");
        Condition condition2 = mock(Condition.class);
        when(condition2.getValue()).thenReturn("24");
        when(condition2.getCondition()).thenReturn("<");
        conditions.add(condition);
        conditions.add(condition2);
        when(rule.getArgument()).thenReturn(null);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));
        assertNull(uiRegistry.getConfirmCmdMessage(mapviewMock));

        when(itemMock.getState()).thenReturn(new DecimalType(21.0));
        assertEquals(ItemUIRegistryImpl.DEFAULT_CONFIRM_CMD_MESSAGE, uiRegistry.getConfirmCmdMessage(widgetMock));

        when(rule.getArgument()).thenReturn(message);
        when(itemMock.getState()).thenReturn(new DecimalType(23.5));
        assertEquals(message, uiRegistry.getConfirmCmdMessage(widgetMock));

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));
        assertNull(uiRegistry.getConfirmCmdMessage(widgetMock));
    }

    @Test
    public void getCategoryWhenIconSetWithoutRules() {
        when(widgetMock.getWidgetType()).thenReturn("Text");
        when(widgetMock.getIcon()).thenReturn("temperature");
        when(widgetMock.isStaticIcon()).thenReturn(false);
        when(widgetMock.getIconRules()).thenReturn(List.of());

        String icon = uiRegistry.getCategory(widgetMock);
        assertEquals("temperature", icon);
    }

    @Test
    public void getCategoryWhenIconSetWithRules() {
        when(widgetMock.getWidgetType()).thenReturn("Text");
        when(widgetMock.getIcon()).thenReturn(null);
        when(widgetMock.isStaticIcon()).thenReturn(false);
        Condition condition = mock(Condition.class);
        when(condition.getValue()).thenReturn("21");
        when(condition.getCondition()).thenReturn(">=");
        Condition condition2 = mock(Condition.class);
        when(condition2.getValue()).thenReturn("24");
        when(condition2.getCondition()).thenReturn("<");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        conditions.add(condition2);
        Rule rule = mock(Rule.class);
        when(rule.getConditions()).thenReturn(conditions);
        when(rule.getArgument()).thenReturn("temperature");
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        List<Condition> conditions2 = new ArrayList<>();
        Rule rule2 = mock(Rule.class);
        when(rule2.getConditions()).thenReturn(conditions2);
        when(rule2.getArgument()).thenReturn("humidity");
        rules.add(rule2);
        when(widgetMock.getIconRules()).thenReturn(rules);

        when(itemMock.getState()).thenReturn(new DecimalType(20.9));

        String icon = uiRegistry.getCategory(widgetMock);
        assertEquals("humidity", icon);

        when(itemMock.getState()).thenReturn(new DecimalType(21.0));

        icon = uiRegistry.getCategory(widgetMock);
        assertEquals("temperature", icon);

        when(itemMock.getState()).thenReturn(new DecimalType(23.5));

        icon = uiRegistry.getCategory(widgetMock);
        assertEquals("temperature", icon);

        when(itemMock.getState()).thenReturn(new DecimalType(24.0));

        icon = uiRegistry.getCategory(widgetMock);
        assertEquals("humidity", icon);
    }

    @Test
    public void getCategoryWhenStaticIconSet() {
        when(widgetMock.getWidgetType()).thenReturn("Text");
        when(widgetMock.getIcon()).thenReturn("temperature");
        when(widgetMock.isStaticIcon()).thenReturn(true);
        when(widgetMock.getIconRules()).thenReturn(List.of());

        String icon = uiRegistry.getCategory(widgetMock);
        assertEquals("temperature", icon);
    }

    @Test
    public void getCategoryWhenIconSetOnItem() {
        when(widgetMock.getWidgetType()).thenReturn("Text");
        when(widgetMock.getIcon()).thenReturn(null);
        when(widgetMock.isStaticIcon()).thenReturn(false);
        when(widgetMock.getIconRules()).thenReturn(List.of());

        when(itemMock.getCategory()).thenReturn("temperature");

        String icon = uiRegistry.getCategory(widgetMock);
        assertEquals("temperature", icon);
    }

    @Test
    public void getCategoryDefaultIcon() {
        when(widgetMock.getWidgetType()).thenReturn("Text");
        when(widgetMock.getIcon()).thenReturn(null);
        when(widgetMock.isStaticIcon()).thenReturn(false);
        when(widgetMock.getIconRules()).thenReturn(List.of());

        when(itemMock.getCategory()).thenReturn(null);

        String icon = uiRegistry.getCategory(widgetMock);
        assertEquals("text", icon);
    }

    @Test
    public void getWidgetId() throws ItemNotFoundException {
        when(sitemapMock.getWidgets()).thenReturn(List.of(frameMock));
        when(frameMock.getWidgets()).thenReturn(List.of(switchMock, sliderMock, groupMock, colorpickerMock, imageMock,
                mapviewMock, selectionMock, setpointMock, chartMock, webviewMock, textMock, buttongridMock));
        when(buttongridMock.getWidgets()).thenReturn(List.of(buttonMock));
        when(frameMock.getParent()).thenReturn(sitemapMock);
        when(switchMock.getParent()).thenReturn(frameMock);
        when(sliderMock.getParent()).thenReturn(frameMock);
        when(groupMock.getParent()).thenReturn(frameMock);
        when(colorpickerMock.getParent()).thenReturn(frameMock);
        when(imageMock.getParent()).thenReturn(frameMock);
        when(mapviewMock.getParent()).thenReturn(frameMock);
        when(selectionMock.getParent()).thenReturn(frameMock);
        when(setpointMock.getParent()).thenReturn(frameMock);
        when(chartMock.getParent()).thenReturn(frameMock);
        when(webviewMock.getParent()).thenReturn(frameMock);
        when(textMock.getParent()).thenReturn(frameMock);
        when(buttongridMock.getParent()).thenReturn(frameMock);
        when(buttonMock.getParent()).thenReturn(buttongridMock);

        assertEquals("1_0", uiRegistry.getWidgetId(frameMock));
        assertEquals("1_00", uiRegistry.getWidgetId(switchMock));
        assertEquals("1_01", uiRegistry.getWidgetId(sliderMock));
        assertEquals("1_02", uiRegistry.getWidgetId(groupMock));
        assertEquals("1_03", uiRegistry.getWidgetId(colorpickerMock));
        assertEquals("1_04", uiRegistry.getWidgetId(imageMock));
        assertEquals("1_05", uiRegistry.getWidgetId(mapviewMock));
        assertEquals("1_06", uiRegistry.getWidgetId(selectionMock));
        assertEquals("1_07", uiRegistry.getWidgetId(setpointMock));
        assertEquals("1_08", uiRegistry.getWidgetId(chartMock));
        assertEquals("1_09", uiRegistry.getWidgetId(webviewMock));
        assertEquals("2_0010", uiRegistry.getWidgetId(textMock));
        assertEquals("2_0011", uiRegistry.getWidgetId(buttongridMock));
        assertEquals("2_001100", uiRegistry.getWidgetId(buttonMock));
    }

    @Test
    public void getWidget() throws ItemNotFoundException {
        when(sitemapMock.getWidgets()).thenReturn(List.of(frameMock));
        when(frameMock.getWidgets()).thenReturn(List.of(switchMock, buttongridMock));
        when(buttongridMock.getWidgets()).thenReturn(List.of(buttonMock));

        when(registryMock.getItem(anyString())).thenThrow(new ItemNotFoundException("not found"));

        assertEquals(frameMock, uiRegistry.getWidget(sitemapMock, "1_0"));
        assertEquals(switchMock, uiRegistry.getWidget(sitemapMock, "1_00"));
        assertEquals(buttongridMock, uiRegistry.getWidget(sitemapMock, "1_01"));
        assertEquals(buttonMock, uiRegistry.getWidget(sitemapMock, "1_010"));
        assertEquals(frameMock, uiRegistry.getWidget(sitemapMock, "2_00"));
        assertEquals(switchMock, uiRegistry.getWidget(sitemapMock, "2_0000"));
        assertEquals(buttongridMock, uiRegistry.getWidget(sitemapMock, "2_0001"));
        assertEquals(buttonMock, uiRegistry.getWidget(sitemapMock, "2_000100"));
        assertEquals(frameMock, uiRegistry.getWidget(sitemapMock, "3_000"));
        assertEquals(switchMock, uiRegistry.getWidget(sitemapMock, "3_000000"));
        assertEquals(buttongridMock, uiRegistry.getWidget(sitemapMock, "3_000001"));
        assertEquals(buttonMock, uiRegistry.getWidget(sitemapMock, "3_000001000"));
        assertEquals(frameMock, uiRegistry.getWidget(sitemapMock, "00"));
        assertEquals(switchMock, uiRegistry.getWidget(sitemapMock, "0000"));
        assertEquals(buttongridMock, uiRegistry.getWidget(sitemapMock, "0001"));
        assertEquals(buttonMock, uiRegistry.getWidget(sitemapMock, "000100"));
        assertNull(uiRegistry.getWidget(sitemapMock, "1_1"));
        assertNull(uiRegistry.getWidget(sitemapMock, "1_02"));
        assertNull(uiRegistry.getWidget(sitemapMock, "1_011"));
        assertNull(uiRegistry.getWidget(sitemapMock, "2_0"));
        assertNull(uiRegistry.getWidget(sitemapMock, "2_000"));
        assertNull(uiRegistry.getWidget(sitemapMock, "2_01"));
        assertNull(uiRegistry.getWidget(sitemapMock, "2_0002"));
        assertNull(uiRegistry.getWidget(sitemapMock, "2_000101"));
        assertNull(uiRegistry.getWidget(sitemapMock, "0"));
        assertNull(uiRegistry.getWidget(sitemapMock, "000"));
        assertNull(uiRegistry.getWidget(sitemapMock, "01"));
        assertNull(uiRegistry.getWidget(sitemapMock, "0002"));
        assertNull(uiRegistry.getWidget(sitemapMock, "000101"));
    }

    private void setSorting(String mode) throws Exception {
        uiRegistry.modified(Map.of("groupMembersSorting", mode));
    }

    @Test
    public void testDynamicGroupChildrenSortByName() throws Exception {
        setSorting("NAME");
        List<Widget> children = uiRegistry.getChildren(groupMock);
        assertEquals(List.of("Alpha", "Mango", "Zebra"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByLabel() throws Exception {
        setSorting("LABEL");
        List<Widget> children = uiRegistry.getChildren(groupMock);
        // switchItem label "Alpha", dimmerItem label "Zebra", contactItem no label -> falls back to name "Mango"
        assertEquals(List.of("Zebra", "Mango", "Alpha"), // Alpha(label) < Mango(name-fallback) < Zebra(label)
                children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByMetadataWithValues() throws Exception {
        setSorting("METADATA");
        setOrderMetadata(switchItemMock.getName(), "2");
        setOrderMetadata(dimmerItemMock.getName(), "1");
        setOrderMetadata(contactItemMock.getName(), "3");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        assertEquals(List.of("Alpha", "Zebra", "Mango"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByMetadataMissingValuesGoLast() throws Exception {
        setSorting("METADATA");
        setOrderMetadata(dimmerItemMock.getName(), "1"); // only one member has metadata

        List<Widget> children = uiRegistry.getChildren(groupMock);
        // memberB (has metadata) first, then remaining two by name fallback: Mango, Zebra
        assertEquals(List.of("Alpha", "Mango", "Zebra"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByMetadataStringNumericComparison() throws Exception {
        setSorting("METADATA");
        setOrderMetadata(switchItemMock.getName(), "10");
        setOrderMetadata(dimmerItemMock.getName(), "2");
        setOrderMetadata(contactItemMock.getName(), "1");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        assertEquals(List.of("Mango", "Alpha", "Zebra"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByLocationHierarchySameParentUsesOwnWidgetOrder() throws Exception {
        setSorting("METADATA");
        setupThreeLocationMembers("Kitchen", "LivingRoom", "Garage");
        setLocationMetadata("Kitchen", "GroundFloor");
        setLocationMetadata("LivingRoom", "GroundFloor");
        setLocationMetadata("Garage", "GroundFloor");
        setOrderMetadata("Kitchen", "2");
        setOrderMetadata("LivingRoom", "1");
        setOrderMetadata("Garage", "3");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        assertEquals(List.of("LivingRoom", "Kitchen", "Garage"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByLocationHierarchyDifferentParentsUsesParentOrder() throws Exception {
        setSorting("METADATA");
        setupThreeLocationMembers("Kitchen", "Zzz_Bedroom", "Garage");
        setLocationMetadata("Kitchen", "GroundFloor");
        setLocationMetadata("Zzz_Bedroom", "FirstFloor");
        setLocationMetadata("Garage", "GroundFloor");
        setOrderMetadata("GroundFloor", "2");
        setOrderMetadata("FirstFloor", "1");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        // FirstFloor (order 1) < GroundFloor (order 2) => Bedroom before the GroundFloor rooms.
        // Among the two GroundFloor siblings (Kitchen, Garage) with no own widgetOrder,
        // falls back to label/name: "Garage" < "Kitchen".
        assertEquals(List.of("Zzz_Bedroom", "Garage", "Kitchen"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByLocationHierarchyThreeLevelsDivergesAtGrandparent() throws Exception {
        setSorting("METADATA");
        setupThreeLocationMembers("RoomA", "RoomB", "RoomC");
        setLocationMetadata("RoomA", "Floor1");
        setLocationMetadata("RoomB", "Floor2");
        setLocationMetadata("RoomC", "Floor2");
        setLocationMetadata("Floor1", "Home");
        setLocationMetadata("Floor2", "Home");
        setOrderMetadata("Floor1", "2");
        setOrderMetadata("Floor2", "1");
        setOrderMetadata("RoomB", "2");
        setOrderMetadata("RoomC", "1");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        // Floor2 (order 1) beats Floor1 (order 2) => RoomB/RoomC before RoomA.
        // Within Floor2, siblings RoomB (order 2) vs RoomC (order 1) => RoomC before RoomB.
        assertEquals(List.of("RoomC", "RoomB", "RoomA"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByLocationHierarchyDifferentDepth() throws Exception {
        setSorting("METADATA");
        setupThreeLocationMembers("RoomA", "RoomB", "RoomC");
        setLocationMetadata("RoomA", "FirstFloor");
        setLocationMetadata("RoomB", "FirstFloor");
        setLocationMetadata("FirstFloor", "Home");
        setLocationMetadata("RoomC", "Home");
        setOrderMetadata("RoomA", "10");
        setOrderMetadata("RoomB", "9");
        setOrderMetadata("FirstFloor", "1");
        setOrderMetadata("RoomC", "2");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        // FirstFloor & RoomC are both depth 1 -> compared by own widgetOrder (1 < 2).
        assertEquals(List.of("RoomB", "RoomA", "RoomC"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByLocationHierarchyDifferentDepthShallowFirst() throws Exception {
        setSorting("METADATA");
        setupThreeLocationMembers("RoomA", "RoomB", "RoomC");
        setLocationMetadata("RoomA", "Upstairs");
        setLocationMetadata("RoomB", "FirstFloor");
        setLocationMetadata("FirstFloor", "Upstairs");
        setLocationMetadata("Upstairs", "Home");
        setLocationMetadata("RoomC", "Home");
        setOrderMetadata("Upstairs", "1");
        setOrderMetadata("RoomC", "2");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        // RoomA (depth 2) vs RoomB (depth 3): comparison is between RoomA and FirstFloor (depth 2), FirstFloor (and
        // therefore RoomB) comes first
        // RoomC (depth 1) compares to Upstairs -> widgetOrder of Upstairs (1) < RoomC (2)
        assertEquals(List.of("RoomB", "RoomA", "RoomC"), children.stream().map(Widget::getItem).toList());
    }

    @Test
    public void testDynamicGroupChildrenSortByMetadataMixedLocationAndNonLocationFallsBackToWidgetOrder()
            throws Exception {
        setSorting("METADATA");
        setupThreeLocationMembers("Kitchen", "TemperatureSensor", "Garage");
        setLocationMetadata("Kitchen", "GroundFloor");
        setLocationMetadata("Garage", "GroundFloor");
        setOrderMetadata("Kitchen", "1");
        setOrderMetadata("Garage", "2");

        List<Widget> children = uiRegistry.getChildren(groupMock);
        assertEquals(List.of("Kitchen", "Garage", "TemperatureSensor"),
                children.stream().map(Widget::getItem).toList());
    }

    private void setOrderMetadata(String itemName, String value) {
        MetadataKey key = new MetadataKey(ItemUIRegistryImpl.WIDGET_ORDER_KEY, itemName);
        when(metadataRegistryMock.get(key)).thenReturn(new Metadata(key, value, null));
    }

    private void setLocationMetadata(String itemName, @Nullable String parentName) {
        MetadataKey key = new MetadataKey(ItemUIRegistryImpl.SEMANTICS_KEY, itemName);
        Map<String, Object> config = parentName != null
                ? Map.of(ItemUIRegistryImpl.SEMANTICS_PARENT_LOCATION_CONFIG, parentName)
                : Map.of();
        // Value just needs to start with SEMANTICS_LOCATION for isLocationContext to be true.
        when(metadataRegistryMock.get(key))
                .thenReturn(new Metadata(key, ItemUIRegistryImpl.SEMANTICS_LOCATION + "_Room", config));
    }

    private void setupThreeLocationMembers(String name1, String name2, String name3) throws Exception {
        GroupItem groupItem1 = new GroupItem(name1);
        GroupItem groupItem2 = new GroupItem(name2);
        GroupItem groupItem3 = new GroupItem(name3);
        Group group1Mock = mock(Group.class);
        Group group2Mock = mock(Group.class);
        Group group3Mock = mock(Group.class);
        when(groupItemMock.getMembers()).thenReturn(Set.of(groupItem1, groupItem2, groupItem3));
        when(group1Mock.getItem()).thenReturn(name1);
        when(group2Mock.getItem()).thenReturn(name2);
        when(group3Mock.getItem()).thenReturn(name3);
        doReturn(group1Mock).when(uiRegistry).getDefaultWidget(GroupItem.class, name1);
        doReturn(group2Mock).when(uiRegistry).getDefaultWidget(GroupItem.class, name2);
        doReturn(group3Mock).when(uiRegistry).getDefaultWidget(GroupItem.class, name3);
    }
}
