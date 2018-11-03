/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.ui.start.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.openhab.ui.dashboard.DashboardReady;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet registers status (starting/stopping/updating) pages and serves the 404 page if system is started and an
 * unknown url is called.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@Component(immediate = true)
public class RootServlet extends DefaultServlet {

    private static final String STATIC_CONTENT_FOLDER = "html";
    private static final String OPENHAB_CONF_SYSPROPERTY = "openhab.conf";
    private static final String STATIC_CONTENT_URL = "/static";
    public static final String START_URL = "/start/index";

    private static final long serialVersionUID = -2091860295954594917L;

    private final Logger logger = LoggerFactory.getLogger(RootServlet.class);

    protected HttpService httpService;

    // an enumeration for the state the whole system is in
    private enum LifeCycleState {
        STARTING,
        STARTED,
        STOPPING,
        UPDATING;
    }

    private String page404;
    private String pageStatus;

    private DashboardReady dashboardStarted;
    private LifeCycleState lifecycleState = LifeCycleState.STARTING;
    private ServiceRegistration handlerRef;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (dashboardStarted != null) {
            // all is up and running
            if (req.getRequestURI().equals("/")) {
                resp.sendRedirect(START_URL);
            } else if (!req.getRequestURI().startsWith(STATIC_CONTENT_URL)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().append(page404);
                resp.getWriter().close();
            } else {
                super.doGet(req, resp);
            }
        } else {
            // report current system state
            String message = null;
            String subMessage = null;
            switch (lifecycleState) {
                case STARTING:
                    message = "openHAB is starting...";
                    subMessage = "Please wait a moment!";
                    break;
                case UPDATING:
                    message = "openHAB is updating...";
                    subMessage = "Please wait a moment!";
                    break;
                case STOPPING:
                    message = "openHAB is shutting down...";
                    subMessage = "Please stand by.";
                    break;
                default:
                    throw new IllegalStateException("Invalid system state " + lifecycleState);
            }
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(pageStatus.replace("${message}", message).replace("${submessage}", subMessage));
            resp.getWriter().close();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Activate
    protected void activate(ComponentContext context) {

        // register static content context handler
        ContextHandler staticContent = new ContextHandler();
        ResourceHandler handler = new ResourceHandler();
        handler.setDirectoriesListed(false);
        handler.setResourceBase(System.getProperty(OPENHAB_CONF_SYSPROPERTY) + "/" + STATIC_CONTENT_FOLDER);
        staticContent.setHandler(handler);
        Dictionary props = new Hashtable();
        props.put("contextPath", STATIC_CONTENT_URL);
        handlerRef = context.getBundleContext().registerService(Handler.class.getName(), staticContent, props);

        // register servlet
        try {
            httpService.registerServlet("/", this, new Properties(), null);
        } catch (ServletException | NamespaceException e) {
            logger.error("Failed registering root servlet!", e);
        }
        URL notfound = context.getBundleContext().getBundle().getEntry("pages/404.html");
        if (notfound != null) {
            try {
                page404 = IOUtils.toString(notfound.openStream());
            } catch (IOException e) {
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Cannot find 404.html - failed to initialize root servlet");
        }
        URL status = context.getBundleContext().getBundle().getEntry("pages/status.html");
        if (status != null) {
            try {
                pageStatus = IOUtils.toString(status.openStream());
            } catch (IOException e) {
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Cannot find status.html - failed to initialize root servlet");
        }

        // we can determine whether the whole framework is shutdown by listening to a STOPPING event for bundle 0.
        Bundle systemBundle = context.getBundleContext().getBundle(0);
        systemBundle.getBundleContext().addBundleListener(new SynchronousBundleListener() {
            @Override
            public void bundleChanged(final BundleEvent event) {
                if (event.getBundle().getBundleId() == 0 && event.getType() == BundleEvent.STOPPING) {
                    lifecycleState = LifeCycleState.STOPPING;
                }
            }
        });
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        httpService.unregister("/");
        if (handlerRef != null) {
            handlerRef.unregister();
            handlerRef = null;
        }

        // reset, if this component is ever reused (should normally not be the case), it should be "starting" again.
        lifecycleState = LifeCycleState.STARTING;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setDashboardStarted(DashboardReady dashboardStarted) {
        this.dashboardStarted = dashboardStarted;
        this.lifecycleState = LifeCycleState.STARTED;
    }

    protected void unsetDashboardStarted(DashboardReady dashboardStarted) {
        if (lifecycleState != LifeCycleState.STOPPING) {
            lifecycleState = LifeCycleState.UPDATING;
        }
        this.dashboardStarted = null;
    }

    @Reference
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }
}
