/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.rest.sitemap.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Group;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.openhab.core.ui.items.ItemUIRegistry.WidgetLabelSource;
import org.osgi.framework.BundleContext;

/**
 * Test aspects of the {@link SitemapResource}.
 *
 * @author Henning Treu - Initial contribution
 * @author Laurent Garnier - Extended tests for static icon and icon based on conditional rules
 * @author Mark Herwege - Implement sitemap registry
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SitemapResourceTest extends JavaTest {

    private static final int STATE_UPDATE_WAIT_TIME = 100;

    private static final String HTTP_HEADER_X_ATMOSPHERE_TRANSPORT = "X-Atmosphere-Transport";
    private static final String ITEM_NAME = "itemName";

    private static final String ITEM_LABEL = "item label";
    private static final String SITEMAP_PATH = "/sitemaps";
    private static final String SITEMAP_NAME = "defaultSitemap";
    private static final String SITEMAP_TITLE = "Default Sitemap";
    private static final String VISIBILITY_RULE_ITEM_NAME = "visibilityRuleItem";
    private static final String LABEL_COLOR_ITEM_NAME = "labelColorItemName";
    private static final String VALUE_COLOR_ITEM_NAME = "valueColorItemName";
    private static final String ICON_COLOR_ITEM_NAME = "iconColorItemName";
    private static final String ICON_ITEM_NAME = "iconItemName";
    private static final String WIDGET1_LABEL = "widget 1";
    private static final String WIDGET3_LABEL = "widget 3";
    private static final String GROUP_LABEL = "frame";
    private static final String WIDGET4_LABEL = "widget 4";
    private static final String WIDGET1_ID = "00";
    private static final String WIDGET2_ID = "01";
    private static final String WIDGET3_ID = "02";
    private static final String FRAME_ID = "03";
    private static final String WIDGET4_ID = "04";
    private static final String WIDGET1_ICON = "icon1";
    private static final String WIDGET2_ICON = "icon2";
    private static final String WIDGET3_ICON = "icon3";
    private static final String GROUP_ICON = "frame";
    private static final String WIDGET4_ICON = "icon4";
    private static final String CLIENT_IP = "127.0.0.1";

    private @NonNullByDefault({}) SitemapResource sitemapResource;
    private @NonNullByDefault({}) SitemapSubscriptionService subscriptions;
    private @NonNullByDefault({}) GenericItem item;
    private @NonNullByDefault({}) GenericItem visibilityRuleItem;
    private @NonNullByDefault({}) GenericItem labelColorItem;
    private @NonNullByDefault({}) GenericItem valueColorItem;
    private @NonNullByDefault({}) GenericItem iconColorItem;
    private @NonNullByDefault({}) GenericItem iconItem;

    private @Mock @NonNullByDefault({}) HttpHeaders headersMock;
    private @Mock @NonNullByDefault({}) Sitemap defaultSitemapMock;
    private @Mock @NonNullByDefault({}) ItemUIRegistry itemUIRegistryMock;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProviderMock;
    private @Mock @NonNullByDefault({}) LocaleService localeServiceMock;
    private @Mock @NonNullByDefault({}) HttpServletRequest requestMock;
    private @Mock @NonNullByDefault({}) SitemapRegistry sitemapRegistryMock;
    private @Mock @NonNullByDefault({}) UriInfo uriInfoMock;
    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;

    private List<Widget> widgets = new ArrayList<>();

    @BeforeEach
    public void setup() throws Exception {
        subscriptions = new SitemapSubscriptionService(Collections.emptyMap(), itemUIRegistryMock, sitemapRegistryMock,
                timeZoneProviderMock, bundleContextMock);

        sitemapResource = new SitemapResource(itemUIRegistryMock, sitemapRegistryMock, localeServiceMock,
                timeZoneProviderMock, subscriptions);

        when(uriInfoMock.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath(SITEMAP_PATH));
        when(uriInfoMock.getBaseUriBuilder()).thenReturn(UriBuilder.fromPath(SITEMAP_PATH));
        sitemapResource.uriInfo = uriInfoMock;

        when(requestMock.getRemoteAddr()).thenReturn(CLIENT_IP);
        sitemapResource.request = requestMock;

        item = new TestItem(ITEM_NAME);
        visibilityRuleItem = new TestItem(VISIBILITY_RULE_ITEM_NAME);
        labelColorItem = new TestItem(LABEL_COLOR_ITEM_NAME);
        valueColorItem = new TestItem(VALUE_COLOR_ITEM_NAME);
        iconColorItem = new TestItem(ICON_COLOR_ITEM_NAME);
        iconItem = new TestItem(ICON_ITEM_NAME);

        when(localeServiceMock.getLocale(null)).thenReturn(Locale.US);

        configureSitemapRegistryMock();
        configureSitemapMock();

        widgets = initSitemapWidgetsWithSubpages();
        configureItemUIRegistry(PercentType.HUNDRED, OnOffType.ON);

        // Disable long polling
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);
    }

    @Test
    public void whenSitemapsAreProvidedShouldReturnSitemapBeans() {
        Response sitemaps = sitemapResource.getSitemaps();

        assertThat((Collection<?>) sitemaps.getEntity(), hasSize(1));

        @SuppressWarnings("unchecked")
        SitemapDTO dto = ((Collection<SitemapDTO>) sitemaps.getEntity()).iterator().next();
        assertThat(dto.name, is(SITEMAP_NAME));
        assertThat(dto.link, is(SITEMAP_PATH + "/" + SITEMAP_NAME));
    }

    @Test
    public void whenLongPollingWholeSitemapShouldObserveAllItems() throws ItemNotFoundException {
        configureItemUIRegistryWithSubpages(PercentType.HUNDRED, OnOffType.ON, OpenClosedType.OPEN);

        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(item.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getSitemapData(headersMock, null, SITEMAP_NAME, null, false);

        SitemapDTO sitemapDTO = (SitemapDTO) response.getEntity();
        // assert that the item state change did trigger the blocking method to return
        assertThat(sitemapDTO.homepage.timeout, is(false));
    }

    @Test
    public void whenLongPollingSpecificPageMustNotObserveAllItems() throws ItemNotFoundException {
        configureItemUIRegistryWithSubpages(PercentType.HUNDRED, OnOffType.ON, OpenClosedType.OPEN);

        // TODO it would be cooler to not wait 30s in this test because SitemapResource has that hardcoded timeout

        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(item.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        // assert that the item state change did trigger the blocking method to return
        assertThat(pageDTO.timeout, is(true));
    }

    @Test
    public void whenLongPollingShouldObserveItems() {
        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(item.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        // assert that the item state change did trigger the blocking method to return
        assertThat(pageDTO.timeout, is(false));
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromRules() {
        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(visibilityRuleItem.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        // assert that the item state change did trigger the blocking method to return
        assertThat(pageDTO.timeout, is(false));
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromLabelColorConditions() {
        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(labelColorItem.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        // assert that the item state change did trigger the blocking method to return
        assertThat(pageDTO.timeout, is(false));
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromValueColorConditions() {
        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(valueColorItem.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromIconColorConditions() {
        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(iconColorItem.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromIconConditions() {
        ItemEvent itemEvent = mock(ItemEvent.class);
        when(itemEvent.getItemName()).thenReturn(iconItem.getName());
        executeWithDelay(() -> sitemapResource.receive(itemEvent));

        // non-null is sufficient here.
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        // assert that the item state change did trigger the blocking method to return
        assertThat(pageDTO.timeout, is(false));
    }

    private static void executeWithDelay(Runnable executionWithDelay) {
        new Thread(() -> {
            try {
                // wait for the #getPageData call and listeners to attach to the item
                Thread.sleep(STATE_UPDATE_WAIT_TIME);
                executionWithDelay.run();
            } catch (InterruptedException e) {
            }
        }).start();
    }

    @Test
    public void whenGetPageDataShouldReturnPageBean() throws ItemNotFoundException {
        item.setState(new PercentType(50));
        configureItemUIRegistry(item.getState(), OnOffType.ON);

        // Disable long polling
        when(headersMock.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);

        Response response = sitemapResource.getPageData(headersMock, null, SITEMAP_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.id, is(SITEMAP_NAME));
        assertThat(pageDTO.title, is(SITEMAP_TITLE));
        assertThat(pageDTO.leaf, is(true));
        assertThat(pageDTO.timeout, is(false));

        assertThat(pageDTO.widgets, notNullValue());
        assertThat((Collection<?>) pageDTO.widgets, hasSize(3));

        assertThat(pageDTO.widgets.getFirst().widgetId, is(WIDGET1_ID));
        assertThat(pageDTO.widgets.getFirst().label, is(WIDGET1_LABEL));
        assertThat(pageDTO.widgets.getFirst().labelSource, is("SITEMAP_WIDGET"));
        assertThat(pageDTO.widgets.getFirst().labelcolor, is("GREEN"));
        assertThat(pageDTO.widgets.getFirst().valuecolor, is("BLUE"));
        assertThat(pageDTO.widgets.getFirst().iconcolor, is("ORANGE"));
        assertThat(pageDTO.widgets.getFirst().icon, is(WIDGET1_ICON));
        assertThat(pageDTO.widgets.getFirst().staticIcon, is(true));
        assertThat(pageDTO.widgets.getFirst().state, nullValue());
        assertThat(pageDTO.widgets.getFirst().item, notNullValue());
        assertThat(pageDTO.widgets.getFirst().item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.getFirst().item.state, is("50"));

        assertThat(pageDTO.widgets.get(1).widgetId, is(WIDGET2_ID));
        assertThat(pageDTO.widgets.get(1).label, is(ITEM_LABEL));
        assertThat(pageDTO.widgets.get(1).labelSource, is("ITEM_LABEL"));
        assertThat(pageDTO.widgets.get(1).labelcolor, nullValue());
        assertThat(pageDTO.widgets.get(1).valuecolor, nullValue());
        assertThat(pageDTO.widgets.get(1).iconcolor, nullValue());
        assertThat(pageDTO.widgets.get(1).icon, is(WIDGET2_ICON));
        assertThat(pageDTO.widgets.get(1).staticIcon, is(false));
        assertThat(pageDTO.widgets.get(1).state, is("ON"));
        assertThat(pageDTO.widgets.get(1).item, notNullValue());
        assertThat(pageDTO.widgets.get(1).item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.get(1).item.state, is("50"));

        assertThat(pageDTO.widgets.get(2).widgetId, is(WIDGET3_ID));
        assertThat(pageDTO.widgets.get(2).label, is(WIDGET3_LABEL));
        assertThat(pageDTO.widgets.get(2).labelcolor, nullValue());
        assertThat(pageDTO.widgets.get(2).valuecolor, nullValue());
        assertThat(pageDTO.widgets.get(2).iconcolor, nullValue());
        assertThat(pageDTO.widgets.get(2).icon, is(WIDGET3_ICON));
        assertThat(pageDTO.widgets.get(2).staticIcon, is(true));
        assertThat(pageDTO.widgets.get(2).state, is("ON"));
        assertThat(pageDTO.widgets.get(2).item, notNullValue());
        assertThat(pageDTO.widgets.get(2).item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.get(2).item.state, is("50"));
    }

    private void configureItemUIRegistryWithSubpages(State state1, State state2, State state3)
            throws ItemNotFoundException {
        Group group1 = (Group) widgets.getFirst();
        Group group2 = (Group) widgets.get(4);

        when(itemUIRegistryMock.getChildren(defaultSitemapMock)).thenReturn(List.of(group1, group2));
        configureCommonUIRegistryMockMethods();
        List<Widget> subpage1Widgets = group1.getWidgets();

        configureItemUIRegistryForWidget(group1, FRAME_ID, GROUP_ICON, GROUP_LABEL, WidgetLabelSource.SITEMAP_WIDGET,
                true, null, null, null, null);
        when(itemUIRegistryMock.getChildren(group1)).thenReturn(subpage1Widgets);

        configureWidgetStatesPage1(state1, state2);

        List<Widget> subpage2Widgets = group2.getWidgets();

        configureItemUIRegistryForWidget(group2, FRAME_ID, GROUP_ICON, GROUP_LABEL, WidgetLabelSource.SITEMAP_WIDGET,
                true, null, null, null, null);
        when(itemUIRegistryMock.getChildren(group2)).thenReturn(subpage2Widgets);

        configureWidgetStatesPage2(state3);
    }

    private void configureItemUIRegistry(State state1, State state2) throws ItemNotFoundException {
        List<Widget> mainpageWidgets = widgets.subList(1, 4);

        when(itemUIRegistryMock.getChildren(defaultSitemapMock)).thenReturn(mainpageWidgets);
        configureCommonUIRegistryMockMethods();

        configureWidgetStatesPage1(state1, state2);
    }

    private void configureCommonUIRegistryMockMethods() throws ItemNotFoundException {
        when(itemUIRegistryMock.getItem(ITEM_NAME)).thenReturn(item);
        when(itemUIRegistryMock.getItem(VISIBILITY_RULE_ITEM_NAME)).thenReturn(visibilityRuleItem);
        when(itemUIRegistryMock.getItem(LABEL_COLOR_ITEM_NAME)).thenReturn(labelColorItem);
        when(itemUIRegistryMock.getItem(VALUE_COLOR_ITEM_NAME)).thenReturn(valueColorItem);
        when(itemUIRegistryMock.getItem(ICON_COLOR_ITEM_NAME)).thenReturn(iconColorItem);
        when(itemUIRegistryMock.getItem(ICON_ITEM_NAME)).thenReturn(iconItem);
    }

    private void configureWidgetStatesPage1(State state1, State state2) {
        List<Widget> mainpageWidgets = widgets.subList(1, 4);
        Widget w1 = mainpageWidgets.getFirst();
        configureItemUIRegistryForWidget(w1, WIDGET1_ID, WIDGET1_ICON, WIDGET1_LABEL, WidgetLabelSource.SITEMAP_WIDGET,
                true, "GREEN", "BLUE", "ORANGE", state1);

        Widget w2 = mainpageWidgets.get(1);
        configureItemUIRegistryForWidget(w2, WIDGET2_ID, WIDGET2_ICON, ITEM_LABEL, WidgetLabelSource.ITEM_LABEL, true,
                null, null, null, state2);

        Widget w3 = mainpageWidgets.get(2);
        configureItemUIRegistryForWidget(w3, WIDGET3_ID, WIDGET3_ICON, WIDGET3_LABEL, WidgetLabelSource.SITEMAP_WIDGET,
                true, null, null, null, state2);
    }

    private void configureWidgetStatesPage2(State state3) {
        Widget w4 = widgets.get(5);
        configureItemUIRegistryForWidget(w4, WIDGET4_ID, WIDGET4_ICON, WIDGET4_LABEL, WidgetLabelSource.SITEMAP_WIDGET,
                true, null, null, null, state3);
    }

    private void configureItemUIRegistryForWidget(Widget w, String widgetId, String widgetIcon, String widgetLabel,
            WidgetLabelSource widgetLabelSource, boolean visibility, String labelColor, String valueColor,
            String iconColor, State state) {
        when(itemUIRegistryMock.getWidgetId(w)).thenReturn(widgetId);
        when(itemUIRegistryMock.getCategory(w)).thenReturn(widgetIcon);
        when(itemUIRegistryMock.getLabel(w)).thenReturn(widgetLabel);
        when(itemUIRegistryMock.getLabelSource(w)).thenReturn(widgetLabelSource);
        when(itemUIRegistryMock.getVisiblity(w)).thenReturn(visibility);
        when(itemUIRegistryMock.getLabelColor(w)).thenReturn(labelColor);
        when(itemUIRegistryMock.getValueColor(w)).thenReturn(valueColor);
        when(itemUIRegistryMock.getIconColor(w)).thenReturn(iconColor);
        when(itemUIRegistryMock.getState(w)).thenReturn(state);
    }

    private List<Widget> initSitemapWidgets() {
        // Initialize a sitemap containing 2 widgets linked to the same number item,
        // one slider and one switch,
        // which has one subpage

        // add icon rules to the mock widget:
        Class<Rule> classToMock = Rule.class;
        Rule iconRule = mock(classToMock);
        Condition conditon0 = mock(Condition.class);
        when(conditon0.getItem()).thenReturn(ICON_ITEM_NAME);
        List<Condition> conditions0 = new ArrayList<>();
        conditions0.add(conditon0);
        when(iconRule.getConditions()).thenReturn(conditions0);
        List<Rule> iconRulesW1 = new ArrayList<>();
        iconRulesW1.add(iconRule);

        // add visibility rules to the mock widget:
        Rule visibilityRule = mock(Rule.class);
        Condition conditon = mock(Condition.class);
        when(conditon.getItem()).thenReturn(VISIBILITY_RULE_ITEM_NAME);
        List<Condition> conditions = new ArrayList<>();
        conditions.add(conditon);
        when(visibilityRule.getConditions()).thenReturn(conditions);
        List<Rule> visibilityRulesW1 = new ArrayList<>(1);
        visibilityRulesW1.add(visibilityRule);

        // add label color conditions to the item:
        Rule labelColor = mock(Rule.class);
        Condition conditon1 = mock(Condition.class);
        when(conditon1.getItem()).thenReturn(LABEL_COLOR_ITEM_NAME);
        List<Condition> conditions1 = new ArrayList<>();
        conditions1.add(conditon1);
        when(labelColor.getConditions()).thenReturn(conditions1);
        List<Rule> labelColorsW1 = new ArrayList<>();
        labelColorsW1.add(labelColor);

        // add value color conditions to the item:
        Rule valueColor = mock(Rule.class);
        Condition conditon2 = mock(Condition.class);
        when(conditon2.getItem()).thenReturn(VALUE_COLOR_ITEM_NAME);
        List<Condition> conditions2 = new ArrayList<>();
        conditions2.add(conditon2);
        when(valueColor.getConditions()).thenReturn(conditions2);
        List<Rule> valueColorsW1 = new ArrayList<>();
        valueColorsW1.add(valueColor);

        // add icon color conditions to the item:
        Rule iconColor = mock(Rule.class);
        Condition conditon3 = mock(Condition.class);
        when(conditon3.getItem()).thenReturn(ICON_COLOR_ITEM_NAME);
        List<Condition> conditions3 = new ArrayList<>();
        conditions3.add(conditon3);
        when(iconColor.getConditions()).thenReturn(conditions3);
        List<Rule> iconColorsW1 = new ArrayList<>();
        iconColorsW1.add(iconColor);

        String sliderType = "Slider";

        Widget w1 = mockWidget(iconRulesW1, visibilityRulesW1, labelColorsW1, valueColorsW1, iconColorsW1, sliderType,
                WIDGET1_LABEL, null, false);

        List<Rule> iconRules = new ArrayList<>();
        List<Rule> visibilityRules = new ArrayList<>();
        List<Rule> labelColors = new ArrayList<>();
        List<Rule> valueColors = new ArrayList<>();
        List<Rule> iconColors = new ArrayList<>();

        String switchType = "Switch";

        Widget w2 = mockWidget(iconRules, visibilityRules, labelColors, valueColors, iconColors, switchType, null,
                WIDGET2_ICON, false);
        mock(Widget.class);

        Widget w3 = mockWidget(iconRules, visibilityRules, labelColors, valueColors, iconColors, switchType,
                WIDGET3_LABEL, WIDGET3_ICON, true);

        List<Widget> widgets = new ArrayList<>(3);
        widgets.add(w1);
        widgets.add(w2);
        widgets.add(w3);
        return widgets;
    }

    private List<Widget> initSitemapWidgetsWithSubpages() {
        List<Widget> baseWidgets = initSitemapWidgets();

        String groupType = "Group";

        List<Rule> iconRules = new ArrayList<>();
        List<Rule> visibilityRules = new ArrayList<>();
        List<Rule> labelColors = new ArrayList<>();
        List<Rule> valueColors = new ArrayList<>();
        List<Rule> iconColors = new ArrayList<>();

        Widget group1 = mockGroup(iconRules, visibilityRules, labelColors, valueColors, iconColors, groupType,
                GROUP_LABEL, GROUP_ICON, true, baseWidgets);

        String switchType = "Switch";

        Widget w4 = mockWidget(iconRules, visibilityRules, labelColors, valueColors, iconColors, switchType,
                WIDGET4_LABEL, WIDGET4_ICON, true);

        Widget group2 = mockGroup(iconRules, visibilityRules, labelColors, valueColors, iconColors, groupType,
                GROUP_LABEL, GROUP_ICON, true, new ArrayList<>(List.of(w4)));

        List<Widget> allWidgets = new ArrayList<>();
        allWidgets.add(group1);
        allWidgets.addAll(baseWidgets);
        allWidgets.add(group2);
        allWidgets.add(w4);
        return allWidgets;
    }

    private static Widget mockWidget(List<Rule> iconRules1, List<Rule> visibilityRules1, List<Rule> labelColors1,
            List<Rule> valueColors1, List<Rule> iconColors1, String widgetType, String widgetLabel, String widgetIcon,
            boolean widgetStaticIcon) {
        Widget w = mock(Widget.class);
        mockWidgetMethods(iconRules1, visibilityRules1, labelColors1, valueColors1, iconColors1, widgetType,
                widgetLabel, widgetIcon, widgetStaticIcon, w);
        when(w.getItem()).thenReturn(ITEM_NAME);
        return w;
    }

    private static Group mockGroup(List<Rule> iconRules1, List<Rule> visibilityRules1, List<Rule> labelColors1,
            List<Rule> valueColors1, List<Rule> iconColors1, String widgetType, String widgetLabel, String widgetIcon,
            boolean widgetStaticIcon, List<Widget> children) {
        Group w = mock(Group.class);
        mockWidgetMethods(iconRules1, visibilityRules1, labelColors1, valueColors1, iconColors1, widgetType,
                widgetLabel, widgetIcon, widgetStaticIcon, w);
        when(w.getWidgets()).thenReturn(children);
        return w;
    }

    private static void mockWidgetMethods(List<Rule> iconRules1, List<Rule> visibilityRules1, List<Rule> labelColors1,
            List<Rule> valueColors1, List<Rule> iconColors1, String widgetType, String widgetLabel, String widgetIcon,
            boolean widgetStaticIcon, Widget w) {
        when(w.getWidgetType()).thenReturn(widgetType);
        when(w.getLabel()).thenReturn(widgetLabel);
        when(w.getIcon()).thenReturn(widgetIcon);
        when(w.isStaticIcon()).thenReturn(widgetStaticIcon);
        when(w.getIconRules()).thenReturn(iconRules1);
        when(w.getVisibility()).thenReturn(visibilityRules1);
        when(w.getLabelColor()).thenReturn(labelColors1);
        when(w.getValueColor()).thenReturn(valueColors1);
        when(w.getIconColor()).thenReturn(iconColors1);
    }

    private void configureSitemapMock() {
        when(defaultSitemapMock.getName()).thenReturn(SITEMAP_NAME);
        when(defaultSitemapMock.getLabel()).thenReturn(SITEMAP_TITLE);
        when(defaultSitemapMock.getIcon()).thenReturn("");
    }

    private void configureSitemapRegistryMock() {
        when(sitemapRegistryMock.getAll()).thenReturn(Set.of(defaultSitemapMock));
        when(sitemapRegistryMock.get(SITEMAP_NAME)).thenReturn(defaultSitemapMock);
    }

    private static class TestItem extends GenericItem {

        public TestItem(String name) {
            super("Number", name);
            label = ITEM_LABEL;
        }

        @Override
        public List<Class<? extends State>> getAcceptedDataTypes() {
            return List.of();
        }

        @Override
        public List<Class<? extends Command>> getAcceptedCommandTypes() {
            return List.of();
        }
    }
}
