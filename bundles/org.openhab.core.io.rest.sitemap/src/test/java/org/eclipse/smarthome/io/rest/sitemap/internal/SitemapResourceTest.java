/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.model.sitemap.ColorArray;
import org.eclipse.smarthome.model.sitemap.Mapping;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.eclipse.smarthome.model.sitemap.Switch;
import org.eclipse.smarthome.model.sitemap.VisibilityRule;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.test.java.JavaTest;
import org.eclipse.smarthome.ui.internal.items.ItemUIRegistryImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test aspects of the {@link SitemapResource}.
 *
 * @author Henning Treu - Initial contribution
 * @author Henning Treu - Added a new DateTime item in the test and pattern in the labels
 *
 */
public class SitemapResourceTest extends JavaTest {

    private static final int STATE_UPDATE_WAIT_TIME = 100;

    private static final String HTTP_HEADER_X_ATMOSPHERE_TRANSPORT = "X-Atmosphere-Transport";
    private static final String ITEM_NAME = "itemName";
    private static final String ITEM2_NAME = "item2Name";
    private static final String SITEMAP_PATH = "/sitemaps";
    private static final String SITEMAP_MODEL_NAME = "sitemapModel";
    private static final String SITEMAP_NAME = "defaultSitemap";
    private static final String SITEMAP_TITLE = "Default Sitemap";
    private static final String VISIBILITY_RULE_ITEM_NAME = "visibilityRuleItem";
    private static final String LABEL_COLOR_ITEM_NAME = "labelColorItemName";
    private static final String VALUE_COLOR_ITEM_NAME = "valueColorItemName";
    private static final String WIDGET1_LABEL = "Number [%d]";
    private static final String WIDGET2_LABEL = "Switch";
    private static final String WIDGET3_LABEL = "DateTime [%1$td.%1$tm.%1$tY %1$tH:%1$tM]";
    private static final String WIDGET1_ID = "00";
    private static final String WIDGET2_ID = "01";
    private static final String WIDGET3_ID = "02";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final int NUMBER_VALUE = 50;
    private static final String DATETIME_VALUE = "2019-07-29T18:12:34.567+0200";

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
    private ItemRegistry itemRegistry;

    private ItemUIRegistryImpl itemUIRegistry;

    @Mock
    private HttpHeaders headers;

    private GenericItem item;
    private GenericItem item2;
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
        item.setState(PercentType.HUNDRED);
        item2 = new TestItem(ITEM2_NAME);
        item2.setState(new DateTimeType(DATETIME_VALUE));
        visibilityRuleItem = new TestItem(VISIBILITY_RULE_ITEM_NAME);
        visibilityRuleItem.setState(OnOffType.ON);
        labelColorItem = new TestItem(LABEL_COLOR_ITEM_NAME);
        labelColorItem.setState(OnOffType.ON);
        valueColorItem = new TestItem(VALUE_COLOR_ITEM_NAME);
        valueColorItem.setState(OnOffType.ON);

        LocaleService localeService = mock(LocaleService.class);
        when(localeService.getLocale(null)).thenReturn(Locale.US);
        sitemapResource.setLocaleService(localeService);

        widgets = initSitemapWidgets();

        configureSitemapProviderMock();
        configureSitemapMock();
        sitemapResource.addSitemapProvider(sitemapProvider);

        configureItemRegistry();
        itemUIRegistry = new TestItemUIRegistryImpl();
        itemUIRegistry.setItemRegistry(itemRegistry);

        sitemapResource.setItemUIRegistry(itemUIRegistry);

        // Disable long polling
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);

        // Set time zone to GMT+1
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01"));
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

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

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

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

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

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

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

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.timeout, is(false)); // assert that the item state change did trigger the blocking method to
                                                // return
    }

    @Test
    public void itemUIRegistryTest() throws ItemNotFoundException {
        item.setState(new PercentType(NUMBER_VALUE));

        assertThat(itemUIRegistry.getChildren(defaultSitemap), is(widgets));

        assertThat(itemUIRegistry.getItem(ITEM_NAME), is(item));
        assertThat(itemUIRegistry.getItem(ITEM2_NAME), is(item2));
        assertThat(itemUIRegistry.getItem(VISIBILITY_RULE_ITEM_NAME), is(visibilityRuleItem));
        assertThat(itemUIRegistry.getItem(LABEL_COLOR_ITEM_NAME), is(labelColorItem));
        assertThat(itemUIRegistry.getItem(VALUE_COLOR_ITEM_NAME), is(valueColorItem));

        assertThat(itemUIRegistry.getWidgetId(widgets.get(0)), is(WIDGET1_ID));
        assertThat(itemUIRegistry.getCategory(widgets.get(0)), is("slider"));
        assertThat(itemUIRegistry.getLabel(widgets.get(0)), is(String.format(WIDGET1_LABEL, NUMBER_VALUE)));
        assertThat(itemUIRegistry.getVisiblity(widgets.get(0)), is(true));
        assertThat(itemUIRegistry.getLabelColor(widgets.get(0)), is("GREEN"));
        assertThat(itemUIRegistry.getValueColor(widgets.get(0)), is("BLUE"));
        assertThat(itemUIRegistry.getState(widgets.get(0)), is(new PercentType(NUMBER_VALUE)));

        assertThat(itemUIRegistry.getWidgetId(widgets.get(1)), is(WIDGET2_ID));
        assertThat(itemUIRegistry.getCategory(widgets.get(1)), is("switch"));
        assertThat(itemUIRegistry.getLabel(widgets.get(1)), is(WIDGET2_LABEL));
        assertThat(itemUIRegistry.getVisiblity(widgets.get(1)), is(true));
        assertThat(itemUIRegistry.getLabelColor(widgets.get(1)), nullValue());
        assertThat(itemUIRegistry.getValueColor(widgets.get(1)), nullValue());
        assertThat(itemUIRegistry.getState(widgets.get(1)), is(OnOffType.ON));

        assertThat(itemUIRegistry.getWidgetId(widgets.get(2)), is(WIDGET3_ID));
        assertThat(itemUIRegistry.getCategory(widgets.get(2)), is("text"));
        assertThat(itemUIRegistry.getLabel(widgets.get(2)), is("DateTime [29.07.2019 18:12]"));
        assertThat(itemUIRegistry.getVisiblity(widgets.get(2)), is(true));
        assertThat(itemUIRegistry.getLabelColor(widgets.get(2)), nullValue());
        assertThat(itemUIRegistry.getValueColor(widgets.get(2)), nullValue());
        assertThat(itemUIRegistry.getState(widgets.get(2)), is(new DateTimeType(DATETIME_VALUE)));
    }

    @Test
    public void whenGetPageData_ShouldReturnPageBean() throws ItemNotFoundException {
        item.setState(new PercentType(NUMBER_VALUE));

        // Disable long polling
        when(headers.getRequestHeader(HTTP_HEADER_X_ATMOSPHERE_TRANSPORT)).thenReturn(null);

        Response response = sitemapResource.getPageData(headers, null, SITEMAP_MODEL_NAME, SITEMAP_NAME, null, false);

        PageDTO pageDTO = (PageDTO) response.getEntity();
        assertThat(pageDTO.id, is(SITEMAP_NAME));
        assertThat(pageDTO.title, is(SITEMAP_TITLE));
        assertThat(pageDTO.leaf, is(true));
        assertThat(pageDTO.timeout, is(false));

        assertThat(pageDTO.widgets, notNullValue());
        assertThat((Collection<?>) pageDTO.widgets, hasSize(3));

        assertThat(pageDTO.widgets.get(0).widgetId, is(WIDGET1_ID));
        assertThat(pageDTO.widgets.get(0).label, is(String.format(WIDGET1_LABEL, NUMBER_VALUE)));
        assertThat(pageDTO.widgets.get(0).labelcolor, is("GREEN"));
        assertThat(pageDTO.widgets.get(0).valuecolor, is("BLUE"));
        assertThat(pageDTO.widgets.get(0).state, nullValue());
        assertThat(pageDTO.widgets.get(0).item, notNullValue());
        assertThat(pageDTO.widgets.get(0).item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.get(0).item.state, is(String.valueOf(NUMBER_VALUE)));

        assertThat(pageDTO.widgets.get(1).widgetId, is(WIDGET2_ID));
        assertThat(pageDTO.widgets.get(1).label, is(WIDGET2_LABEL));
        assertThat(pageDTO.widgets.get(1).labelcolor, nullValue());
        assertThat(pageDTO.widgets.get(1).valuecolor, nullValue());
        assertThat(pageDTO.widgets.get(1).state, is("ON"));
        assertThat(pageDTO.widgets.get(1).item, notNullValue());
        assertThat(pageDTO.widgets.get(1).item.name, is(ITEM_NAME));
        assertThat(pageDTO.widgets.get(1).item.state, is(String.valueOf(NUMBER_VALUE)));

        assertThat(pageDTO.widgets.get(2).widgetId, is(WIDGET3_ID));
        assertThat(pageDTO.widgets.get(2).label, is("DateTime [29.07.2019 18:12]"));
        assertThat(pageDTO.widgets.get(2).labelcolor, nullValue());
        assertThat(pageDTO.widgets.get(2).valuecolor, nullValue());
        assertThat(pageDTO.widgets.get(2).state, nullValue());
        assertThat(pageDTO.widgets.get(2).item, notNullValue());
        assertThat(pageDTO.widgets.get(2).item.name, is(ITEM2_NAME));
        assertThat(pageDTO.widgets.get(2).item.state, is(DATETIME_VALUE));
    }

    private void configureItemRegistry() throws ItemNotFoundException {
        when(itemRegistry.getItem(ITEM_NAME)).thenReturn(item);
        when(itemRegistry.getItem(ITEM2_NAME)).thenReturn(item2);
        when(itemRegistry.getItem(VISIBILITY_RULE_ITEM_NAME)).thenReturn(visibilityRuleItem);
        when(itemRegistry.getItem(LABEL_COLOR_ITEM_NAME)).thenReturn(labelColorItem);
        when(itemRegistry.getItem(VALUE_COLOR_ITEM_NAME)).thenReturn(valueColorItem);
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
        when(w1.getIcon()).thenReturn(null);

        // add visibility rules to the mock widget:
        VisibilityRule visibilityRule = mock(VisibilityRule.class);
        when(visibilityRule.getItem()).thenReturn(VISIBILITY_RULE_ITEM_NAME);
        when(visibilityRule.getSign()).thenReturn(null);
        when(visibilityRule.getCondition()).thenReturn("==");
        when(visibilityRule.getState()).thenReturn("ON");
        BasicEList<VisibilityRule> visibilityRules = new BasicEList<>(1);
        visibilityRules.add(visibilityRule);
        when(w1.getVisibility()).thenReturn(visibilityRules);

        // add label color conditions to the item:
        ColorArray labelColor = mock(ColorArray.class);
        when(labelColor.getItem()).thenReturn(LABEL_COLOR_ITEM_NAME);
        when(labelColor.getSign()).thenReturn(null);
        when(labelColor.getCondition()).thenReturn("==");
        when(labelColor.getState()).thenReturn("ON");
        when(labelColor.getArg()).thenReturn("GREEN");
        EList<ColorArray> labelColors = new BasicEList<>();
        labelColors.add(labelColor);
        when(w1.getLabelColor()).thenReturn(labelColors);

        // add value color conditions to the item:
        ColorArray valueColor = mock(ColorArray.class);
        when(valueColor.getItem()).thenReturn(VALUE_COLOR_ITEM_NAME);
        when(valueColor.getSign()).thenReturn(null);
        when(valueColor.getCondition()).thenReturn("==");
        when(valueColor.getState()).thenReturn("ON");
        when(valueColor.getArg()).thenReturn("BLUE");
        EList<ColorArray> valueColors = new BasicEList<>();
        valueColors.add(valueColor);
        when(w1.getValueColor()).thenReturn(valueColors);

        visibilityRules = new BasicEList<>();
        labelColors = new BasicEList<>();
        valueColors = new BasicEList<>();
        EList<Mapping> mappings = new BasicEList<>();

        Switch w2 = mock(Switch.class);
        EClass switchEClass = mock(EClass.class);
        when(switchEClass.getName()).thenReturn("switch");
        when(switchEClass.getInstanceTypeName()).thenReturn("org.eclipse.smarthome.model.sitemap.Switch");
        when(w2.eClass()).thenReturn(switchEClass);
        when(w2.getLabel()).thenReturn(WIDGET2_LABEL);
        when(w2.getItem()).thenReturn(ITEM_NAME);
        when(w2.getIcon()).thenReturn(null);
        when(w2.getVisibility()).thenReturn(visibilityRules);
        when(w2.getLabelColor()).thenReturn(labelColors);
        when(w2.getValueColor()).thenReturn(valueColors);
        when(w2.getMappings()).thenReturn(mappings);

        Widget w3 = mock(Widget.class);
        EClass textEClass = mock(EClass.class);
        when(textEClass.getName()).thenReturn("text");
        when(textEClass.getInstanceTypeName()).thenReturn("org.eclipse.smarthome.model.sitemap.Text");
        when(w3.eClass()).thenReturn(textEClass);
        when(w3.getLabel()).thenReturn(WIDGET3_LABEL);
        when(w3.getItem()).thenReturn(ITEM2_NAME);
        when(w3.getIcon()).thenReturn(null);
        when(w3.getVisibility()).thenReturn(visibilityRules);
        when(w3.getLabelColor()).thenReturn(labelColors);
        when(w3.getValueColor()).thenReturn(valueColors);

        BasicEList<Widget> widgets = new BasicEList<>(3);
        widgets.add(w1);
        widgets.add(w2);
        widgets.add(w3);
        return widgets;
    }

    private void configureSitemapMock() {
        when(defaultSitemap.getName()).thenReturn(SITEMAP_NAME);
        when(defaultSitemap.getLabel()).thenReturn(SITEMAP_TITLE);
        when(defaultSitemap.getIcon()).thenReturn("");
        when(defaultSitemap.getChildren()).thenReturn(widgets);
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

    private class TestItemUIRegistryImpl extends ItemUIRegistryImpl {

        public TestItemUIRegistryImpl() {
            super();
        }

        @Override
        public @NonNull String getWidgetId(@NonNull Widget widget) {
            if (widget == widgets.get(0)) {
                return WIDGET1_ID;
            } else if (widget == widgets.get(1)) {
                return WIDGET2_ID;
            } else if (widget == widgets.get(2)) {
                return WIDGET3_ID;
            } else {
                return "";
            }
        }
    }
}
