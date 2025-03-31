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
package org.openhab.core.media.internal;

import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.media.MediaHTTPServer;
import org.openhab.core.media.MediaService;
import org.openhab.core.media.model.MediaEntry;
import org.openhab.core.media.model.MediaRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet that
 *
 * @author Laurent Arnal - Initial contribution
 */
@Component(service = { MediaHTTPServer.class, Servlet.class })
@HttpWhiteboardServletName(MediaServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(MediaServlet.SERVLET_PATH + "/*")
@NonNullByDefault
public class MediaServlet extends HttpServlet implements MediaHTTPServer {

    @Serial
    private static final long serialVersionUID = -3364664035854567854L;

    private static final List<String> WAV_MIME_TYPES = List.of("audio/wav", "audio/x-wav", "audio/vnd.wave");

    // A 1MB in memory buffer will help playing multiple times an AudioStream, if the sink cannot do otherwise
    private static final int ONETIME_STREAM_BUFFER_MAX_SIZE = 1048576;
    // 5MB max for a file buffer
    private static final int ONETIME_STREAM_FILE_MAX_SIZE = 5242880;

    public final MediaService mediaService;

    static final String SERVLET_PATH = "/media";

    private final Logger logger = LoggerFactory.getLogger(MediaServlet.class);

    private final ScheduledExecutorService threadPool = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    @Nullable
    ScheduledFuture<?> periodicCleaner;

    @Activate
    public MediaServlet(@Reference MediaService mediaService) {
        super();
        logger.info("constructor");
        this.mediaService = mediaService;
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        logger.info("activate");
    }

    @Deactivate
    protected synchronized void deactivate() {
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        if (requestURI == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "requestURI is null");
            return;
        }

        String qs = req.getQueryString();
        String path = req.getParameter("path");

        ServletOutputStream stream = resp.getOutputStream();
        final StringBuilder sb = new StringBuilder(5000);

        if (path == null) {
            path = "/Root";
        }

        MediaRegistry mediaRegistry = mediaService.getMediaRegistry();
        MediaEntry currentEntry = mediaRegistry.getChildForPath(path);

        MediaEntry recurseEntry = currentEntry;
        while (!recurseEntry.getName().equals("Root")) {
            sb.insert(0, " > <a href=" + requestURI + "?path=" + recurseEntry.getPath() + ">" + recurseEntry.getName()
                    + "</a>");
            recurseEntry = recurseEntry.getParent();
        }

        sb.insert(0, "<a href=" + requestURI + "?path=/Root>Root</a>");

        sb.append("<br/><br/>");
        for (String key : currentEntry.getChilds().keySet()) {
            MediaEntry entry = currentEntry.getChilds().get(key);
            sb.append("<a href=" + requestURI + "?path=" + entry.getPath() + ">" + entry.getName() + "</a><br/>");
        }

        resp.setContentType("text/html; charset=utf-8");
        stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));

    }

}
