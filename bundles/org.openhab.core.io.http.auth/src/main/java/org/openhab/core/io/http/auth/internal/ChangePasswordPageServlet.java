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
import org.openhab.core.i18n.LocaleProvider;
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
 * A servlet serving a page allowing users to change their password, after confirming their identity by signing in.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true)
public class ChangePasswordPageServlet extends AbstractAuthPageServlet {

    private static final long serialVersionUID = 5340598701104679843L;

    private final Logger logger = LoggerFactory.getLogger(ChangePasswordPageServlet.class);

    @Activate
    public ChangePasswordPageServlet(BundleContext bundleContext, @Reference HttpService httpService,
            @Reference UserRegistry userRegistry, @Reference AuthenticationProvider authProvider,
            @Reference LocaleProvider localeProvider) {
        super(bundleContext, httpService, userRegistry, authProvider, localeProvider);
        try {
            httpService.registerServlet("/changePassword", this, null, null);
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during change password page registration: {}", e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();

        try {
            String message = "";

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
            if (!params.containsKey("new_password")) {
                throw new AuthenticationException("no new password");
            }
            if (!params.containsKey("csrf_token") || !csrfTokens.containsKey(params.get("csrf_token")[0])) {
                throw new AuthenticationException("CSRF check failed");
            }

            removeCsrfToken(params.get("csrf_token")[0]);

            String username = params.get("username")[0];
            String password = params.get("password")[0];
            String newPassword = params.get("new_password")[0];

            if (!params.containsKey("password_repeat") || !newPassword.equals(params.get("password_repeat")[0])) {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().append(getPageBody(params, getLocalizedMessage("auth.password.confirm.fail"), false));
                resp.getWriter().close();
                return;
            }

            User user = login(username, password);

            if (user instanceof ManagedUser) {
                userRegistry.changePassword(user, newPassword);
            } else {
                throw new AuthenticationException("User is not managed");
            }

            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(getResultPageBody(params, getLocalizedMessage("auth.changepassword.success")));
            resp.getWriter().close();
        } catch (AuthenticationException e) {
            processFailedLogin(resp, params, e.getMessage());
        }
    }

    @Override
    protected String getPageBody(Map<String, String[]> params, String message, boolean hideForm) {
        String responseBody = getPageTemplate().replace("{form_fields}", getFormFields(params));
        String buttonLabel = getLocalizedMessage("auth.button.changepassword");
        responseBody = responseBody.replace("{message}", message);
        responseBody = responseBody.replace("{formAction}", "/changePassword");
        responseBody = responseBody.replace("{formClass}", hideForm ? "hide" : "show");
        responseBody = responseBody.replace("{repeatPasswordFieldType}", "password");
        responseBody = responseBody.replace("{newPasswordFieldType}", "password");
        responseBody = responseBody.replace("{tokenNameFieldType}", "hidden");
        responseBody = responseBody.replace("{tokenScopeFieldType}", "hidden");
        responseBody = responseBody.replace("{buttonLabel}", buttonLabel);
        responseBody = responseBody.replace("{resultClass}", "");
        return responseBody;
    }

    protected String getResultPageBody(Map<String, String[]> params, String message) {
        String responseBody = getPageTemplate().replace("{form_fields}", "");
        responseBody = responseBody.replace("{message}", message);
        responseBody = responseBody.replace("{formAction}", "/changePassword");
        responseBody = responseBody.replace("{formClass}", "hide");
        responseBody = responseBody.replace("{repeatPasswordFieldType}", "password");
        responseBody = responseBody.replace("{newPasswordFieldType}", "password");
        responseBody = responseBody.replace("{tokenNameFieldType}", "hidden");
        responseBody = responseBody.replace("{tokenScopeFieldType}", "hidden");
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
        httpService.unregister("/changePassword");
    }
}
