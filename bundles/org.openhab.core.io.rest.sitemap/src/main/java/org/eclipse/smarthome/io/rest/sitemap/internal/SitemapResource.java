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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent.Builder;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTOMapper;
import org.eclipse.smarthome.io.rest.sitemap.SitemapSubscriptionService;
import org.eclipse.smarthome.io.rest.sitemap.internal.SitemapSubscriptionServiceImpl.SitemapSubscriptionCallback;
import org.eclipse.smarthome.io.rest.sitemap.internal.dto.MappingDTO;
import org.eclipse.smarthome.io.rest.sitemap.internal.dto.PageDTO;
import org.eclipse.smarthome.io.rest.sitemap.internal.dto.SitemapDTO;
import org.eclipse.smarthome.io.rest.sitemap.internal.dto.WidgetDTO;
import org.eclipse.smarthome.io.rest.sitemap.internal.events.SitemapEvent;
import org.eclipse.smarthome.model.sitemap.Chart;
import org.eclipse.smarthome.model.sitemap.Frame;
import org.eclipse.smarthome.model.sitemap.Image;
import org.eclipse.smarthome.model.sitemap.LinkableWidget;
import org.eclipse.smarthome.model.sitemap.Mapping;
import org.eclipse.smarthome.model.sitemap.Mapview;
import org.eclipse.smarthome.model.sitemap.Selection;
import org.eclipse.smarthome.model.sitemap.Setpoint;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.eclipse.smarthome.model.sitemap.Slider;
import org.eclipse.smarthome.model.sitemap.Switch;
import org.eclipse.smarthome.model.sitemap.Video;
import org.eclipse.smarthome.model.sitemap.Webview;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * <p>
 * This class acts as a REST resource for sitemaps and provides different methods to interact with them, like retrieving
 * a list of all available sitemaps or just getting the widgets of a single page.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson
 * @author Yordan Zhelev - Added Swagger annotations
 */
@Path(SitemapResource.PATH_SITEMAPS)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(value = SitemapResource.PATH_SITEMAPS)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class SitemapResource implements SitemapSubscriptionCallback {

    private final Logger logger = LoggerFactory.getLogger(SitemapResource.class);

    public static final String PATH_SITEMAPS = "sitemaps";
    private static final String SEGMENT_EVENTS = "events";
    private static final String X_ACCEL_BUFFERING_HEADER = "X-Accel-Buffering";

    @Reference
    protected @NonNullByDefault({}) ItemUIRegistry itemUIRegistry;

    @Reference
    protected @NonNullByDefault({}) SitemapSubscriptionService subscriptions;

    @Reference
    protected @NonNullByDefault({}) LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected final List<SitemapProvider> sitemapProviders = new ArrayList<>();

    private final Map<String, SitemapEventOutput> eventOutputs = new TreeMap<>();

    private @NonNullByDefault({}) Builder eventBuilder;
    private @NonNullByDefault({}) SseBroadcaster broadcaster;

    @Context
    public void setSse(Sse sse) {
        eventBuilder = sse.newEventBuilder();
        broadcaster = sse.newBroadcaster();
        broadcaster.onClose(event -> {
            if (event instanceof SitemapEventOutput) {
                SitemapEventOutput sitemapEvent = (SitemapEventOutput) event;
                logger.debug("SSE connection for subscription {} has been closed.", sitemapEvent.getSubscriptionId());
                subscriptions.removeSubscription(sitemapEvent.getSubscriptionId());
                eventOutputs.remove(sitemapEvent.getSubscriptionId());
            }
        });
        broadcaster.onError((c, e) -> {
        });
    }

    @Activate
    protected void activate() {
    }

    @Deactivate
    protected void deactivate() {
        broadcaster.close();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available sitemaps.", response = SitemapDTO.class, responseContainer = "Collection")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Collection<SitemapDTO> getSitemaps(@Context UriInfo uriInfo) {
        return getSitemapBeans(uriInfo.getAbsolutePathBuilder().build());
    }

    @GET
    @Path("/{sitemapname: [a-zA-Z_0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get sitemap by name.", response = SitemapDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public SitemapDTO getSitemapData(@Context UriInfo uriInfo, @Context HttpHeaders headers,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("sitemapname") @ApiParam(value = "sitemap name") String sitemapname,
            @QueryParam("type") String type, @QueryParam("jsoncallback") @DefaultValue("callback") String callback,
            @QueryParam("includeHidden") @ApiParam(value = "include hidden widgets", required = false) boolean includeHiddenWidgets) {
        final Locale locale = localeService.getLocale(language);
        Sitemap sitemap = getSitemap(sitemapname);
        if (sitemap != null) {
            return createSitemapBean(sitemapname, sitemap, uriInfo.getBaseUriBuilder().build(), locale,
                    includeHiddenWidgets);
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{sitemapname: [a-zA-Z_0-9]*}/{pageid: [a-zA-Z_0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Polls the data for a sitemap.", response = PageDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Sitemap with requested name does not exist or page does not exist, or page refers to a non-linkable widget"),
            @ApiResponse(code = 400, message = "Invalid subscription id has been provided.") })
    public PageDTO getPageData(@Context UriInfo uriInfo, @Context HttpHeaders headers,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @PathParam("sitemapname") @ApiParam(value = "sitemap name") String sitemapname,
            @PathParam("pageid") @ApiParam(value = "page id") String pageId,
            @QueryParam("subscriptionid") @ApiParam(value = "subscriptionid", required = false) @Nullable String subscriptionId,
            @QueryParam("includeHidden") @ApiParam(value = "include hidden widgets", required = false) boolean includeHiddenWidgets) {
        final Locale locale = localeService.getLocale(language);

        if (subscriptionId != null) {
            try {
                subscriptions.setPageId(subscriptionId, sitemapname, pageId);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e);
            }
        }

        boolean timeout = false;
        return getPageBean(sitemapname, pageId, uriInfo.getBaseUriBuilder().build(), locale, timeout,
                includeHiddenWidgets);
    }

    /**
     * Creates a subscription for the stream of sitemap events.
     *
     * @return a subscription id
     */
    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path(SEGMENT_EVENTS + "/subscribe")
    @ApiOperation(value = "Creates a sitemap event subscription.")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Subscription created."),
            @ApiResponse(code = 503, message = "Subscriptions limit reached.") })
    public String createEventSubscription(@Context HttpServletRequest request, @Context UriInfo uriInfo,
            @Context SseEventSink es) {
        String subscriptionId = subscriptions.createSubscription(this);
        if (subscriptionId == null) {
            throw new ServiceUnavailableException("Max number of subscriptions is reached.");
        }
        final SitemapEventOutput eventOutput = new SitemapEventOutput(es, subscriptions, subscriptionId);
        broadcaster.register(eventOutput);
        eventOutputs.put(subscriptionId, eventOutput);
        URI uri = uriInfo.getBaseUriBuilder().path(PATH_SITEMAPS).path(SEGMENT_EVENTS).path(subscriptionId).build();
        logger.debug("Client from IP {} requested new subscription => got id {}.", request.getRemoteAddr(),
                subscriptionId);
        return uri.toString();
    }

    /**
     * Subscribes the connecting client to the stream of sitemap events.
     *
     * @return {@link EventOutput} object associated with the incoming
     *         connection.
     */
    @SuppressWarnings("null")
    @GET
    @Path(SEGMENT_EVENTS + "/{subscriptionid: [a-zA-Z_0-9-]*}/")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @ApiOperation(value = "Get sitemap events.", response = SseEventSink.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Page not linked to the subscription."),
            @ApiResponse(code = 404, message = "Subscription not found.") })
    public SseEventSink getSitemapEvents(@Context HttpServletResponse response,
            @PathParam("subscriptionid") @ApiParam(value = "subscription id") String subscriptionId,
            @QueryParam("sitemap") @ApiParam(value = "sitemap name", required = false) @Nullable String sitemapname,
            @QueryParam("pageid") @ApiParam(value = "page id", required = false) @Nullable String pageId) {
        SitemapEventOutput eventOutput = eventOutputs.get(subscriptionId);
        if (!subscriptions.exists(subscriptionId) || eventOutput == null) {
            throw new NotFoundException("Subscription id " + subscriptionId + " does not exist.");
        }
        if (sitemapname != null && pageId != null) {
            subscriptions.setPageId(subscriptionId, sitemapname, pageId);
        }
        if (subscriptions.getSitemapName(subscriptionId) == null || subscriptions.getPageId(subscriptionId) == null) {
            throw new BadRequestException("Subscription id " + subscriptionId + " s not yet linked to a sitemap/page.");
        }
        logger.debug("Client requested sitemap event stream for subscription {}.", subscriptionId);

        // Disables proxy buffering when using an nginx http server proxy for this response.
        response.addHeader(X_ACCEL_BUFFERING_HEADER, "no");
        response.addHeader(HttpHeaders.CONTENT_ENCODING, "identity");
        return eventOutput;
    }

    private PageDTO getPageBean(String sitemapName, String pageId, URI uri, Locale locale, boolean timeout,
            boolean includeHidden) {
        Sitemap sitemap = getSitemap(sitemapName);
        if (sitemap == null) {
            throw new NotFoundException();
        }
        if (pageId.equals(sitemap.getName())) {
            EList<Widget> children = itemUIRegistry.getChildren(sitemap);
            return createPageBean(sitemapName, sitemap.getLabel(), sitemap.getIcon(), sitemap.getName(), children,
                    false, isLeaf(children), uri, locale, timeout, includeHidden);
        }

        Widget pageWidget = itemUIRegistry.getWidget(sitemap, pageId);
        if (pageWidget instanceof LinkableWidget) {
            EList<Widget> children = itemUIRegistry.getChildren((LinkableWidget) pageWidget);
            PageDTO pageBean = createPageBean(sitemapName, itemUIRegistry.getLabel(pageWidget),
                    itemUIRegistry.getCategory(pageWidget), pageId, children, false, isLeaf(children), uri, locale,
                    timeout, includeHidden);
            EObject parentPage = pageWidget.eContainer();
            while (parentPage instanceof Frame) {
                parentPage = parentPage.eContainer();
            }
            if (parentPage instanceof Widget) {
                String parentId = itemUIRegistry.getWidgetId((Widget) parentPage);
                pageBean.parent = getPageBean(sitemapName, parentId, uri, locale, timeout, includeHidden);
                pageBean.parent.widgets = null;
                pageBean.parent.parent = null;
            } else if (parentPage instanceof Sitemap) {
                pageBean.parent = getPageBean(sitemapName, sitemap.getName(), uri, locale, timeout, includeHidden);
                pageBean.parent.widgets = null;
            }
            return pageBean;
        } else {
            if (logger.isDebugEnabled()) {
                if (pageWidget == null) {
                    logger.debug("Received HTTP GET request at '{}' for the unknown page id '{}'.", uri, pageId);
                } else {
                    logger.debug(
                            "Received HTTP GET request at '{}' for the page id '{}'. "
                                    + "This id refers to a non-linkable widget and is therefore no valid page id.",
                            uri, pageId);
                }
            }
            throw new NotFoundException();
        }
    }

    public Collection<SitemapDTO> getSitemapBeans(URI uri) {
        Collection<SitemapDTO> beans = new LinkedList<SitemapDTO>();
        Set<String> names = new HashSet<>();
        for (SitemapProvider provider : sitemapProviders) {
            for (String modelName : provider.getSitemapNames()) {
                Sitemap sitemap = provider.getSitemap(modelName);
                if (sitemap == null) {
                    continue;
                }
                if (names.contains(modelName)) {
                    logger.warn("Found duplicate sitemap name '{}' - ignoring it. Please check your configuration.",
                            modelName);
                    continue;
                }
                names.add(modelName);
                SitemapDTO bean = new SitemapDTO();
                bean.name = modelName;
                bean.icon = sitemap.getIcon();
                bean.label = sitemap.getLabel();
                bean.link = uri.resolve(bean.name).toASCIIString();
                bean.homepage = new PageDTO();
                bean.homepage.link = bean.link + "/" + sitemap.getName();
                beans.add(bean);

            }
        }
        return beans;
    }

    private SitemapDTO createSitemapBean(String sitemapName, Sitemap sitemap, URI uri, Locale locale,
            boolean includeHiddenWidgets) {
        SitemapDTO bean = new SitemapDTO();

        bean.name = sitemapName;
        bean.icon = sitemap.getIcon();
        bean.label = sitemap.getLabel();

        bean.link = uri.resolve(SitemapResource.PATH_SITEMAPS).resolve(bean.name).toASCIIString();
        bean.homepage = createPageBean(sitemap.getName(), sitemap.getLabel(), sitemap.getIcon(), sitemap.getName(),
                itemUIRegistry.getChildren(sitemap), true, false, uri, locale, false, includeHiddenWidgets);
        return bean;
    }

    private PageDTO createPageBean(String sitemapName, @Nullable String title, @Nullable String icon, String pageId,
            @Nullable EList<Widget> children, boolean drillDown, boolean isLeaf, URI uri, Locale locale,
            boolean timeout, boolean includeHiddenWidgets) {
        PageDTO bean = new PageDTO();
        bean.timeout = timeout;
        bean.id = pageId;
        bean.title = title;
        bean.icon = icon;
        bean.leaf = isLeaf;
        bean.link = uri.resolve(SitemapResource.PATH_SITEMAPS).resolve(sitemapName).resolve(pageId).toASCIIString();
        if (children != null) {
            for (Widget widget : children) {
                String widgetId = itemUIRegistry.getWidgetId(widget);
                WidgetDTO subWidget = createWidgetBean(sitemapName, widget, drillDown, uri, widgetId, locale,
                        includeHiddenWidgets);
                if (subWidget != null) {
                    bean.widgets.add(subWidget);
                }
            }
        } else {
            bean.widgets = null;
        }
        return bean;
    }

    private @Nullable WidgetDTO createWidgetBean(String sitemapName, Widget widget, boolean drillDown, URI uri,
            String widgetId, Locale locale, boolean evenIfHidden) {
        // Test visibility
        if (!evenIfHidden && !itemUIRegistry.getVisiblity(widget)) {
            return null;
        }

        WidgetDTO bean = new WidgetDTO();
        if (widget.getItem() != null) {
            try {
                Item item = itemUIRegistry.getItem(widget.getItem());
                String widgetTypeName = widget.eClass().getInstanceTypeName()
                        .substring(widget.eClass().getInstanceTypeName().lastIndexOf(".") + 1);
                boolean isMapview = "mapview".equalsIgnoreCase(widgetTypeName);
                Predicate<Item> itemFilter = (i -> i.getType().equals(CoreItemFactory.LOCATION));
                bean.item = EnrichedItemDTOMapper.map(item, isMapview, itemFilter, uri, locale);
                State state = itemUIRegistry.getState(widget);
                bean.state = state != null ? state.toFullString() : null;
                // In case the widget state is identical to the item state, its value is set to null.
                if (bean.state != null && bean.state.equals(bean.item.state)) {
                    bean.state = null;
                }
            } catch (ItemNotFoundException e) {
                logger.debug("{}", e.getMessage());
            }
        }
        bean.widgetId = widgetId;
        bean.icon = itemUIRegistry.getCategory(widget);
        bean.labelcolor = itemUIRegistry.getLabelColor(widget);
        bean.valuecolor = itemUIRegistry.getValueColor(widget);
        bean.label = itemUIRegistry.getLabel(widget);
        bean.type = widget.eClass().getName();
        bean.visibility = itemUIRegistry.getVisiblity(widget);
        if (widget instanceof LinkableWidget) {
            LinkableWidget linkableWidget = (LinkableWidget) widget;
            EList<Widget> children = itemUIRegistry.getChildren(linkableWidget);
            if (widget instanceof Frame) {
                for (Widget child : children) {
                    String wID = itemUIRegistry.getWidgetId(child);
                    WidgetDTO subWidget = createWidgetBean(sitemapName, child, drillDown, uri, wID, locale,
                            evenIfHidden);
                    if (subWidget != null) {
                        bean.widgets.add(subWidget);
                    }
                }
            } else if (children.size() > 0) {
                String pageName = itemUIRegistry.getWidgetId(linkableWidget);
                bean.linkedPage = createPageBean(sitemapName, itemUIRegistry.getLabel(widget),
                        itemUIRegistry.getCategory(widget), pageName, drillDown ? children : null, drillDown,
                        isLeaf(children), uri, locale, false, evenIfHidden);
            }
        }
        if (widget instanceof Switch) {
            Switch switchWidget = (Switch) widget;
            for (Mapping mapping : switchWidget.getMappings()) {
                MappingDTO mappingBean = new MappingDTO();
                mappingBean.command = mapping.getCmd();
                mappingBean.label = mapping.getLabel();
                bean.mappings.add(mappingBean);
            }
        }
        if (widget instanceof Selection) {
            Selection selectionWidget = (Selection) widget;
            for (Mapping mapping : selectionWidget.getMappings()) {
                MappingDTO mappingBean = new MappingDTO();
                mappingBean.command = mapping.getCmd();
                mappingBean.label = mapping.getLabel();
                bean.mappings.add(mappingBean);
            }
        }
        if (widget instanceof Slider) {
            Slider sliderWidget = (Slider) widget;
            bean.sendFrequency = sliderWidget.getFrequency();
            bean.switchSupport = sliderWidget.isSwitchEnabled();
            bean.minValue = sliderWidget.getMinValue();
            bean.maxValue = sliderWidget.getMaxValue();
            bean.step = sliderWidget.getStep();
        }
        if (widget instanceof org.eclipse.smarthome.model.sitemap.List) {
            org.eclipse.smarthome.model.sitemap.List listWidget = (org.eclipse.smarthome.model.sitemap.List) widget;
            bean.separator = listWidget.getSeparator();
        }
        if (widget instanceof Image) {
            bean.url = buildProxyUrl(sitemapName, widget, uri);
            Image imageWidget = (Image) widget;
            if (imageWidget.getRefresh() > 0) {
                bean.refresh = imageWidget.getRefresh();
            }
        }
        if (widget instanceof Video) {
            Video videoWidget = (Video) widget;
            if (videoWidget.getEncoding() != null) {
                bean.encoding = videoWidget.getEncoding();
            }
            if (videoWidget.getEncoding() != null && videoWidget.getEncoding().toLowerCase().contains("hls")) {
                bean.url = videoWidget.getUrl();
            } else {
                bean.url = buildProxyUrl(sitemapName, videoWidget, uri);
            }
        }
        if (widget instanceof Webview) {
            Webview webViewWidget = (Webview) widget;
            bean.url = webViewWidget.getUrl();
            bean.height = webViewWidget.getHeight();
        }
        if (widget instanceof Mapview) {
            Mapview mapViewWidget = (Mapview) widget;
            bean.height = mapViewWidget.getHeight();
        }
        if (widget instanceof Chart) {
            Chart chartWidget = (Chart) widget;
            bean.service = chartWidget.getService();
            bean.period = chartWidget.getPeriod();
            bean.legend = chartWidget.getLegend();
            if (chartWidget.getRefresh() > 0) {
                bean.refresh = chartWidget.getRefresh();
            }
        }
        if (widget instanceof Setpoint) {
            Setpoint setpointWidget = (Setpoint) widget;
            bean.minValue = setpointWidget.getMinValue();
            bean.maxValue = setpointWidget.getMaxValue();
            bean.step = setpointWidget.getStep();
        }
        return bean;
    }

    private String buildProxyUrl(String sitemapName, Widget widget, URI uri) {
        String wId = itemUIRegistry.getWidgetId(widget);
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            sb.append(":").append(uri.getPort());
        }
        sb.append("/proxy?sitemap=").append(sitemapName).append(".sitemap&widgetId=").append(wId);
        return sb.toString();
    }

    private boolean isLeaf(EList<Widget> children) {
        for (Widget w : children) {
            if (w instanceof Frame) {
                if (isLeaf(((Frame) w).getChildren())) {
                    return false;
                }
            } else if (w instanceof LinkableWidget) {
                LinkableWidget linkableWidget = (LinkableWidget) w;
                if (itemUIRegistry.getChildren(linkableWidget).size() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private @Nullable Sitemap getSitemap(String sitemapname) {
        for (SitemapProvider provider : sitemapProviders) {
            Sitemap sitemap = provider.getSitemap(sitemapname);
            if (sitemap != null) {
                return sitemap;
            }
        }

        return null;
    }

    @Override
    public void onEvent(SitemapEvent event) {
        OutboundSseEvent outboundEvent = eventBuilder.name("event").mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(event).build();
        broadcaster.broadcast(outboundEvent);
    }

    @Override
    public void onRelease(String subscriptionId) {
        logger.debug("SSE connection for subscription {} has been released.", subscriptionId);
        eventOutputs.remove(subscriptionId);
    }
}