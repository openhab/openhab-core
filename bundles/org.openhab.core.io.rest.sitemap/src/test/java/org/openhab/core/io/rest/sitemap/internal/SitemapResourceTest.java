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
package org.openhab.core.io.rest.sitemap.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.VisibilityRule;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.ui.items.ItemUIRegistry;

/**
 * Test aspects of the {@link SitemapResource}.
 *
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
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

    private @Mock HttpHeaders headers;
    private @Mock Sitemap defaultSitemap;
    private @Mock ItemUIRegistry itemUIRegistry;
    private @Mock LocaleService localeService;
    private @Mock HttpServletRequest request;
    private @Mock SitemapProvider sitemapProvider;
    private @Mock SitemapSubscriptionService subscriptions;
    private @Mock UriInfo uriInfo;

    private GenericItem item;
    private GenericItem visibilityRuleItem;
    private GenericItem labelColorItem;
    private GenericItem valueColorItem;

    private EList<Widget> widgets;

    @BeforeEach
    public void setup() throws Exception {
        sitemapResource = new SitemapResource(itemUIRegistry, localeService, subscriptions);

        when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath(SITEMAP_PATH));
        when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromPath(SITEMAP_PATH));
        sitemapResource.uriInfo = uriInfo;

        when(request.getRemoteAddr()).thenReturn(CLIENT_IP);
        sitemapResource.request = request;

        item = new TestItem(ITEM_NAME);
        visibilityRuleItem = new TestItem(VISIBILITY_RULE_ITEM_NAME);
        labelColorItem = new TestItem(LABEL_COLOR_ITEM_NAME);
        valueColorItem = new TestItem(VALUE_COLOR_ITEM_NAME);

        when(localeService.getLocale(null)).thenReturn(Locale.US);

        configureSitemapProviderMock();
        configureSitemapMock();
        sitemapResource.addSitemapProvider(sitemapProvider);

        widgets = initSitemapWidgets();
        configureItemUIRegistry(PercentType.HUNDRED, OnOffType.ON);

        // Disable long polling
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);
    }

    @Test
    public void whenNoSitemapProvidersAreSetShouldReturnEmptyList() {
        sitemapResource.removeSitemapProvider(sitemapProvider);
        Response sitemaps = sitemapResource.getSitemaps();

        assertThat(sitemaps.getEntity(), instanceOf(Collection.class));
        assertThat((Collection<?>) sitemaps.getEntity(), is(empty()));
    }

    @Test
    public void whenSitemapsAreProvidedShouldReturnSitemapBeans() {
        Response sitemaps = sitemapResource.getSitemaps();

        assertThat((Collection<?>) sitemaps.getEntity(), hasSize(1));

        @SuppressWarnings("unchecked")
        SitemapDTO dto = ((Collection<SitemapDTO>) sitemaps.getEntity()).iterator().next();
        assertThat(dto.name, is(SITEMAP_MODEL_NAME));
        assertThat(dto.link, is(SITEMAP_PATH + "/" + SITEMAP_MODEL_NAME));
    }

    @Test
    public void whenLongPollingShouldObserveItems() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                item.setState(PercentType.ZERO);
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromVisibilityRules() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                visibilityRuleItem.setState(new DecimalType(BigDecimal.ONE));
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromLabelColorConditions() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                labelColorItem.setState(new DecimalType(BigDecimal.ONE));
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenLongPollingShouldObserveItemsFromValueColorConditions() {
        new Thread(() -> {
            try {
                Thread.sleep(STATE_UPDATE_WAIT_TIME); // wait for the #getPageData call and listeners to attach to the
                                                      // item
                valueColorItem.setState(new DecimalType(BigDecimal.ONE));
            } catch (InterruptedException e) {
            }
        }).start();

        // non-null is sufficient here.
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(List.of());

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void whenGetPageDataShouldReturnPageBean() throws ItemNotFoundException {
        item.setState(new PercentType(50));
        configureItemUIRegistry(item.getState(), OnOffType.ON);

        // Disable long polling
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

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
        when(sliderEClass.getInstanceTypeName()).thenReturn("org.openhab.core.model.sitemap.Slider");
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
        when(switchEClass.getInstanceTypeName()).thenReturn("org.openhab.core.model.sitemap.Switch");
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
        when(sitemapProvider.getSitemapNames()).thenReturn(Set.of(SITEMAP_MODEL_NAME));
        when(sitemapProvider.getSitemap(SITEMAP_MODEL_NAME)).thenReturn(defaultSitemap);
    }

    private class TestItem extends GenericItem {

        public TestItem(String name) {
            super("Number", name);
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
