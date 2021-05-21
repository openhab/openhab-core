/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.updater.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.updater.dto.GetStateDTO;
import org.openhab.core.updater.enums.OperatingSystem;
import org.openhab.core.updater.updaterclasses.BaseUpdater;
import org.openhab.core.updater.updaterfactory.UpdaterFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This is a REST resource for OpenHAB self updating features.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class UpdaterServlet extends HttpServlet {

    private static final long serialVersionUID = 3638639874148443320L;

    private final Logger logger = LoggerFactory.getLogger(UpdaterServlet.class);

    // HTML paramaters
    private static final String HTML_TOP_MESSAGE = "HTML_TOP_MESSAGE";
    private static final String HTML_VER_NAME = "HTML_VER_NAME";
    private static final String HTML_VER_TYPE = "HTML_VER_TYPE";
    private static final String HTML_ACTION_URL = "HTML_ACTION_URL";
    private static final String HTML_HIDE_CREDS = "HTML_HIDE_CREDS";
    private static final String HTML_CHECKED = "HTML_CHECKED";
    private static final String HTML_HIDE_ALL = "HTML_HIDE_ALL";
    private static final String HTML_RET_BUT_LABEL = "HTML_RET_BUT_LABEL";

    private static final String OPENHAB_HTTP_PORT = "8443";

    public static final String URI_BASE = "/updater";
    public static final String URI_GET_STATUS = URI_BASE + "/getStatus";
    public static final String URI_START_UPDATE = URI_BASE + "/startUpdate";

    private @Nullable BaseUpdater updater;
    private String htmlTemplate = "";

    /**
     * We use a <strong>static</strong> and <strong>single thread</strong> executor here, in order to synchronise
     * multiple doUpdate method calls both a) within the same class instance, and b) across multiple class instances.
     */
    private static final ExecutorService UPDATE_EXECUTOR = Executors.newSingleThreadExecutor();

    // =============== Constructor ===============

    @Activate
    public UpdaterServlet(BundleContext bundleContext, @Reference HttpService httpService,
            @Reference UserRegistry userRegistry, @Reference AuthenticationProvider authProvider,
            @Reference LocaleProvider localeProvider) {

        updater = UpdaterFactory.newUpdater();

        // load the html template from resource
        String resourceId = "/html/" + getClass().getSimpleName() + ".html";
        InputStream resourceStream = getClass().getResourceAsStream(resourceId);
        if (resourceStream != null) {
            try {
                htmlTemplate = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
            }
        }
        if ("".equals(htmlTemplate)) {
            logger.debug("Could not get the resource id: {}", resourceId);
        }

        // register
        try {
            httpService.registerServlet(URI_BASE + "/*", this, null, null);
        } catch (NamespaceException | ServletException e) {
            logger.debug("Error registering servlet: {}", e.getMessage());
        }
    }

    // =============== Override Methods ===============

    /**
     * This method handles HTTP GET on the servlet's /* path. It dispatches the requests to respective methods.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        if (URI_GET_STATUS.equals(uri)) {
            doGetState(request, response);
            return;
        }
        if (URI_START_UPDATE.equals(uri)) {
            doGetStartUpdate(request, response);
            return;
        }
        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException e) {
        }
    }

    /**
     * This method handles HTTP POST on the servlet's /* path. It dispatches the requests to respective methods.
     *
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        if (URI_START_UPDATE.equals(request.getRequestURI())) {
            doPostStartUpdate(request, response);
            return;
        }
        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException e) {
        }
    }

    // =============== Private Methods ===============

    /**
     * Construct the HTML form.
     */
    private String getPageHTML(String topMessage, boolean hideAll, String returnButtonLabel) {
        String html = "";
        if (updater != null) {
            GetStateDTO dto = updater.getStatusDTO();

            html = htmlTemplate;

            // top message text
            html = html.replaceFirst(HTML_TOP_MESSAGE, topMessage);

            // apply hidden attribute on all fields except return button
            html = html.replace(HTML_HIDE_ALL, hideAll ? "hidden=\"true\"" : "");

            // current version text
            html = html.replaceFirst(HTML_VER_NAME, dto.actualVersion.versionName);
            html = html.replaceFirst(HTML_VER_TYPE, dto.actualVersion.versionType);

            // latest versions texts and radio buttons
            String newVerType = dto.targetNewVersionType;
            for (int i = 0; i < dto.latestVersionCount; i++) {
                String verName = dto.latestVersions[i].versionName;
                String verType = dto.latestVersions[i].versionType;
                html = html.replaceFirst(HTML_VER_NAME, verName);
                html = html.replaceFirst(HTML_VER_TYPE, verType);
                html = html.replaceFirst(HTML_CHECKED, newVerType.equals(verType) ? "checked" : "");
            }

            // apply hidden attribute on credentials depending on OS
            html = html.replaceFirst(HTML_HIDE_CREDS,
                    OperatingSystem.getOperatingSystemVersion() == OperatingSystem.WINDOWS ? "hidden=\"true\"" : "");

            // apply action url on submit button
            html = html.replaceFirst(HTML_ACTION_URL, URI_START_UPDATE);

            // apply label on return button
            html = html.replaceFirst(HTML_RET_BUT_LABEL, returnButtonLabel);
        }
        return html;
    }

    /**
     * This method handles HTTP GET on the servlet's getStatus path. It serves a JSON DTO containing the updater status.
     *
     */
    private void doGetState(HttpServletRequest request, HttpServletResponse response) {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            String json = new Gson().toJson(updater.getStatusDTO());
            response.getWriter().append(json).close();
        } catch (IOException e) {
        }
    }

    /**
     * This method handles HTTP GET on the servlet's '/startUpdate' path. It serves an HTML page containing an HTML FORM
     * for submitting a request to the updater for starting the update process. If the request is not secure (on HTTP
     * only) it redirects to the OpenHAB HTTPS server.
     *
     */
    private void doGetStartUpdate(HttpServletRequest request, HttpServletResponse response) {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
            }
            return;
        }

        // redirect HTTP requests to the OH HTTPS url
        if (!request.isSecure()) {
            String httpsPath = "https://" + request.getServerName() + ":" + OPENHAB_HTTP_PORT + request.getRequestURI();
            try {
                response.sendRedirect(httpsPath);
            } catch (IOException e) {
            }
            return;
        }

        // get the page
        String html = getPageHTML("OpenHAB Self- Updater", false, "Cancel");

        // serve the page
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().append(html).close();
        } catch (IOException e) {
        }
    }

    /**
     * This method handles HTTP POST on the servlet's '/startUpdate' path. It accepts the respective form parameters,
     * and if they are OK starts the updater's update process.
     */
    private void doPostStartUpdate(HttpServletRequest request, HttpServletResponse response) {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
            }
            return;
        }

        // return not found error if the connection is not secure
        if (!request.isSecure()) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
            }
            return;
        }

        boolean fail = false;
        String targetNewVersionType = request.getParameter("targetNewVersionType");
        if (targetNewVersionType != null) {
            try {
                updater.setNewVersionType(targetNewVersionType);
            } catch (IllegalArgumentException e) {
                fail = true;
            }
        }

        String user = request.getParameter("user");
        if (user != null) {
            try {
                updater.setUserName(user);
            } catch (IllegalArgumentException e) {
                fail = true;
            }
        }

        String password = request.getParameter("password");
        if (password != null) {
            try {
                updater.setPassword(password);
            } catch (IllegalArgumentException e) {
                fail = true;
            }
        }

        // get the page
        String html;
        if (fail) {
            // show all fields and ask user to try again
            html = getPageHTML("Please try again..", false, "Cancel");
        } else {
            // hide all fields and let the user go back
            html = getPageHTML("Update started..", true, "Back");
        }

        // serve the page
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().append(html).close();
        } catch (IOException e) {
        }

        // finally submit the updater's run() method for execution
        if (!fail) {
            UPDATE_EXECUTOR.submit(updater);
        }
    }
}
