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
package org.openhab.core.io.http.auth.internal;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet serving a page allowing users to create a new API token, after confirming their identity by signing in.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true)
public class CreateAPITokenPageServlet extends AbstractAuthPageServlet {

    private static final long serialVersionUID = 5340598701104679843L;

    private final Logger logger = LoggerFactory.getLogger(CreateAPITokenPageServlet.class);

    @Activate
    public CreateAPITokenPageServlet(BundleContext bundleContext, @Reference HttpService httpService,
            @Reference UserRegistry userRegistry, @Reference AuthenticationProvider authProvider) {
        super(bundleContext, httpService, userRegistry, authProvider);
        try {
            httpService.registerServlet("/createApiToken", this, null, null);
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during create API token page registration: {}", e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();

        try {
            String message = "Create a new API token to authorize external services.";

            // TODO: i18n
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(getPageBody(params, message, false));
            resp.getWriter().close();
        } catch (Exception e) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().append(e.getMessage());
            resp.getWriter().close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();
        try {
            if (!params.containsKey("username")) {
                throw new AuthenticationException("no username");
            }
            if (!params.containsKey("password")) {
                throw new AuthenticationException("no password");
            }
            if (!params.containsKey("token_name")) {
                throw new AuthenticationException("no new password");
            }
            if (!params.containsKey("csrf_token") || !csrfTokens.containsKey(params.get("csrf_token")[0])) {
                throw new AuthenticationException("CSRF check failed");
            }

            removeCsrfToken(params.get("csrf_token")[0]);

            String username = params.get("username")[0];
            String password = params.get("password")[0];
            String tokenName = params.get("token_name")[0];
            String tokenScope = "";
            if (params.containsKey("token_name")) {
                tokenScope = params.get("token_scope")[0];
            }

            User user = login(username, password);
            String newApiToken;

            if (user instanceof ManagedUser) {
                if (((ManagedUser) user).getApiTokens().stream()
                        .anyMatch(apiToken -> apiToken.getName().equals(tokenName))) {
                    resp.setContentType("text/html;charset=UTF-8");
                    // TODO: i18n
                    resp.getWriter().append(
                            getPageBody(params, "A token with the same name already exists, please try again.", false));
                    resp.getWriter().close();
                    return;
                }

                if (!tokenName.matches("[a-zA-Z0-9]*")) {
                    resp.setContentType("text/html;charset=UTF-8");
                    // TODO: i18n
                    resp.getWriter().append(
                            getPageBody(params, "Invalid token name, please use alphanumeric characters only.", false));
                    resp.getWriter().close();
                    return;
                }
                newApiToken = userRegistry.addUserApiToken(user, tokenName, tokenScope);
            } else {
                throw new AuthenticationException("User is not managed");
            }

            // TODO: i18n
            String resultMessage = "New token created:<br /><br /><code>" + newApiToken + "</code>";
            resultMessage += "<br /><br /><small>Please copy it now, it will not be shown again.</small>";
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(getResultPageBody(params, resultMessage)); // TODO: i18n
            resp.getWriter().close();
        } catch (AuthenticationException e) {
            processFailedLogin(resp, params, e.getMessage());
        }
    }

    @Override
    protected String getPageBody(Map<String, String[]> params, String message, boolean hideForm) {
        String responseBody = pageTemplate.replace("{form_fields}", getFormFields(params));
        String buttonLabel = "Create API Token"; // TODO: i18n
        responseBody = responseBody.replace("{message}", message);
        responseBody = responseBody.replace("{formAction}", "/createApiToken");
        responseBody = responseBody.replace("{formClass}", hideForm ? "hide" : "show");
        responseBody = responseBody.replace("{repeatPasswordFieldType}", "hidden");
        responseBody = responseBody.replace("{newPasswordFieldType}", "hidden");
        responseBody = responseBody.replace("{tokenNameFieldType}", "text");
        responseBody = responseBody.replace("{tokenScopeFieldType}", "text");
        responseBody = responseBody.replace("{buttonLabel}", buttonLabel);
        responseBody = responseBody.replace("{resultClass}", "");
        return responseBody;
    }

    protected String getResultPageBody(Map<String, String[]> params, String message) {
        String responseBody = pageTemplate.replace("{form_fields}", "");
        responseBody = responseBody.replace("{message}", message);
        responseBody = responseBody.replace("{formAction}", "/createApiToken");
        responseBody = responseBody.replace("{formClass}", "hide");
        responseBody = responseBody.replace("{repeatPasswordFieldType}", "hidden");
        responseBody = responseBody.replace("{newPasswordFieldType}", "hidden");
        responseBody = responseBody.replace("{tokenNameFieldType}", "text");
        responseBody = responseBody.replace("{tokenScopeFieldType}", "text");
        responseBody = responseBody.replace("{resultClass}", "Password");
        return responseBody;
    }

    @Override
    protected String getFormFields(Map<String, String[]> params) {
        String hiddenFormFields = "";
        String csrfToken = addCsrfToken();
        hiddenFormFields += "<input type=\"hidden\" name=\"csrf_token\" value=\"" + csrfToken + "\">";

        return hiddenFormFields;
    }

    @Deactivate
    public void deactivate() {
        httpService.unregister("/createApiToken");
    }
}
