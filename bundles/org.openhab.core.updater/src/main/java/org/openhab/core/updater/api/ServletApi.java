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
package org.openhab.core.updater.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.io.http.servlet.OpenHABServlet;
import org.openhab.core.updater.dto.UpdaterStatusDTO;
import org.openhab.core.updater.enums.OperatingSystem;
import org.openhab.core.updater.updaterclasses.BaseUpdater;
import org.openhab.core.updater.updaterfactory.UpdaterFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import com.google.gson.Gson;

/**
 * This is a REST resource for OpenHAB self updating features.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class ServletApi extends OpenHABServlet {
    private static final long serialVersionUID = 5439260400198671435L;

    // ui texts
    private static final String TOP_MESSAGE_INITIAL = "OpenHAB Self- Updater";
    private static final String TOP_MESSAGE_TRY_AGAIN = "<b>Input error: Please try again..</b>";
    private static final String TOP_MESSAGE_UPDATE_STARTED = "<b>Update process has started..</b>";
    private static final String BOTTOM_BUTTON_CANCEL = "Cancel";
    private static final String BOTTOM_BUTTON_HOME = "Home";

    // error messages
    private static final String ERR_UPDATER_NULL = "Updater not found!";
    private static final String ERR_NOT_FOUND = "Resource not found!";

    // HTML customisation parameters
    private static final String HTML_TOP_MESSAGE = "HTML_TOP_MESSAGE";
    private static final String HTML_FORM_HIDDEN = "HTML_FORM_HIDDEN";
    private static final String HTML_VERSION_NAME = "HTML_VERSION_NAME";
    private static final String HTML_VERSION_TYPE = "HTML_VERSION_TYPE";
    private static final String HTML_VERSION_CHECKED = "HTML_VERSION_CHECKED";
    private static final String HTML_CREDENTIALS_HIDDEN = "HTML_CREDENTIALS_HIDDEN";
    private static final String HTML_BOTTOM_BUTTON_LABEL = "HTML_BOTTOM_BUTTON_LABEL";
    private static final String HTML_URL_CHOICE_0 = "HTML_URL_CHOICE_0";
    private static final String HTML_URL_CHOICE_1 = "HTML_URL_CHOICE_1";
    private static final String HTML_URL_CHOICE_HIDDEN = "HTML_URL_CHOICE_HIDDEN";
    private static final String HTML_SPINNER_HIDDEN = "HTML_SPINNER_HIDDEN";

    // the port number of the OpenHAB HTTPS server
    // private static final int OPENHAB_HTTPS_PORT = 8443;

    private static final String URI_ALIAS = "/" + ApiConstants.URI_BASE + "/*";
    private static final String URI_STATUS = "/" + ApiConstants.URI_BASE + ApiConstants.URI_STATUS;
    private static final String URI_EXECUTE = "/" + ApiConstants.URI_BASE + ApiConstants.URI_EXECUTE;

    private static final boolean HIDE_FORM = true;
    private static final boolean SHOW_FORM = !HIDE_FORM;

    private String htmlTemplate = "";
    private @Nullable BaseUpdater updater;
    private UserRegistry userRegistry;

    // =============== Constructor ===============

    @Activate
    public ServletApi(@Reference HttpService httpService, @Reference HttpContext httpContext,
            @Reference UserRegistry userRegistry) {
        super(httpService, httpContext);
        this.userRegistry = userRegistry;
        updater = UpdaterFactory.newUpdater();

        // load the HTML template from resource
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
    }

    @Activate
    protected void activate() {
        super.activate(URI_ALIAS);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate(URI_ALIAS);
    }

    // =============== Overridden Methods ===============

    /**
     * This method handles HTTP GET on the servlet's /* path. It dispatches the requests to respective methods.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (URI_STATUS.equals(uri)) {
            doGetStatus(request, response);
            return;
        }
        if (URI_EXECUTE.equals(uri)) {
            doGetExecute(request, response);
            return;
        }
        setResponse(HttpStatus.NOT_FOUND_404, ERR_NOT_FOUND, response);
    }

    /**
     * This method handles HTTP POST on the servlet's /* path. It dispatches the requests to respective methods.
     *
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (URI_EXECUTE.equals(uri)) {
            doPostExecute(request, response);
            return;
        }
        setResponse(HttpStatus.NOT_FOUND_404, ERR_NOT_FOUND, response);
    }

    // =============== Private Helper Methods ===============

    /**
     * Creates and formats the HTML form. It starts off with the htmlTemplate and replaces all the HTML_xxx markers in
     * it depending on the input parameters below.
     *
     * @param message the message at the top of the form.
     * @param formHidden selects if the HTML elements between top message and bottom button are hidden; if
     *            true then only top message and bottom button can be seen.
     * @param buttonLabel the label on the bottom button.
     * @param urlChoice0 choice 0 for the form action submit url
     * @param urlChoice1 choice 1 for the form action submit url
     * @return
     */
    private String getFormattedExecutePageHTML(HttpServletRequest request, String message, boolean formHidden,
            String buttonLabel) {
        String html = "";
        if (updater != null) {
            UpdaterStatusDTO dto = updater.getStatusDTO();

            html = htmlTemplate;

            // top message text
            html = html.replaceFirst(HTML_TOP_MESSAGE, message);

            // apply hidden attribute on all fields except return button
            html = html.replace(HTML_FORM_HIDDEN, formHidden ? "hidden=\"true\"" : "");

            // current version text
            html = html.replaceFirst(HTML_VERSION_NAME, dto.actualVersion.versionName);
            html = html.replaceFirst(HTML_VERSION_TYPE, dto.actualVersion.versionType);

            // latest versions texts and radio buttons
            String newVerType = dto.targetNewVersionType;
            for (int i = 0; i < dto.latestVersionCount; i++) {
                String verName = dto.latestVersions[i].versionName;
                String verType = dto.latestVersions[i].versionType;
                html = html.replaceFirst(HTML_VERSION_NAME, verName);
                html = html.replaceFirst(HTML_VERSION_TYPE, verType);
                html = html.replaceFirst(HTML_VERSION_CHECKED, newVerType.equals(verType) ? "checked" : "");
            }

            // the credentials can be hidden on some Operating Systems (e.g. Windows)
            html = html.replaceFirst(HTML_CREDENTIALS_HIDDEN,
                    OperatingSystem.getOperatingSystemVersion().requiresCredentials() ? "" : "hidden=\"true\"");

            // form submit action url choices both default to the same URI as the GET was received on
            String thisUrl = request.getRequestURI();
            String urlChoice0 = thisUrl != null ? thisUrl : URI_EXECUTE;
            String urlChoice1 = urlChoice0;

            if (!request.isSecure()) {
                // the urlChoice1 submit action url is explicitly set to the matching url on the HTTPS server
                urlChoice1 = String.format("https://%s:%s%s", request.getServerName(), System.getenv("HTTPS_PORT"),
                        request.getRequestURI());
            }

            // apply the submit action url choices
            html = html.replaceFirst(HTML_URL_CHOICE_0, urlChoice0);
            html = html.replaceFirst(HTML_URL_CHOICE_1, urlChoice1);
            html = html.replaceFirst(HTML_URL_CHOICE_HIDDEN, urlChoice1.equals(urlChoice0) ? "hidden=\"true\"" : "");

            // hide or show the spinner
            html = html.replace(HTML_SPINNER_HIDDEN, formHidden ? "" : "hidden=\"true\"");

            // apply label on return button
            html = html.replaceFirst(HTML_BOTTOM_BUTTON_LABEL, buttonLabel);
        }
        return html;
    }

    /**
     * Sets a response message and HTTP status code code in the response.
     *
     * @param httpStatus
     * @param message
     * @param response
     * @throws IOException
     */
    private void setResponse(int httpStatus, String message, HttpServletResponse response) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.TEXT_PLAIN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().append(message).close();
    }

    // =============== Private Processing Methods ===============

    /**
     * This method handles HTTP GET on the servlet's status path. It serves a JSON DTO containing the updater status.
     *
     */
    private void doGetStatus(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            setResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, ERR_UPDATER_NULL, response);
            return;
        }

        // return the DTO
        response.setStatus(HttpStatus.OK_200);
        response.setContentType(MediaType.APPLICATION_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = new Gson().toJson(updater.getStatusDTO());
        response.getWriter().append(json).close();
    }

    /**
     * This method handles HTTP GET on the servlet's execute path. It serves an HTML page containing an HTML form for
     * submitting a request to the updater to start the update process.
     *
     */
    private void doGetExecute(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            setResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, ERR_UPDATER_NULL, response);
            return;
        }

        // get and format the execute page HTML; formatted so the form is shown, and the bottom button says 'Cancel'
        String html = getFormattedExecutePageHTML(request, TOP_MESSAGE_INITIAL, SHOW_FORM, BOTTOM_BUTTON_CANCEL);

        // serve the HTML page
        response.setStatus(HttpStatus.OK_200);
        response.setContentType(MediaType.TEXT_HTML);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().append(html).close();
    }

    /**
     * This method handles HTTP POST on the servlet's execut path. It accepts the respective form parameters, and if
     * they are OK starts the updater's update process.
     */
    private void doPostExecute(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            setResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, ERR_UPDATER_NULL, response);
            return;
        }

        // all form parameters must be OK
        boolean paramsOk = true;

        // a) OpenHAB user credentials
        String openHabUser = request.getParameter("openhabuser");
        String openHabPassword = request.getParameter("openhabpassword");
        try {
            userRegistry.authenticate(new UsernamePasswordCredentials(openHabUser, openHabPassword));
        } catch (AuthenticationException e1) {
            paramsOk = false;
        }

        // b) target version type
        String targetNewVersionType = request.getParameter("targetNewVersionType");
        if (targetNewVersionType != null) {
            try {
                updater.setNewVersionType(targetNewVersionType);
            } catch (IllegalArgumentException e) {
                paramsOk = false;
            }
        }

        // c) system user name
        String systemUser = request.getParameter("systemuser");
        if (systemUser != null) {
            try {
                updater.setUserName(systemUser);
            } catch (IllegalArgumentException e) {
                paramsOk = false;
            }
        }

        // d) system password
        String systemPassword = request.getParameter("systempassword");
        if (systemPassword != null) {
            try {
                updater.setPassword(systemPassword);
            } catch (IllegalArgumentException e) {
                paramsOk = false;
            }
        }

        // get and format the execute page HTML
        String html;
        if (paramsOk) {
            // formatted so the form is hidden, and the bottom button says 'Home'
            html = getFormattedExecutePageHTML(request, TOP_MESSAGE_UPDATE_STARTED, HIDE_FORM, BOTTOM_BUTTON_HOME);
        } else {
            // formatted so the form is shown, and the bottom button says 'Cancel'
            html = getFormattedExecutePageHTML(request, TOP_MESSAGE_TRY_AGAIN, SHOW_FORM, BOTTOM_BUTTON_CANCEL);
        }

        // serve the HTML page
        response.setStatus(HttpStatus.OK_200);
        response.setContentType(MediaType.TEXT_HTML);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().append(html).close();

        // finally submit the updater for execution
        if (paramsOk) {
            updater.submit();
        }
    }
}
