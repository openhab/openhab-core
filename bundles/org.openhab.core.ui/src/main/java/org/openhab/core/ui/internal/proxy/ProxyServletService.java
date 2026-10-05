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
package org.openhab.core.ui.internal.proxy;

import java.io.IOException;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.core.library.types.StringType;
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.types.State;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The proxy servlet is used by image and video widgets. As its name suggests, it proxies the content, so
 * that it is possible to include resources (images/videos) from the LAN in the web UI. This is
 * especially useful for webcams as you would not want to make them directly available to the internet.
 *
 * The servlet registers as "/proxy" and expects the two parameters "sitemap" and "widgetId". It will
 * hence provide the data of the url specified in the according widget. Note that it does NOT allow
 * general access to any servers in the LAN - only urls that are specified in a sitemap are accessible.
 *
 * If the Image or Video widget is associated with an item whose current State is a StringType, the state of the
 * item is used as the url to proxy ONLY IF its host (or host:port) is listed in the "allowedHosts" configuration
 * (comma-separated, case-insensitive, e.g. "camera.local, 192.168.1.20:8080") of the "org.openhab.sitemap" service
 * (Sitemap in the system settings).
 * By default the list is empty, so item states are never used as proxy targets. Item states with credentials in the
 * url, with a scheme other than http/https, or with a host that is not allowed are ignored and the url= attribute
 * of the widget is used instead. Only list hosts that you want to expose through the proxy, as anybody who can
 * change the state of the item can then make the proxy fetch any resource on those hosts.
 *
 * It is also possible to use credentials in a url, e.g. "http://user:pwd@localserver/image.jpg" -
 * the proxy servlet will be able to access the content and provide it to the web UIs through the
 * standard web authentication mechanism (if enabled).
 *
 * This servlet also supports data streams, such as a webcam video stream etc.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author John Cocula - added optional Image/Video item= support; refactored to allow use of later spec servlet
 * @author Mark Herwege - Implement sitemap registry
 */
@NonNullByDefault
@Component(immediate = true, configurationPid = "org.openhab.sitemap")
public class ProxyServletService extends HttpServlet {

    /** the alias for this servlet */
    public static final String PROXY_ALIAS = "proxy";

    @Serial
    private static final long serialVersionUID = -4716754591953017793L;
    private static final String CONFIG_MAX_PROXY_THREADS = "maxProxyThreads";
    private static final String JETTY_MAX_THREADS = "maxThreads";
    private static final int DEFAULT_MAX_THREADS = 8;
    private static final String CONFIG_ALLOWED_HOSTS = "allowedHosts";
    public static final String ATTR_URI = ProxyServletService.class.getName() + ".URI";
    public static final String ATTR_SERVLET_EXCEPTION = ProxyServletService.class.getName() + ".ProxyServletException";

    private final Logger logger = LoggerFactory.getLogger(ProxyServletService.class);

    private @Nullable Servlet impl;
    private final Set<String> allowedHosts;

    protected final HttpService httpService;
    protected final ItemUIRegistry itemUIRegistry;
    protected final SitemapRegistry sitemapRegistry;

    @Activate
    public ProxyServletService(@Reference HttpService httpService, @Reference ItemUIRegistry itemUIRegistry,
            @Reference SitemapRegistry sitemapRegistry, Map<String, Object> config) {
        this.httpService = httpService;
        this.itemUIRegistry = itemUIRegistry;
        this.sitemapRegistry = sitemapRegistry;
        this.allowedHosts = parseAllowedHosts(config.get(CONFIG_ALLOWED_HOSTS));

        Servlet servlet = getImpl();

        logger.debug("Starting up '{}' servlet  at /{}", servlet.getServletInfo(), PROXY_ALIAS);
        try {
            httpService.registerServlet("/" + PROXY_ALIAS, servlet, propsFromConfig(config, servlet),
                    httpService.createDefaultHttpContext());
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during servlet startup: {}", e.getMessage());
        }
    }

    private static Set<String> parseAllowedHosts(@Nullable Object value) {
        if (value == null) {
            return Set.of();
        }
        return Stream.of(value.toString().split(",")).map(String::trim).filter(h -> !h.isEmpty())
                .map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks whether an url derived from an item state may be proxied.
     */
    private boolean isAllowedItemUri(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getUserInfo() != null) {
            return false;
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        return allowedHosts.contains(host)
                || (uri.getPort() != -1 && allowedHosts.contains(host + ":" + uri.getPort()));
    }

    @Deactivate
    protected void deactivate() {
        try {
            httpService.unregister("/" + PROXY_ALIAS);
        } catch (IllegalArgumentException e) {
            // ignore, had not been registered before
        }
    }

    /**
     * Return the async in preference to the blocking proxy servlet, if possible.
     * Supported OSGi containers might only support Servlet API 2.4 (blocking only).
     */
    private Servlet getImpl() {
        Servlet servlet = impl;
        if (servlet == null) {
            try {
                ServletRequest.class.getMethod("startAsync");
                servlet = new AsyncProxyServlet(this);
            } catch (Throwable t) {
                servlet = new BlockingProxyServlet(this);
            }
            impl = servlet;
        }
        return servlet;
    }

    /**
     * Build the init parameters of the servlet. Only the maximum number of threads is taken from the configuration
     * ("maxProxyThreads") and passed on under the name expected by the Jetty proxy servlet ("maxThreads").
     *
     * @param config the OSGi config
     * @return properties to pass to servlet for initialization
     */
    Hashtable<String, @Nullable String> propsFromConfig(Map<String, Object> config, Servlet servlet) {
        Hashtable<String, @Nullable String> props = new Hashtable<>();

        int maxThreads = Math.max(DEFAULT_MAX_THREADS, Runtime.getRuntime().availableProcessors());
        Object configured = config.get(CONFIG_MAX_PROXY_THREADS);
        if (configured != null) {
            try {
                int value = Integer.parseInt(configured.toString().trim());
                if (value > 0) {
                    maxThreads = value;
                } else {
                    logger.warn("Ignoring invalid {} '{}', using {}", CONFIG_MAX_PROXY_THREADS, configured, maxThreads);
                }
            } catch (NumberFormatException e) {
                logger.warn("Ignoring invalid {} '{}', using {}", CONFIG_MAX_PROXY_THREADS, configured, maxThreads);
            }
        }
        // must specify for Jetty proxy servlet, per http://stackoverflow.com/a/27625380
        props.put(JETTY_MAX_THREADS, String.valueOf(maxThreads));

        if (servlet instanceof AsyncProxyServlet) {
            props.put("async-supported", "true");
        }

        return props;
    }

    /**
     * Encapsulate the HTTP status code and message in an exception.
     */
    static class ProxyServletException extends Exception {
        static final long serialVersionUID = -1L;
        private final int code;

        public ProxyServletException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Determine which URI to address based on the request contents.
     *
     * @param request the servlet request. New attributes may be added to the request in order to cache the result for
     *            future calls.
     * @return the URI indicated by the request, or <code>null</code> if not possible
     */
    /* default */ @Nullable
    URI uriFromRequest(HttpServletRequest request) {
        try {
            // Return any URI we've already saved for this request
            URI uri = (URI) request.getAttribute(ATTR_URI);
            if (uri != null) {
                return uri;
            } else {
                ProxyServletException pse = (ProxyServletException) request.getAttribute(ATTR_SERVLET_EXCEPTION);
                if (pse != null) {
                    // If we errored on this request before, there is no point continuing
                    return null;
                }
            }

            String sitemapName = request.getParameter("sitemap");
            if (sitemapName == null) {
                throw new ProxyServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "Parameter 'sitemap' must be provided!");
            }

            String widgetId = request.getParameter("widgetId");
            if (widgetId == null) {
                throw new ProxyServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "Parameter 'widgetId' must be provided!");
            }

            Sitemap sitemap = getSitemap(sitemapName);

            if (sitemap == null) {
                throw new ProxyServletException(HttpServletResponse.SC_NOT_FOUND,
                        String.format("Sitemap '%s' could not be found!", sitemapName));
            }

            Widget widget = itemUIRegistry.getWidget(sitemap, widgetId);
            if (widget == null) {
                throw new ProxyServletException(HttpServletResponse.SC_NOT_FOUND,
                        String.format("Widget '%s' could not be found!", widgetId));
            }

            String uriString;
            if (widget instanceof Image image) {
                uriString = image.getUrl();
            } else if (widget instanceof Video video) {
                uriString = video.getUrl();
            } else {
                throw new ProxyServletException(HttpServletResponse.SC_FORBIDDEN,
                        String.format("Widget type '%s' is not supported!", widget.getClass().getName()));
            }

            String itemName = widget.getItem();
            if (itemName != null && !allowedHosts.isEmpty()) {
                State state = itemUIRegistry.getItemState(itemName);
                if (state instanceof StringType) {
                    try {
                        URI itemUri = createURIFromString(state.toString());
                        if (isAllowedItemUri(itemUri)) {
                            request.setAttribute(ATTR_URI, itemUri);
                            return itemUri;
                        }
                        logger.debug("Ignoring url of item '{}' as its host is not in '{}'", itemName,
                                CONFIG_ALLOWED_HOSTS);
                    } catch (MalformedURLException | URISyntaxException ex) {
                        // fall thru
                    }
                }
            }

            try {
                uri = createURIFromString(uriString);
                request.setAttribute(ATTR_URI, uri);
                return uri;
            } catch (MalformedURLException | URISyntaxException ex) {
                throw new ProxyServletException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        String.format("URL '%s' is not a valid URL.", uriString));
            }
        } catch (ProxyServletException pse) {
            request.setAttribute(ATTR_SERVLET_EXCEPTION, pse);
            return null;
        }
    }

    private @Nullable Sitemap getSitemap(String sitemapName) {
        return sitemapRegistry.get(sitemapName);
    }

    private URI createURIFromString(@Nullable String url) throws MalformedURLException, URISyntaxException {
        if (url == null) {
            throw new MalformedURLException();
        }
        URI uri = new URI(url);
        // URI in this context should be valid URL. Therefore before returning URI, create URL,
        // which validates the string.
        try {
            uri.toURL();
        } catch (IllegalArgumentException e) {
            throw new MalformedURLException(e.getMessage());
        }
        return uri;
    }

    /**
     * If the URI contains user info in the form <code>user[:pass]@</code>, attempt to preempt the server
     * returning a 401 by providing Basic Authentication support in the initial request to the server.
     *
     * @param uri the URI which may contain user info
     * @param request the outgoing request to which an authorization header may be added
     */
    void maybeAppendAuthHeader(@Nullable URI uri, Request request) {
        if (uri != null && uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":");

            if (userInfo.length >= 1) {
                String user = userInfo[0];
                String password = userInfo.length >= 2 ? userInfo[1] : null;
                String authString = password != null ? user + ":" + password : user + ":";

                String basicAuthentication = "Basic " + Base64.getEncoder().encodeToString(authString.getBytes());
                request.header(HttpHeader.AUTHORIZATION, basicAuthentication);
            }
        }
    }

    /**
     * Determine if the request is relative to a video widget.
     *
     * @param request the servlet request
     * @return true if the request is relative to a video widget
     */
    boolean proxyingVideoWidget(HttpServletRequest request) {
        try {
            String sitemapName = request.getParameter("sitemap");
            if (sitemapName == null) {
                throw new ProxyServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "Parameter 'sitemap' must be provided!");
            }

            String widgetId = request.getParameter("widgetId");
            if (widgetId == null) {
                throw new ProxyServletException(HttpServletResponse.SC_BAD_REQUEST,
                        "Parameter 'widgetId' must be provided!");
            }

            Sitemap sitemap = getSitemap(sitemapName);
            if (sitemap == null) {
                throw new ProxyServletException(HttpServletResponse.SC_NOT_FOUND,
                        String.format("Sitemap '%s' could not be found!", sitemapName));
            }

            Widget widget = itemUIRegistry.getWidget(sitemap, widgetId);
            if (widget == null) {
                throw new ProxyServletException(HttpServletResponse.SC_NOT_FOUND,
                        String.format("Widget '%s' could not be found!", widgetId));
            }

            if (widget instanceof Image) {
                return false;
            } else if (widget instanceof Video) {
                return true;
            } else {
                throw new ProxyServletException(HttpServletResponse.SC_FORBIDDEN,
                        String.format("Widget type '%s' is not supported!", widget.getClass().getName()));
            }
        } catch (ProxyServletException pse) {
            request.setAttribute(ATTR_SERVLET_EXCEPTION, pse);
            return false;
        }
    }

    /**
     * Send the most specific error back to the client.
     *
     * @param request the request which may be marked with an error
     * @param response the reponse to which to send the error
     */
    void sendError(HttpServletRequest request, HttpServletResponse response) {
        ProxyServletException pse = (ProxyServletException) request
                .getAttribute(ProxyServletService.ATTR_SERVLET_EXCEPTION);
        if (pse != null) {
            try {
                response.sendError(pse.getCode(), pse.getMessage());
            } catch (IOException ioe) {
                response.setStatus(pse.getCode());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
