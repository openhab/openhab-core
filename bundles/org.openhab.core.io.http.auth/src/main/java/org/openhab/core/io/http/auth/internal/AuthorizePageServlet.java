/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Set;
import java.util.UUID;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.PendingToken;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet serving the authorization page part of the OAuth2 authorization code flow.
 *
 * The page can register the first administrator account when there are no users yet in the {@link UserRegistry}, and
 * authenticates the user otherwise. It also presents the scope that is about to be granted to the client, so the user
 * can review what kind of access is being authorized. If successful, it redirects the client back to the URI which was
 * specified and creates an authorization code stored for later in the user's profile.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true, service = Servlet.class)
@HttpWhiteboardServletName(AuthorizePageServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(AuthorizePageServlet.SERVLET_PATH + "/*")
public class AuthorizePageServlet extends AbstractAuthPageServlet {

    public static final String SERVLET_PATH = "/auth";

    private static final long serialVersionUID = 5340598701104679843L;

    private final Logger logger = LoggerFactory.getLogger(AuthorizePageServlet.class);

    @Activate
    public AuthorizePageServlet(BundleContext bundleContext, @Reference UserRegistry userRegistry,
            @Reference AuthenticationProvider authProvider, @Reference LocaleProvider localeProvider) {
        super(bundleContext, userRegistry, authProvider, localeProvider);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();

        try {
            String message = "";
            String scope = params.containsKey("scope") ? params.get("scope")[0] : "";
            String clientId = params.containsKey("client_id") ? params.get("client_id")[0] : "";

            // Basic sanity check
            if (scope.contains("<") || clientId.contains("<")) {
                throw new IllegalArgumentException("invalid_request");
            }

            if (isSignupMode()) {
                message = getLocalizedMessage("auth.createaccount.prompt");
            } else {
                message = String.format(getLocalizedMessage("auth.login.prompt"), scope, clientId);
            }
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
            if (!params.containsKey("csrf_token") || !csrfTokens.containsKey(params.get("csrf_token")[0])) {
                throw new AuthenticationException("CSRF check failed");
            }
            if (!params.containsKey("redirect_uri")) {
                throw new IllegalArgumentException("invalid_request");
            }
            if (!params.containsKey("response_type")) {
                throw new IllegalArgumentException("unsupported_response_type");
            }
            if (!params.containsKey("client_id")) {
                throw new IllegalArgumentException("unauthorized_client");
            }
            if (!params.containsKey("scope")) {
                throw new IllegalArgumentException("invalid_scope");
            }

            removeCsrfToken(params.get("csrf_token")[0]);

            String baseRedirectUri = params.get("redirect_uri")[0];
            String responseType = params.get("response_type")[0];
            String clientId = params.get("client_id")[0];
            String scope = params.get("scope")[0];

            if (!"code".equals(responseType)) {
                throw new AuthenticationException("unsupported_response_type");
            }

            if (!clientId.equals(baseRedirectUri)) {
                throw new IllegalArgumentException("unauthorized_client");
            }

            String username = params.get("username")[0];
            String password = params.get("password")[0];

            User user;
            if (isSignupMode()) {
                // Create a first administrator account with the supplied credentials

                // first verify the password confirmation and bail out if necessary
                if (!params.containsKey("password_repeat") || !password.equals(params.get("password_repeat")[0])) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.getWriter()
                            .append(getPageBody(params, getLocalizedMessage("auth.password.confirm.fail"), false));
                    resp.getWriter().close();
                    return;
                }

                user = userRegistry.register(username, password, Set.of(Role.ADMIN));
                logger.info("First user account created: {}", username);
            } else {
                user = login(username, password);
            }

            String authorizationCode = UUID.randomUUID().toString().replace("-", "");

            if (user instanceof ManagedUser) {
                String codeChallenge = params.containsKey("code_challenge") ? params.get("code_challenge")[0] : null;
                String codeChallengeMethod = params.containsKey("code_challenge_method")
                        ? params.get("code_challenge_method")[0]
                        : null;
                ManagedUser managedUser = (ManagedUser) user;
                PendingToken pendingToken = new PendingToken(authorizationCode, clientId, baseRedirectUri, scope,
                        codeChallenge, codeChallengeMethod);
                managedUser.setPendingToken(pendingToken);
                userRegistry.update(managedUser);
            }

            String state = params.containsKey("state") ? params.get("state")[0] : null;
            resp.addHeader(HttpHeaders.LOCATION, getRedirectUri(baseRedirectUri, authorizationCode, null, state));
            resp.setStatus(HttpStatus.MOVED_TEMPORARILY_302);
        } catch (AuthenticationException e) {
            processFailedLogin(resp, req.getRemoteAddr(), params, e.getMessage());
        } catch (IllegalArgumentException e) {
            @Nullable
            String baseRedirectUri = params.containsKey("redirect_uri") ? params.get("redirect_uri")[0] : null;
            @Nullable
            String state = params.containsKey("state") ? params.get("state")[0] : null;
            if (baseRedirectUri != null) {
                resp.addHeader(HttpHeaders.LOCATION, getRedirectUri(baseRedirectUri, null, e.getMessage(), state));
                resp.setStatus(HttpStatus.MOVED_TEMPORARILY_302);
            } else {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().append(e.getMessage());
                resp.getWriter().close();
            }
        }
    }

    @Override
    protected String getPageBody(Map<String, String[]> params, String message, boolean hideForm) {
        String responseBody = getPageTemplate().replace("{form_fields}", getFormFields(params));
        String repeatPasswordFieldType = isSignupMode() ? "password" : "hidden";
        String buttonLabel = getLocalizedMessage(isSignupMode() ? "auth.button.createaccount" : "auth.button.signin");
        responseBody = responseBody.replace("{message}", message);
        responseBody = responseBody.replace("{formAction}", "/auth");
        responseBody = responseBody.replace("{formClass}", "show");
        responseBody = responseBody.replace("{repeatPasswordFieldType}", repeatPasswordFieldType);
        responseBody = responseBody.replace("{newPasswordFieldType}", "hidden");
        responseBody = responseBody.replace("{tokenNameFieldType}", "hidden");
        responseBody = responseBody.replace("{tokenScopeFieldType}", "hidden");
        responseBody = responseBody.replace("{buttonLabel}", buttonLabel);
        responseBody = responseBody.replace("{resultClass}", "");
        return responseBody;
    }

    @Override
    protected String getFormFields(Map<String, String[]> params) {
        String hiddenFormFields = "";

        if (!params.containsKey("redirect_uri")) {
            throw new IllegalArgumentException("invalid_request");
        }
        if (!params.containsKey("response_type")) {
            throw new IllegalArgumentException("unsupported_response_type");
        }
        if (!params.containsKey("client_id")) {
            throw new IllegalArgumentException("unauthorized_client");
        }
        if (!params.containsKey("scope")) {
            throw new IllegalArgumentException("invalid_scope");
        }
        String csrfToken = addCsrfToken();
        String redirectUri = params.get("redirect_uri")[0];
        String responseType = params.get("response_type")[0];
        String clientId = params.get("client_id")[0];
        String scope = params.get("scope")[0];
        String state = params.containsKey("state") ? params.get("state")[0] : null;
        String codeChallenge = params.containsKey("code_challenge") ? params.get("code_challenge")[0] : null;
        String codeChallengeMethod = params.containsKey("code_challenge_method")
                ? params.get("code_challenge_method")[0]
                : null;
        hiddenFormFields += "<input type=\"hidden\" name=\"csrf_token\" value=\"" + csrfToken + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"redirect_uri\" value=\"" + redirectUri + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"response_type\" value=\"" + responseType + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"client_id\" value=\"" + clientId + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"scope\" value=\"" + scope + "\">";
        if (state != null) {
            hiddenFormFields += "<input type=\"hidden\" name=\"state\" value=\"" + state + "\">";
        }
        if (codeChallenge != null && codeChallengeMethod != null) {
            hiddenFormFields += "<input type=\"hidden\" name=\"code_challenge\" value=\"" + codeChallenge + "\">";
            hiddenFormFields += "<input type=\"hidden\" name=\"code_challenge_method\" value=\"" + codeChallengeMethod
                    + "\">";
        }

        return hiddenFormFields;
    }

    private String getRedirectUri(String baseRedirectUri, @Nullable String authorizationCode, @Nullable String error,
            @Nullable String state) {
        String redirectUri = baseRedirectUri;

        if (authorizationCode != null) {
            redirectUri += "?code=" + authorizationCode;
        } else if (error != null) {
            redirectUri += "?error=" + error;
        }

        if (state != null) {
            redirectUri += "&state=" + state;
        }

        return redirectUri;
    }

    private boolean isSignupMode() {
        return userRegistry.getAll().isEmpty();
    }
}
