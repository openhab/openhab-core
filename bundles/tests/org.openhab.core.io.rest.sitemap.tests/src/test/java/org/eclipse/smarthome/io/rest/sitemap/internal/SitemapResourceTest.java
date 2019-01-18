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
package org.eclipse.smarthome.io.rest.sitemap.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.model.sitemap.ColorArray;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.eclipse.smarthome.model.sitemap.VisibilityRule;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.test.java.JavaTest;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test aspects of the {@link SitemapResource}.
 *
 * @author Henning Treu - initial contribution
 *
 */
public class SitemapResourceTest extends JavaTest {

    private static final int STATE_UPDATE_WAIT_TIME = 100;

    private static final String HTTP_HEADER_X_ATMOSPHERE_TRANSPORT = "X-Atmosphere-Transport";
    private static final String ITEM_NAME = "itemName";
    private static final String SITEMAP_PATH = "/sitemaps";
    private static final String SITEMAP_MODEL_NAME = "sitemapModel";
    private static final String SITEMAP_NAME = "defaultSitemap";
    private static final String SITEMAP_TITLE = "Default Sitemap";
    private static final String VISIBILITY_RULE_ITEM_NAME = "visibilityRuleItem";
    private static final String LABEL_COLOR_ITEM_NAME = "labelColorItemName";
    private static final String VALUE_COLOR_ITEM_NAME = "valueColorItemName";
    private static final String WIDGET1_LABEL = "widget 1";
    private static final String WIDGET2_LABEL = "widget 2";
    private static final String WIDGET1_ID = "00";
    private static final String WIDGET2_ID = "01";
    private static final String CLIENT_IP = "127.0.0.1";

    private SitemapResource sitemapResource;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private HttpServletRequest request;

    @Mock
    private SitemapProvider sitemapProvider;

    @Mock
    private Sitemap defaultSitemap;

    @Mock
    private ItemUIRegistry itemUIRegistry;

    @Mock
    private HttpHeaders headers;

    private GenericItem item;
    private GenericItem visibilityRuleItem;
    private GenericItem labelColorItem;
    private GenericItem valueColorItem;

    private EList<Widget> widgets;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        sitemapResource = new SitemapResource();

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath(SITEMAP_PATH));
        when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromPath(SITEMAP_PATH));
        sitemapResource.uriInfo = uriInfo;

        when(request.getRemoteAddr()).thenReturn(CLIENT_IP);
        sitemapResource.request = request;

        item = new TestItem(ITEM_NAME);
        visibilityRuleItem = new TestItem(VISIBILITY_RULE_ITEM_NAME);
        labelColorItem = new TestItem(LABEL_COLOR_ITEM_NAME);
        valueColorItem = new TestItem(VALUE_COLOR_ITEM_NAME);

        LocaleService localeService = mock(LocaleService.class);
        when(localeService.getLocale(null)).thenReturn(Locale.US);
        sitemapResource.setLocaleService(localeService);

        configureSitemapProviderMock();
        configureSitemapMock();
        sitemapResource.addSitemapProvider(sitemapProvider);

        widgets = initSitemapWidgets();
        configureItemUIRegistry(PercentType.HUNDRED, OnOffType.ON);
        sitemapResource.setItemUIRegistry(itemUIRegistry);

        // Disable long polling
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);
    }

    @Test
    public void whenNoSitemapProvidersAreSet_ShouldReturnEmptyList() {
        sitemapResource.removeSitemapProvider(sitemapProvider);
        Response sitemaps = sitemapResource.getSitemaps();

        assertThat(sitemaps.getEntity(), instanceOf(Collection.class));
        assertThat((Collection<?>) sitemaps.getEntity(), is(empty()));
    }

    @Test
    public void whenSitemapsAreProvided_ShouldReturnSitemapBeans() {
        Response sitemaps = sitemapResource.getSitemaps();

        assertThat((Collection<?>) sitemaps.getEntity(), hasSize(1));

        @SuppressWarnings("unchecked")
        SitemapDTO dto = ((Collection<SitemapDTO>) sitemaps.getEntity()).iterator().next();
        assertThat(dto.name, is(SITEMAP_MODEL_NAME));
        assertThat(dto.link, is(SITEMAP_PATH + "/" + SITEMAP_MODEL_NAME));
    }

    @Test
    public void whenLongPolling_ShouldObserveItems() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                item.setState(PercentType.ZERO);
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(Collections.emptyList());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPolling_ShouldObserveItemsFromVisibilityRules() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                visibilityRuleItem.setState(new DecimalType(BigDecimal.ONE));
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(Collections.emptyList());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPolling_ShouldObserveItemsFromLabelColorConditions() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                labelColorItem.setState(new DecimalType(BigDecimal.ONE));
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(Collections.emptyList());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPolling_ShouldObserveItemsFromValueColorConditions() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                valueColorItem.setState(new DecimalType(BigDecimal.ONE));
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(Collections.emptyList());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenGetPageData_ShouldReturnPageBean() throws ItemNotFoundException {
        item.setState(new PercentType(50));
        configureItemUIRegistry(item.getState(), OnOffType.ON);

        // Disable long polling
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.id, is(SITEMAP_NAME));
        assertThat(pageDTO.title, is(SITEMAP_TITLE));
        assertThat(pageDTO.leaf, is(true));
        assertThat(pageDTO.timeout, is(false));

        assertThat(pageDTO.widgets, notNullValue());
        assertThat((Collection<?>) pageDTO.widgets, hasSize(2));

        assertThat(pageDTO.widgets.get(0).widgetId, is(WIDGET1_ID));
        assertThat(pageDTO.widgets.get(0).label, is(WIDGET1_LABEL));
        assertThat(pageDTO.widgets.get(0).labelcolor, is("GREEN"));
        assertThat(pageDTO.widgets.get(0).valuecolor, is("BLUE"));
        assertThat(pageDTO.widgets.get(0).state, nullValue());
        assertThat(pageDTO.widgets.get(0).item, notNullValue());
        assertThat(pageDTO.widgets.get(0).item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.get(0).item.state, is("50"));

        assertThat(pageDTO.widgets.get(1).widgetId, is(WIDGET2_ID));
        assertThat(pageDTO.widgets.get(1).label, is(WIDGET2_LABEL));
        assertThat(pageDTO.widgets.get(1).labelcolor, nullValue());
        assertThat(pageDTO.widgets.get(1).valuecolor, nullValue());
        assertThat(pageDTO.widgets.get(1).state, is("ON"));
        assertThat(pageDTO.widgets.get(1).item, notNullValue());
        assertThat(pageDTO.widgets.get(1).item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.get(1).item.state, is("50"));
    }

    private void configureItemUIRegistry(State state1, State state2) throws ItemNotFoundException {
        when(itemUIRegistry.getChildren(defaultSitemap)).thenReturn(widgets);
        when(itemUIRegistry.getItem(ITEM_NAME)).thenReturn(item);
        when(itemUIRegistry.getItem(VISIBILITY_RULE_ITEM_NAME)).thenReturn(visibilityRuleItem);
        when(itemUIRegistry.getItem(LABEL_COLOR_ITEM_NAME)).thenReturn(labelColorItem);
        when(itemUIRegistry.getItem(VALUE_COLOR_ITEM_NAME)).thenReturn(valueColorItem);

        when(itemUIRegistry.getWidgetId(widgets.get(0))).thenReturn(WIDGET1_ID);
        when(itemUIRegistry.getCategory(widgets.get(0))).thenReturn("");
        when(itemUIRegistry.getLabel(widgets.get(0))).thenReturn(WIDGET1_LABEL);
        when(itemUIRegistry.getVisiblity(widgets.get(0))).thenReturn(true);
        when(itemUIRegistry.getLabelColor(widgets.get(0))).thenReturn("GREEN");
        when(itemUIRegistry.getValueColor(widgets.get(0))).thenReturn("BLUE");
        when(itemUIRegistry.getState(widgets.get(0))).thenReturn(state1);

        when(itemUIRegistry.getWidgetId(widgets.get(1))).thenReturn(WIDGET2_ID);
        when(itemUIRegistry.getCategory(widgets.get(1))).thenReturn("");
        when(itemUIRegistry.getLabel(widgets.get(1))).thenReturn(WIDGET2_LABEL);
        when(itemUIRegistry.getVisiblity(widgets.get(1))).thenReturn(true);
        when(itemUIRegistry.getLabelColor(widgets.get(1))).thenReturn(null);
        when(itemUIRegistry.getValueColor(widgets.get(1))).thenReturn(null);
        when(itemUIRegistry.getState(widgets.get(1))).thenReturn(state2);
    }

    private EList<Widget> initSitemapWidgets() {
        // Initialize a sitemap containing 2 widgets linked to the same number item,
        // one slider and one switch

        Widget w1 = mock(Widget.class);
        EClass sliderEClass = mock(EClass.class);
        when(sliderEClass.getName()).thenReturn("slider");
        when(sliderEClass.getInstanceTypeName()).thenReturn("org.eclipse.smarthome.model.sitemap.Slider");
        when(w1.eClass()).thenReturn(sliderEClass);
        when(w1.getLabel()).thenReturn(WIDGET1_LABEL);
        when(w1.getItem()).thenReturn(ITEM_NAME);

        // add visibility rules to the mock widget:
        VisibilityRule visibilityRule = mock(VisibilityRule.class);
        when(visibilityRule.getItem()).thenReturn(VISIBILITY_RULE_ITEM_NAME);
        BasicEList<VisibilityRule> visibilityRules = new BasicEList<>(1);
        visibilityRules.add(visibilityRule);
        when(w1.getVisibility()).thenReturn(visibilityRules);

        // add label color conditions to the item:
        ColorArray labelColor = mock(ColorArray.class);
        when(labelColor.getItem()).thenReturn(LABEL_COLOR_ITEM_NAME);
        EList<ColorArray> labelColors = new BasicEList<>();
        labelColors.add(labelColor);
        when(w1.getLabelColor()).thenReturn(labelColors);

        // add value color conditions to the item:
        ColorArray valueColor = mock(ColorArray.class);
        when(valueColor.getItem()).thenReturn(VALUE_COLOR_ITEM_NAME);
        EList<ColorArray> valueColors = new BasicEList<>();
        valueColors.add(valueColor);
        when(w1.getValueColor()).thenReturn(valueColors);

        visibilityRules = new BasicEList<>();
        labelColors = new BasicEList<>();
        valueColors = new BasicEList<>();

        Widget w2 = mock(Widget.class);
        EClass switchEClass = mock(EClass.class);
        when(switchEClass.getName()).thenReturn("switch");
        when(switchEClass.getInstanceTypeName()).thenReturn("org.eclipse.smarthome.model.sitemap.Switch");
        when(w2.eClass()).thenReturn(switchEClass);
        when(w2.getLabel()).thenReturn(WIDGET2_LABEL);
        when(w2.getItem()).thenReturn(ITEM_NAME);
        when(w2.getVisibility()).thenReturn(visibilityRules);
        when(w2.getLabelColor()).thenReturn(labelColors);
        when(w2.getValueColor()).thenReturn(valueColors);

        BasicEList<Widget> widgets = new BasicEList<>(2);
        widgets.add(w1);
        widgets.add(w2);
        return widgets;
    }

    private void configureSitemapMock() {
        when(defaultSitemap.getName()).thenReturn(SITEMAP_NAME);
        when(defaultSitemap.getLabel()).thenReturn(SITEMAP_TITLE);
        when(defaultSitemap.getIcon()).thenReturn("");
    }

    private void configureSitemapProviderMock() {
        when(sitemapProvider.getSitemapNames()).thenReturn(Collections.singleton(SITEMAP_MODEL_NAME));
        when(sitemapProvider.getSitemap(SITEMAP_MODEL_NAME)).thenReturn(defaultSitemap);
    }

    private class TestItem extends GenericItem {

        public TestItem(String name) {
            super("Number", name);
        }

        @Override
        public List<Class<? extends State>> getAcceptedDataTypes() {
            return Collections.emptyList();
        }

        @Override
        public List<Class<? extends Command>> getAcceptedCommandTypes() {
            return Collections.emptyList();
        }
    }
}
