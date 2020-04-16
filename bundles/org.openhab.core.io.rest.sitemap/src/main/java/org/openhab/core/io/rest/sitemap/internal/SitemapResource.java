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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.BroadcasterListener;
import org.glassfish.jersey.server.ChunkedOutput;
import org.openhab.core.auth.Role;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.item.EnrichedItemDTOMapper;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService.SitemapSubscriptionCallback;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.model.sitemap.sitemap.Chart;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.Frame;
import org.openhab.core.model.sitemap.sitemap.Image;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.List;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Mapview;
import org.openhab.core.model.sitemap.sitemap.Selection;
import org.openhab.core.model.sitemap.sitemap.Setpoint;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Video;
import org.openhab.core.model.sitemap.sitemap.VisibilityRule;
import org.openhab.core.model.sitemap.sitemap.Webview;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

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
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Yordan Zhelev - Added Swagger annotations
 */
@Component(service = RESTResource.class)
@Path(SitemapResource.PATH_SITEMAPS)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(value = SitemapResource.PATH_SITEMAPS)
public class SitemapResource implements RESTResource, SitemapSubscriptionCallback, BroadcasterListener<OutboundEvent> {

    private final Logger logger = LoggerFactory.getLogger(SitemapResource.class);

    public static final String PATH_SITEMAPS = "sitemaps";
    private static final String SEGMENT_EVENTS = "events";
    private static final String X_ACCEL_BUFFERING_HEADER = "X-Accel-Buffering";

    private static final long TIMEOUT_IN_MS = 30000;

    private SseBroadcaster broadcaster;

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    private ItemUIRegistry itemUIRegistry;

    private SitemapSubscriptionService subscriptions;

    private LocaleService localeService;

    private final java.util.List<SitemapProvider> sitemapProviders = new ArrayList<>();

    private final Map<String, EventOutput> eventOutputs = new MapMaker().weakValues().makeMap();

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private ScheduledFuture<?> cleanSubscriptionsJob;

    @Activate
    protected void activate() {
        broadcaster = new SseBroadcaster();
        broadcaster.add(this);

        // The clean SSE subscriptions job sends an ALIVE event to all subscribers. This will trigger
        // an exception when the subscriber is dead, leading to the release of the SSE subscription
        // on server side.
        // In practice, the exception occurs only after the sending of a second ALIVE event. So this
        // will require two runs of the job to release an SSE subscription.
        // The clean SSE subscriptions job is run every 5 minutes.
        cleanSubscriptionsJob = scheduler.scheduleAtFixedRate(() -> {
            logger.debug("Run clean SSE subscriptions job");
            if (subscriptions != null) {
                subscriptions.checkAliveClients();
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        if (cleanSubscriptionsJob != null && !cleanSubscriptionsJob.isCancelled()) {
            logger.debug("Cancel clean SSE subscriptions job");
            cleanSubscriptionsJob.cancel(true);
            cleanSubscriptionsJob = null;
        }
        broadcaster.remove(this);
        broadcaster = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = itemUIRegistry;
    }

    public void unsetItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setSitemapSubscriptionService(SitemapSubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    public void unsetSitemapSubscriptionService(SitemapSubscriptionService subscriptions) {
        this.subscriptions = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSitemapProvider(SitemapProvider provider) {
        sitemapProviders.add(provider);
    }

    public void removeSitemapProvider(SitemapProvider provider) {
        sitemapProviders.remove(provider);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available sitemaps.", response = SitemapDTO.class, responseContainer = "Collection")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getSitemaps() {
        logger.debug("Received HTTP GET request from IP {} at '{}'", request.getRemoteAddr(), uriInfo.getPath());
        Object responseObject = getSitemapBeans(uriInfo.getAbsolutePathBuilder().build());
        return Response.ok(responseObject).build();
    }

    @GET
    @Path("/{sitemapname: [a-zA-Z_0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get sitemap by name.", response = SitemapDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getSitemapData(@Context HttpHeaders headers,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("sitemapname") @ApiParam(value = "sitemap name") String sitemapname,
            @QueryParam("type") String type, @QueryParam("jsoncallback") @DefaultValue("callback") String callback,
            @QueryParam("includeHidden") @ApiParam(value = "include hidden widgets", required = false) boolean includeHiddenWidgets) {
        final Locale locale = localeService.getLocale(language);
        logger.debug("Received HTTP GET request from IP {} at '{}' for media type '{}'.", request.getRemoteAddr(),
                uriInfo.getPath(), type);
        Object responseObject = getSitemapBean(sitemapname, uriInfo.getBaseUriBuilder().build(), locale,
                includeHiddenWidgets);
        return Response.ok(responseObject).build();
    }

    @GET
    @Path("/{sitemapname: [a-zA-Z_0-9]+}/{pageid: [a-zA-Z_0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Polls the data for a sitemap.", response = PageDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Sitemap with requested name does not exist or page does not exist, or page refers to a non-linkable widget"),
            @ApiResponse(code = 400, message = "Invalid subscription id has been provided.") })
    public Response getPageData(@Context HttpHeaders headers,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("sitemapname") @ApiParam(value = "sitemap name") String sitemapname,
            @PathParam("pageid") @ApiParam(value = "page id") String pageId,
            @QueryParam("subscriptionid") @ApiParam(value = "subscriptionid", required = false) String subscriptionId,
            @QueryParam("includeHidden") @ApiParam(value = "include hidden widgets", required = false) boolean includeHiddenWidgets) {
        final Locale locale = localeService.getLocale(language);
        logger.debug("Received HTTP GET request from IP {} at '{}'", request.getRemoteAddr(), uriInfo.getPath());

        if (subscriptionId != null) {
            try {
                subscriptions.setPageId(subscriptionId, sitemapname, pageId);
            } catch (IllegalArgumentException e) {
                return JSONResponse.createErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
            }
        }

        boolean timeout = false;
        if (headers.getRequestHeader("X-Atmosphere-Transport") != null) {
            // Make the REST-API pseudo-compatible with openHAB 1.x
            // The client asks Atmosphere for server push functionality,
            // so we do a simply listening for changes on the appropriate items
            // The blocking has a timeout of 30 seconds. If this timeout is reached,
            // we notice this information in the response object.
            timeout = blockUnlessChangeOccurs(sitemapname, pageId);
        }
        PageDTO responseObject = getPageBean(sitemapname, pageId, uriInfo.getBaseUriBuilder().build(), locale, timeout,
                includeHiddenWidgets);
        return Response.ok(responseObject).build();
    }

    /**
     * Creates a subscription for the stream of sitemap events.
     *
     * @return a subscription id
     */
    @POST
    @Path(SEGMENT_EVENTS + "/subscribe")
    @ApiOperation(value = "Creates a sitemap event subscription.")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Subscription created."),
            @ApiResponse(code = 503, message = "Subscriptions limit reached.") })
    public Object createEventSubscription() {
        String subscriptionId = subscriptions.createSubscription(this);
        if (subscriptionId == null) {
            return JSONResponse.createResponse(Status.SERVICE_UNAVAILABLE, null,
                    "Max number of subscriptions is reached.");
        }
        final EventOutput eventOutput = new SitemapEventOutput(subscriptions, subscriptionId);
        broadcaster.add(eventOutput);
        eventOutputs.put(subscriptionId, eventOutput);
        URI uri = uriInfo.getBaseUriBuilder().path(PATH_SITEMAPS).path(SEGMENT_EVENTS).path(subscriptionId).build();
        logger.debug("Client from IP {} requested new subscription => got id {}.", request.getRemoteAddr(),
                subscriptionId);
        return Response.created(uri);
    }

    /**
     * Subscribes the connecting client to the stream of sitemap events.
     *
     * @return {@link EventOutput} object associated with the incoming
     *         connection.
     */
    @GET
    @Path(SEGMENT_EVENTS + "/{subscriptionid: [a-zA-Z_0-9-]+}/")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @ApiOperation(value = "Get sitemap events.", response = EventOutput.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Page not linked to the subscription."),
            @ApiResponse(code = 404, message = "Subscription not found.") })
    public Object getSitemapEvents(
            @PathParam("subscriptionid") @ApiParam(value = "subscription id") String subscriptionId,
            @QueryParam("sitemap") @ApiParam(value = "sitemap name", required = false) String sitemapname,
            @QueryParam("pageid") @ApiParam(value = "page id", required = false) String pageId) {
        EventOutput eventOutput = eventOutputs.get(subscriptionId);
        if (!subscriptions.exists(subscriptionId) || eventOutput == null) {
            return JSONResponse.createResponse(Status.NOT_FOUND, null,
                    "Subscription id " + subscriptionId + " does not exist.");
        }
        if (sitemapname != null && pageId != null) {
            subscriptions.setPageId(subscriptionId, sitemapname, pageId);
        }
        if (subscriptions.getSitemapName(subscriptionId) == null || subscriptions.getPageId(subscriptionId) == null) {
            return JSONResponse.createResponse(Status.BAD_REQUEST, null,
                    "Subscription id " + subscriptionId + " is not yet linked to a sitemap/page.");
        }
        logger.debug("Client from IP {} requested sitemap event stream for subscription {}.", request.getRemoteAddr(),
                subscriptionId);

        // Disables proxy buffering when using an nginx http server proxy for this response.
        // This allows you to not disable proxy buffering in nginx and still have working sse
        response.addHeader(X_ACCEL_BUFFERING_HEADER, "no");

        return eventOutput;
    }

    private PageDTO getPageBean(String sitemapName, String pageId, URI uri, Locale locale, boolean timeout,
            boolean includeHidden) {
        Sitemap sitemap = getSitemap(sitemapName);
        if (sitemap != null) {
            if (pageId.equals(sitemap.getName())) {
                EList<Widget> children = itemUIRegistry.getChildren(sitemap);
                return createPageBean(sitemapName, sitemap.getLabel(), sitemap.getIcon(), sitemap.getName(), children,
                        false, isLeaf(children), uri, locale, timeout, includeHidden);
            } else {
                Widget pageWidget = itemUIRegistry.getWidget(sitemap, pageId);
                if (pageWidget instanceof LinkableWidget) {
                    EList<Widget> children = itemUIRegistry.getChildren((LinkableWidget) pageWidget);
                    PageDTO pageBean = createPageBean(sitemapName, itemUIRegistry.getLabel(pageWidget),
                            itemUIRegistry.getCategory(pageWidget), pageId, children, false, isLeaf(children), uri,
                            locale, timeout, includeHidden);
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
                        pageBean.parent = getPageBean(sitemapName, sitemap.getName(), uri, locale, timeout,
                                includeHidden);
                        pageBean.parent.widgets = null;
                    }
                    return pageBean;
                } else {
                    if (logger.isDebugEnabled()) {
                        if (pageWidget == null) {
                            logger.debug("Received HTTP GET request at '{}' for the unknown page id '{}'.", uri,
                                    pageId);
                        } else {
                            logger.debug("Received HTTP GET request at '{}' for the page id '{}'. "
                                    + "This id refers to a non-linkable widget and is therefore no valid page id.", uri,
                                    pageId);
                        }
                    }
                    throw new WebApplicationException(404);
                }
            }
        } else {
            logger.info("Received HTTP GET request at '{}' for the unknown sitemap '{}'.", uri, sitemapName);
            throw new WebApplicationException(404);
        }
    }

    public Collection<SitemapDTO> getSitemapBeans(URI uri) {
        Collection<SitemapDTO> beans = new LinkedList<>();
        Set<String> names = new HashSet<>();
        logger.debug("Received HTTP GET request at '{}'.", UriBuilder.fromUri(uri).build().toASCIIString());
        for (SitemapProvider provider : sitemapProviders) {
            for (String modelName : provider.getSitemapNames()) {
                Sitemap sitemap = provider.getSitemap(modelName);
                if (sitemap != null) {
                    if (!names.contains(modelName)) {
                        names.add(modelName);
                        SitemapDTO bean = new SitemapDTO();
                        bean.name = modelName;
                        bean.icon = sitemap.getIcon();
                        bean.label = sitemap.getLabel();
                        bean.link = UriBuilder.fromUri(uri).path(bean.name).build().toASCIIString();
                        bean.homepage = new PageDTO();
                        bean.homepage.link = bean.link + "/" + sitemap.getName();
                        beans.add(bean);
                    } else {
                        logger.warn("Found duplicate sitemap name '{}' - ignoring it. Please check your configuration.",
                                modelName);
                    }
                }
            }
        }
        return beans;
    }

    private SitemapDTO getSitemapBean(String sitemapname, URI uri, Locale locale, boolean includeHiddenWidgets) {
        Sitemap sitemap = getSitemap(sitemapname);
        if (sitemap != null) {
            return createSitemapBean(sitemapname, sitemap, uri, locale, includeHiddenWidgets);
        } else {
            logger.info("Received HTTP GET request at '{}' for the unknown sitemap '{}'.", uriInfo.getPath(),
                    sitemapname);
            throw new WebApplicationException(404);
        }
    }

    private SitemapDTO createSitemapBean(String sitemapName, Sitemap sitemap, URI uri, Locale locale,
            boolean includeHiddenWidgets) {
        SitemapDTO bean = new SitemapDTO();

        bean.name = sitemapName;
        bean.icon = sitemap.getIcon();
        bean.label = sitemap.getLabel();

        bean.link = UriBuilder.fromUri(uri).path(SitemapResource.PATH_SITEMAPS).path(bean.name).build().toASCIIString();
        bean.homepage = createPageBean(sitemap.getName(), sitemap.getLabel(), sitemap.getIcon(), sitemap.getName(),
                itemUIRegistry.getChildren(sitemap), true, false, uri, locale, false, includeHiddenWidgets);
        return bean;
    }

    private PageDTO createPageBean(String sitemapName, String title, String icon, String pageId, EList<Widget> children,
            boolean drillDown, boolean isLeaf, URI uri, Locale locale, boolean timeout, boolean includeHiddenWidgets) {
        PageDTO bean = new PageDTO();
        bean.timeout = timeout;
        bean.id = pageId;
        bean.title = title;
        bean.icon = icon;
        bean.leaf = isLeaf;
        bean.link = UriBuilder.fromUri(uri).path(PATH_SITEMAPS).path(sitemapName).path(pageId).build().toASCIIString();
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

    private WidgetDTO createWidgetBean(String sitemapName, Widget widget, boolean drillDown, URI uri, String widgetId,
            Locale locale, boolean evenIfHidden) {
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
                Predicate<Item> itemFilter = (i -> CoreItemFactory.LOCATION.equals(i.getType()));
                bean.item = EnrichedItemDTOMapper.map(item, isMapview, itemFilter, UriBuilder.fromUri(uri).build(),
                        locale);
                bean.state = itemUIRegistry.getState(widget).toFullString();
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
            } else if (!children.isEmpty()) {
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
        if (widget instanceof List) {
            List listWidget = (List) widget;
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
                if (!itemUIRegistry.getChildren(linkableWidget).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private Sitemap getSitemap(String sitemapname) {
        for (SitemapProvider provider : sitemapProviders) {
            Sitemap sitemap = provider.getSitemap(sitemapname);
            if (sitemap != null) {
                return sitemap;
            }
        }

        return null;
    }

    private boolean blockUnlessChangeOccurs(String sitemapname, String pageId) {
        boolean timeout = false;
        Sitemap sitemap = getSitemap(sitemapname);
        if (sitemap != null) {
            if (pageId.equals(sitemap.getName())) {
                EList<Widget> children = itemUIRegistry.getChildren(sitemap);
                timeout = waitForChanges(children);
            } else {
                Widget pageWidget = itemUIRegistry.getWidget(sitemap, pageId);
                if (pageWidget instanceof LinkableWidget) {
                    EList<Widget> children = itemUIRegistry.getChildren((LinkableWidget) pageWidget);
                    timeout = waitForChanges(children);
                }
            }
        }
        return timeout;
    }

    /**
     * This method only returns when a change has occurred to any item on the
     * page to display or if the timeout is reached
     *
     * @param widgets
     *            the widgets of the page to observe
     * @return true if the timeout is reached
     */
    private boolean waitForChanges(EList<Widget> widgets) {
        long startTime = (new Date()).getTime();
        boolean timeout = false;
        BlockingStateChangeListener listener = new BlockingStateChangeListener();
        // let's get all items for these widgets
        Set<GenericItem> items = getAllItems(widgets);
        for (GenericItem item : items) {
            item.addStateChangeListener(listener);
        }
        while (!listener.hasChangeOccurred() && !timeout) {
            timeout = (new Date()).getTime() - startTime > TIMEOUT_IN_MS;
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                timeout = true;
                break;
            }
        }
        for (GenericItem item : items) {
            item.removeStateChangeListener(listener);
        }
        return timeout;
    }

    /**
     * Collects all items that are represented by a given list of widgets
     *
     * @param widgets
     *            the widget list to get the items for added to all bundles containing REST resources
     * @return all items that are represented by the list of widgets
     */
    private Set<GenericItem> getAllItems(EList<Widget> widgets) {
        Set<GenericItem> items = new HashSet<>();
        if (itemUIRegistry != null) {
            for (Widget widget : widgets) {
                // We skip the chart widgets having a refresh argument
                boolean skipWidget = false;
                if (widget instanceof Chart) {
                    Chart chartWidget = (Chart) widget;
                    skipWidget = chartWidget.getRefresh() > 0;
                }
                String itemName = widget.getItem();
                if (!skipWidget && itemName != null) {
                    try {
                        Item item = itemUIRegistry.getItem(itemName);
                        if (item instanceof GenericItem) {
                            items.add((GenericItem) item);
                        }
                    } catch (ItemNotFoundException e) {
                        // ignore
                    }
                }
                // Consider all items inside the frame
                if (widget instanceof Frame) {
                    items.addAll(getAllItems(((Frame) widget).getChildren()));
                }
                // Consider items involved in any visibility, labelcolor and valuecolor condition
                items.addAll(getItemsInVisibilityCond(widget.getVisibility()));
                items.addAll(getItemsInColorCond(widget.getLabelColor()));
                items.addAll(getItemsInColorCond(widget.getValueColor()));
            }
        }
        return items;
    }

    private Set<GenericItem> getItemsInVisibilityCond(EList<VisibilityRule> ruleList) {
        Set<GenericItem> items = new HashSet<>();
        for (VisibilityRule rule : ruleList) {
            String itemName = rule.getItem();
            if (itemName != null) {
                try {
                    Item item = itemUIRegistry.getItem(itemName);
                    if (item instanceof GenericItem) {
                        items.add((GenericItem) item);
                    }
                } catch (ItemNotFoundException e) {
                    // ignore
                }
            }
        }
        return items;
    }

    private Set<GenericItem> getItemsInColorCond(EList<ColorArray> colorList) {
        Set<GenericItem> items = new HashSet<>();
        for (ColorArray color : colorList) {
            String itemName = color.getItem();
            if (itemName != null) {
                try {
                    Item item = itemUIRegistry.getItem(itemName);
                    if (item instanceof GenericItem) {
                        items.add((GenericItem) item);
                    }
                } catch (ItemNotFoundException e) {
                    // ignore
                }
            }
        }
        return items;
    }

    /**
     * This is a state change listener, which is merely used to determine, if a
     * state change has occurred on one of a list of items.
     *
     * @author Kai Kreuzer - Initial contribution
     *
     */
    private static class BlockingStateChangeListener implements StateChangeListener {

        private boolean changed = false;

        @Override
        public void stateChanged(Item item, State oldState, State newState) {
            changed = true;
        }

        /**
         * determines, whether a state change has occurred since its creation
         *
         * @return true, if a state has changed
         */
        public boolean hasChangeOccurred() {
            return changed;
        }

        @Override
        public void stateUpdated(Item item, State state) {
            // ignore if the state did not change
        }
    }

    @Override
    public void onEvent(SitemapEvent event) {
        OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        OutboundEvent outboundEvent = eventBuilder.name("event").mediaType(MediaType.APPLICATION_JSON_TYPE).data(event)
                .build();
        broadcaster.broadcast(outboundEvent);
    }

    @Override
    public void onRelease(String subscriptionId) {
        logger.debug("SSE connection for subscription {} has been released.", subscriptionId);
        EventOutput eventOutput = eventOutputs.remove(subscriptionId);
        if (eventOutput != null) {
            broadcaster.remove(eventOutput);
        }
    }

    @Override
    public void onClose(ChunkedOutput<OutboundEvent> event) {
        if (event instanceof SitemapEventOutput) {
            SitemapEventOutput sitemapEvent = (SitemapEventOutput) event;
            logger.debug("SSE connection for subscription {} has been closed.", sitemapEvent.getSubscriptionId());
            subscriptions.removeSubscription(sitemapEvent.getSubscriptionId());
            EventOutput eventOutput = eventOutputs.remove(sitemapEvent.getSubscriptionId());
            if (eventOutput != null) {
                broadcaster.remove(eventOutput);
            }
        }
    }

    @Override
    public void onException(ChunkedOutput<OutboundEvent> event, Exception e) {
        // the exception is usually "null" and onClose() is automatically called afterwards
        // - so let's don't do anything in this method.
    }

    @Override
    public boolean isSatisfied() {
        return itemUIRegistry != null && subscriptions != null && localeService != null;
    }

}
