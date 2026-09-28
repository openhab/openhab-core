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
package org.openhab.core.io.http.auth.internal;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.auth.AuthenticatedUser;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.PendingToken;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.util.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

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

    @Serial
    private static final long serialVersionUID = 5340598701104679843L;

    private final Logger logger = LoggerFactory.getLogger(AuthorizePageServlet.class);

    @Activate
    public AuthorizePageServlet(BundleContext bundleContext, @Reference UserRegistry userRegistry,
            @Reference AuthenticationProvider authProvider, @Reference LocaleProvider localeProvider) {
        super(bundleContext, userRegistry, authProvider, localeProvider);
    }

    @Override
    @NonNullByDefault({})
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();

        try {
            String message;
            String scope = params.containsKey("scope") ? params.get("scope")[0] : "";
            String clientId = params.containsKey("client_id") ? params.get("client_id")[0] : "";

            validateAuthorizationRequest(params);
            validateRedirectUriOrigin(req, params.get("redirect_uri")[0]);

            if (isSignupMode()) {
                message = getLocalizedMessage("auth.createaccount.prompt");
            } else {
                message = String.format(getLocalizedMessage("auth.login.prompt"), StringUtils.escapeXml(scope),
                        StringUtils.escapeXml(clientId));
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
    @NonNullByDefault({})
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();
        try {
            // The authorization request is validated before the login is even looked at: a failing login renders
            // the page again from these very parameters, and an exception thrown while doing so could no longer be
            // handled below - it would escape as an HTTP 500 instead of the login page.
            validateAuthorizationRequest(params);
            validateRedirectUriOrigin(req, params.get("redirect_uri")[0]);

            if (!params.containsKey("username")) {
                throw new AuthenticationException("no username");
            }
            if (!params.containsKey("password")) {
                throw new AuthenticationException("no password");
            }
            if (!params.containsKey("csrf_token") || !csrfTokens.containsKey(params.get("csrf_token")[0])) {
                throw new AuthenticationException("CSRF check failed");
            }

            String baseRedirectUri = params.get("redirect_uri")[0];
            String clientId = params.get("client_id")[0];
            String scope = params.get("scope")[0];
            @Nullable
            String codeChallenge = params.containsKey("code_challenge") ? params.get("code_challenge")[0] : null;
            @Nullable
            String codeChallengeMethod = params.containsKey("code_challenge_method")
                    ? params.get("code_challenge_method")[0]
                    : null;

            removeCsrfToken(params.get("csrf_token")[0]);

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

            if (user instanceof AuthenticatedUser authenticatedUser) {
                PendingToken pendingToken = new PendingToken(authorizationCode, clientId, baseRedirectUri, scope,
                        codeChallenge, codeChallengeMethod);
                authenticatedUser.setPendingToken(pendingToken);
                userRegistry.update(authenticatedUser);
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
            if (baseRedirectUri != null && isSafeRedirectUri(req, baseRedirectUri)) {
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
        // No validation takes place here: doGet and doPost reject an invalid authorization request before the page
        // is rendered, and throwing from this method would escape its callers as an HTTP 500. Every value below is
        // HTML-escaped, so reflecting it is safe in any case.
        hiddenFormFields += hiddenInput("csrf_token", csrfToken);
        hiddenFormFields += hiddenInput("redirect_uri", redirectUri);
        hiddenFormFields += hiddenInput("response_type", responseType);
        hiddenFormFields += hiddenInput("client_id", clientId);
        hiddenFormFields += hiddenInput("scope", scope);
        if (state != null) {
            hiddenFormFields += hiddenInput("state", state);
        }
        hiddenFormFields += hiddenInput("code_challenge", codeChallenge);
        hiddenFormFields += hiddenInput("code_challenge_method", codeChallengeMethod);

        return hiddenFormFields;
    }

    private String hiddenInput(String name, @Nullable String value) {
        return value == null ? ""
                : "<input type=\"hidden\" name=\"" + name + "\" value=\"" + StringUtils.escapeXml(value) + "\">";
    }

    private String getRedirectUri(String baseRedirectUri, @Nullable String authorizationCode, @Nullable String error,
            @Nullable String state) {
        String redirectUri = baseRedirectUri;
        String separator = baseRedirectUri.contains("?") ? "&" : "?";

        if (authorizationCode != null) {
            redirectUri += separator + "code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8);
            separator = "&";
        } else if (error != null) {
            redirectUri += separator + "error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);
            separator = "&";
        }

        if (state != null) {
            redirectUri += separator + "state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }

        return redirectUri;
    }

    static void validateAuthorizationRequest(Map<String, String[]> params) {
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

        @Nullable
        String codeChallenge = params.containsKey("code_challenge") ? params.get("code_challenge")[0] : null;
        @Nullable
        String codeChallengeMethod = params.containsKey("code_challenge_method")
                ? params.get("code_challenge_method")[0]
                : null;
        validateAuthorizationRequest(params.get("redirect_uri")[0], params.get("client_id")[0],
                params.get("response_type")[0], codeChallenge, codeChallengeMethod);
    }

    static void validateAuthorizationRequest(String redirectUri, String clientId, String responseType,
            @Nullable String codeChallenge, @Nullable String codeChallengeMethod) {
        if (!"code".equals(responseType)) {
            throw new IllegalArgumentException("unsupported_response_type");
        }
        if (!clientId.equals(redirectUri)) {
            throw new IllegalArgumentException("unauthorized_client");
        }
        if (codeChallenge == null || !"S256".equals(codeChallengeMethod)) {
            throw new IllegalArgumentException("invalid_request");
        }
    }

    /**
     * Ensures the {@code redirect_uri} points back to the same origin (scheme, host and port) the browser used to
     * reach this servlet. Since clients aren't pre-registered (the {@code client_id} is required to equal the
     * {@code redirect_uri}), this is the only origin we can meaningfully trust, and it prevents an attacker from
     * supplying an arbitrary {@code redirect_uri} to have the authorization code (or an error message) delivered to
     * a site they control.
     */
    static void validateRedirectUriOrigin(HttpServletRequest req, String redirectUri) {
        if (!isSafeRedirectUri(req, redirectUri)) {
            throw new IllegalArgumentException("unauthorized_client");
        }
    }

    static boolean isSafeRedirectUri(HttpServletRequest req, String redirectUri) {
        try {
            URI uri = new URI(redirectUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null || uri.getFragment() != null) {
                return false;
            }
            int port = uri.getPort();
            int effectivePort = port != -1 ? port : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
            return scheme.equalsIgnoreCase(req.getScheme()) && host.equalsIgnoreCase(req.getServerName())
                    && effectivePort == req.getServerPort();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isSignupMode() {
        return userRegistry.getAll().isEmpty();
    }
}
