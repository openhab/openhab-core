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
package org.eclipse.smarthome.ui.internal.proxy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.sitemap.Image;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.Video;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;
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
 * However, if the Image or Video widget is associated with an item whose current State is a StringType,
 * it will attempt to use the state of the item as the url to proxy, or fall back to the url= attribute
 * if the state is not a valid url, so you must make sure that the item's state cannot be set to an
 * internal image or video url that you do not wish to proxy out of your network. If you are concerned
 * with the security aspect of using item= to proxy image or video URLs, then do not use item= with those
 * widgets in your sitemaps.
 *
 * It is also possible to use credentials in a url, e.g. "http://user:pwd@localserver/image.jpg" -
 * the proxy servlet will be able to access the content and provide it to the web UIs through the
 * standard web authentication mechanism (if enabled).
 *
 * This servlet also supports data streams, such as a webcam video stream etc.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author John Cocula - added optional Image/Video item= support; refactored to allow use of later spec servlet
 */
@Component(immediate = true, property = { "service.pid=org.eclipse.smarthome.proxy" })
public class ProxyServletService extends HttpServlet {

    /** the alias for this servlet */
    public static final String PROXY_ALIAS = "proxy";

    private static final String CONFIG_MAX_THREADS = "maxThreads";
    private static final int DEFAULT_MAX_THREADS = 8;
    public static final String ATTR_URI = ProxyServletService.class.getName() + ".URI";
    public static final String ATTR_SERVLET_EXCEPTION = ProxyServletService.class.getName() + ".ProxyServletException";

    private final Logger logger = LoggerFactory.getLogger(ProxyServletService.class);

    private static final long serialVersionUID = -4716754591953017793L;

    private Servlet impl;

    protected HttpService httpService;
    protected ItemUIRegistry itemUIRegistry;
    protected ModelRepository modelRepository;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = itemUIRegistry;
    }

    protected void unsetItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = null;
    }

    @Reference
    protected void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    protected void unsetModelRepository(ModelRepository modelRepository) {
        this.modelRepository = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    /**
     * Return the async in preference to the blocking proxy servlet, if possible.
     * Supported OSGi containers might only support Servlet API 2.4 (blocking only).
     */
    private Servlet getImpl() {
        if (impl == null) {
            try {
                ServletRequest.class.getMethod("startAsync");
                impl = new AsyncProxyServlet(this);
            } catch (Throwable t) {
                impl = new BlockingProxyServlet(this);
            }
        }
        return impl;
    }

    /**
     * Copy the ConfigAdminManager's config to the init parameters of the servlet.
     *
     * @param config the OSGi config, may be <code>null</code>
     * @return properties to pass to servlet for initialization
     */
    private Hashtable<String, String> propsFromConfig(Map<String, Object> config) {
        Hashtable<String, String> props = new Hashtable<String, String>();

        if (config != null) {
            for (String key : config.keySet()) {
                props.put(key, config.get(key).toString());
            }
        }

        // must specify for Jetty proxy servlet, per http://stackoverflow.com/a/27625380
        if (props.get(CONFIG_MAX_THREADS) == null) {
            props.put(CONFIG_MAX_THREADS,
                    String.valueOf(Math.max(DEFAULT_MAX_THREADS, Runtime.getRuntime().availableProcessors())));
        }

        return props;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        try {
            Servlet servlet = getImpl();

            logger.debug("Starting up '{}' servlet  at /{}", servlet.getServletInfo(), PROXY_ALIAS);

            Hashtable<String, String> props = propsFromConfig(config);
            httpService.registerServlet("/" + PROXY_ALIAS, servlet, props, createHttpContext());
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during servlet startup: {}", e.getMessage());
        }
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
     * Creates a {@link HttpContext}
     *
     * @return a {@link HttpContext}
     */
    protected HttpContext createHttpContext() {
        return httpService.createDefaultHttpContext();
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

            Sitemap sitemap = (Sitemap) modelRepository.getModel(sitemapName);
            if (sitemap == null) {
                throw new ProxyServletException(HttpServletResponse.SC_NOT_FOUND,
                        String.format("Sitemap '%s' could not be found!", sitemapName));
            }

            Widget widget = itemUIRegistry.getWidget(sitemap, widgetId);
            if (widget == null) {
                throw new ProxyServletException(HttpServletResponse.SC_NOT_FOUND,
                        String.format("Widget '%s' could not be found!", widgetId));
            }

            String uriString = null;
            if (widget instanceof Image) {
                uriString = ((Image) widget).getUrl();
            } else if (widget instanceof Video) {
                uriString = ((Video) widget).getUrl();
            } else {
                throw new ProxyServletException(HttpServletResponse.SC_FORBIDDEN,
                        String.format("Widget type '%s' is not supported!", widget.getClass().getName()));
            }

            String itemName = widget.getItem();
            if (itemName != null) {
                State state = itemUIRegistry.getItemState(itemName);
                if (state != null && state instanceof StringType) {
                    try {
                        uri = createURIFromString(state.toString());
                        request.setAttribute(ATTR_URI, uri);
                        return uri;
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

    private URI createURIFromString(String url) throws MalformedURLException, URISyntaxException {
        // URI in this context should be valid URL. Therefore before creating URI, create URL,
        // which validates the string.
        return new URL(url).toURI();
    }

    /**
     * If the URI contains user info in the form <code>user[:pass]@</code>, attempt to preempt the server
     * returning a 401 by providing Basic Authentication support in the initial request to the server.
     *
     * @param uri the URI which may contain user info
     * @param request the outgoing request to which an authorization header may be added
     */
    void maybeAppendAuthHeader(URI uri, Request request) {
        if (uri != null && uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":");

            if (userInfo.length >= 1) {
                String user = userInfo[0];
                String password = userInfo.length >= 2 ? userInfo[1] : null;
                String authString = password != null ? user + ":" + password : user + ":";

                String basicAuthentication = "Basic " + B64Code.encode(authString, StringUtil.__ISO_8859_1);
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

        boolean proxyingVideo = false;

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

            Sitemap sitemap = (Sitemap) modelRepository.getModel(sitemapName);
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
            } else if (widget instanceof Video) {
                proxyingVideo = true;
            } else {
                throw new ProxyServletException(HttpServletResponse.SC_FORBIDDEN,
                        String.format("Widget type '%s' is not supported!", widget.getClass().getName()));
            }
        } catch (ProxyServletException pse) {
            request.setAttribute(ATTR_SERVLET_EXCEPTION, pse);
        }
        return proxyingVideo;
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
