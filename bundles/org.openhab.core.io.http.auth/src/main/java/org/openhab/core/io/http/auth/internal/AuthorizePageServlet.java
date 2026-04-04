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
import java.util.HashMap;
import java.util.Locale;
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
import org.osgi.service.component.annotations.Modified;
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
 * A redirect URI on another origin than the one serving this page (e.g. an AI assistant's OAuth callback) is shown
 * prominently, and the user has to type its host name before signing in, so a code is never handed to an external
 * site the user didn't knowingly pick.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true, service = Servlet.class, configurationPid = "org.openhab.restauth")
@HttpWhiteboardServletName(AuthorizePageServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(AuthorizePageServlet.SERVLET_PATH + "/*")
public class AuthorizePageServlet extends AbstractAuthPageServlet {

    public static final String SERVLET_PATH = "/auth";

    static final String CONFIG_ALLOW_EXTERNAL_SITES = "allowExternalSites";
    static final String CONFIRM_HOST_PARAM = "confirm_host";
    static final int MAX_REDIRECT_URI_LENGTH = 2048;

    @Serial
    private static final long serialVersionUID = 5340598701104679843L;

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    // set by the servlet to show the host confirmation field, never taken from the request
    private static final String EXTERNAL_HOST_PARAM = "external_host";

    private final Logger logger = LoggerFactory.getLogger(AuthorizePageServlet.class);

    private volatile boolean allowExternalSites = true;

    @Activate
    public AuthorizePageServlet(BundleContext bundleContext, @Reference UserRegistry userRegistry,
            @Reference AuthenticationProvider authProvider, @Reference LocaleProvider localeProvider,
            @Nullable Map<String, Object> config) {
        super(bundleContext, userRegistry, authProvider, localeProvider);
        modified(config);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> config) {
        Object value = config != null ? config.get(CONFIG_ALLOW_EXTERNAL_SITES) : null;
        allowExternalSites = value == null || Boolean.parseBoolean(value.toString());
    }

    @Override
    @NonNullByDefault({})
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = pageParams(req);
        denyFraming(resp);

        try {
            validateAuthorizationRequest(params);
            String redirectUri = params.get("redirect_uri")[0];
            String scope = escape(params.get("scope")[0]);
            String clientId = escape(params.get("client_id")[0]);

            @Nullable
            String externalHost = externalHost(req, redirectUri);
            String message;
            boolean hideForm = false;
            if (externalHost != null && isSignupMode()) {
                message = getLocalizedMessage("auth.redirect.signup.required");
                hideForm = true;
            } else if (externalHost != null) {
                params.put(EXTERNAL_HOST_PARAM, new String[] { externalHost });
                message = externalPrompt(externalHost, redirectUri);
            } else if (isSignupMode()) {
                message = getLocalizedMessage("auth.createaccount.prompt");
            } else {
                message = String.format(getLocalizedMessage("auth.login.prompt"), scope, clientId);
            }
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(getPageBody(params, message, hideForm));
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
        Map<String, String[]> params = pageParams(req);
        denyFraming(resp);
        try {
            // The authorization request is validated before the login is even looked at: a failing login renders
            // the page again from these very parameters, and an exception thrown while doing so could no longer be
            // handled below - it would escape as an HTTP 500 instead of the login page.
            validateAuthorizationRequest(params);
            String baseRedirectUri = params.get("redirect_uri")[0];
            @Nullable
            String externalHost = externalHost(req, baseRedirectUri);
            if (externalHost != null) {
                // the first account has to be created in openHAB itself, not handed to an external site
                if (isSignupMode()) {
                    throw new IllegalArgumentException("access_denied");
                }
                // set before any failure below, so a retry page still asks for the site name
                params.put(EXTERNAL_HOST_PARAM, new String[] { externalHost });
            }
            if (!params.containsKey("csrf_token") || !csrfTokens.containsKey(params.get("csrf_token")[0])) {
                throw new AuthenticationException("CSRF check failed");
            }
            if (externalHost != null) {
                String typedHost = params.containsKey(CONFIRM_HOST_PARAM) ? params.get(CONFIRM_HOST_PARAM)[0] : "";
                if (!externalHost.equalsIgnoreCase(typedHost.strip())) {
                    logger.warn("Site name confirmation for {} failed from {}", externalHost, req.getRemoteAddr());
                    removeCsrfToken(params.get("csrf_token")[0]);
                    String message = "<div class=\"error\">" + getLocalizedMessage("auth.redirect.confirm.fail")
                            + "</div>" + externalPrompt(externalHost, baseRedirectUri);
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.getWriter().append(getPageBody(params, message, false));
                    resp.getWriter().close();
                    return;
                }
            }

            if (!params.containsKey("username")) {
                throw new AuthenticationException("no username");
            }
            if (!params.containsKey("password")) {
                throw new AuthenticationException("no password");
            }

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

            if (externalHost != null) {
                logger.info("User {} granted access to the external site {}", user.getName(), baseRedirectUri);
            }

            String authorizationCode = UUID.randomUUID().toString().replace("-", "");

            if (user instanceof AuthenticatedUser authenticatedUser) {
                PendingToken pendingToken = new PendingToken(authorizationCode, clientId, baseRedirectUri, scope,
                        codeChallenge, codeChallengeMethod);
                authenticatedUser.setPendingToken(pendingToken);
                userRegistry.update(authenticatedUser);
            }

            String state = params.containsKey("state") ? params.get("state")[0] : null;
            String location = getRedirectUri(baseRedirectUri, authorizationCode, null, state);
            if (externalHost != null) {
                // The login page's CSP has "form-action 'self'", so the browser won't follow a redirect from this
                // form to another site. Send it there from a new page instead.
                resp.setHeader("Cache-Control", "no-store");
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().append(getContinuePage(externalHost, location));
                resp.getWriter().close();
            } else {
                resp.addHeader(HttpHeaders.LOCATION, location);
                resp.setStatus(HttpStatus.MOVED_TEMPORARILY_302);
            }
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
        return renderPage(getPageTemplate(), message, getFormFields(params), hideForm);
    }

    private String getContinuePage(String host, String location) {
        String url = escape(location);
        String template = getPageTemplate().replace("</head>",
                "<meta http-equiv=\"refresh\" content=\"0;url=" + url + "\">\n</head>");
        String message = String.format(getLocalizedMessage("auth.redirect.continue"),
                "<a href=\"" + url + "\">" + escape(host) + "</a>");
        return renderPage(template, message, "", true);
    }

    private String renderPage(String template, String message, String formFields, boolean hideForm) {
        String responseBody = template.replace("{form_fields}", formFields);
        String repeatPasswordFieldType = isSignupMode() ? "password" : "hidden";
        String buttonLabel = getLocalizedMessage(isSignupMode() ? "auth.button.createaccount" : "auth.button.signin");
        responseBody = responseBody.replace("{message}", message);
        responseBody = responseBody.replace("{formAction}", "/auth");
        responseBody = responseBody.replace("{formClass}", hideForm ? "hide" : "show");
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
        if (params.containsKey(EXTERNAL_HOST_PARAM)) {
            // never prefilled: the user has to type it
            hiddenFormFields += "<div><input class=\"field confirm\" type=\"text\" name=\"" + CONFIRM_HOST_PARAM
                    + "\" placeholder=\""
                    + escape(String.format(getLocalizedMessage("auth.placeholder.confirmhost"),
                            params.get(EXTERNAL_HOST_PARAM)[0]))
                    + "\" autocomplete=\"off\" autocapitalize=\"none\" spellcheck=\"false\" required></div>";
        }

        return hiddenFormFields;
    }

    @Override
    protected String getFailedLoginMessage(Map<String, String[]> params) {
        String message = super.getFailedLoginMessage(params);
        if (params.containsKey(EXTERNAL_HOST_PARAM)) {
            message += " " + externalPrompt(params.get(EXTERNAL_HOST_PARAM)[0], params.get("redirect_uri")[0]);
        }
        return message;
    }

    private String externalPrompt(String host, String redirectUri) {
        return "<div class=\"site\">" + escape(host) + "</div>" + getLocalizedMessage("auth.login.prompt.external")
                + "<div class=\"warning\">" + getLocalizedMessage("auth.login.prompt.external.warning") + "</div>"
                + "<div class=\"uri\">"
                + String.format(getLocalizedMessage("auth.login.prompt.external.uri"), escape(redirectUri)) + "</div>"
                + getLocalizedMessage("auth.login.prompt.external.confirm");
    }

    // braces too, so a value can't contain a page placeholder like {message}
    private static String escape(String value) {
        return StringUtils.escapeXml(value).replace("{", "&#123;").replace("}", "&#125;");
    }

    private static Map<String, String[]> pageParams(HttpServletRequest req) {
        Map<String, String[]> params = new HashMap<>(req.getParameterMap());
        params.remove(EXTERNAL_HOST_PARAM);
        return params;
    }

    private static void denyFraming(HttpServletResponse resp) {
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
    }

    /**
     * Returns the host the user has to confirm when the redirect URI is on another origin, or null when it's on the
     * origin serving this page.
     *
     * @throws IllegalArgumentException if the redirect URI is on another origin and can't be used
     */
    @Nullable
    String externalHost(HttpServletRequest req, String redirectUri) {
        if (isSafeRedirectUri(req, redirectUri)) {
            return null;
        }
        if (allowExternalSites) {
            @Nullable
            String host = allowedExternalHost(redirectUri);
            if (host != null) {
                return host;
            }
        }
        throw new IllegalArgumentException("unauthorized_client");
    }

    /**
     * Returns the lower-cased host of an external redirect URI, or null if it can't be used: it has to be https (or
     * http to the local machine), without user info or fragment. {@link URI} has no host for non-ASCII names, so
     * those are rejected and the host shown to the user can't hide look-alike characters.
     */
    static @Nullable String allowedExternalHost(String redirectUri) {
        if (redirectUri.length() > MAX_REDIRECT_URI_LENGTH) {
            return null;
        }
        try {
            URI uri = new URI(redirectUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null || uri.getFragment() != null) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            if ("https".equalsIgnoreCase(scheme) || ("http".equalsIgnoreCase(scheme) && isLoopback(host))) {
                return host;
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isLoopback(String host) {
        return "localhost".equals(host) || "[::1]".equals(host) || "[0:0:0:0:0:0:0:1]".equals(host)
                || host.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }

    private String hiddenInput(String name, @Nullable String value) {
        return value == null ? "" : "<input type=\"hidden\" name=\"" + name + "\" value=\"" + escape(value) + "\">";
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
     * Checks that the {@code redirect_uri} points back to the same origin (scheme, host and port) the browser used to
     * reach this servlet. Since clients aren't pre-registered (the {@code client_id} is required to equal the
     * {@code redirect_uri}), this is the only origin trusted without the user confirming it, and it prevents an
     * attacker from supplying an arbitrary {@code redirect_uri} to have the authorization code (or an error message)
     * silently delivered to a site they control.
     */
    static boolean isSafeRedirectUri(HttpServletRequest req, String redirectUri) {
        try {
            URI uri = new URI(redirectUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null || uri.getFragment() != null) {
                return false;
            }
            @Nullable
            String forwardedProto = firstHeaderValue(req, X_FORWARDED_PROTO);
            @Nullable
            String forwardedHost = firstHeaderValue(req, X_FORWARDED_HOST);
            if (forwardedProto == null && forwardedHost == null) {
                return scheme.equalsIgnoreCase(req.getScheme()) && host.equalsIgnoreCase(req.getServerName())
                        && effectivePort(uri) == req.getServerPort();
            }

            // Safe to trust: a page can't make the browser send these headers.
            String originHost = forwardedHost != null ? forwardedHost : req.getHeader(HttpHeaders.HOST);
            if (originHost == null) {
                return false;
            }
            URI origin = new URI((forwardedProto != null ? forwardedProto : req.getScheme()) + "://" + originHost);
            return scheme.equalsIgnoreCase(origin.getScheme()) && host.equalsIgnoreCase(origin.getHost())
                    && effectivePort(uri) == effectivePort(origin);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static int effectivePort(URI uri) {
        int port = uri.getPort();
        return port != -1 ? port : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
    }

    private static @Nullable String firstHeaderValue(HttpServletRequest req, String name) {
        String value = req.getHeader(name);
        if (value == null) {
            return null;
        }
        // the first proxy's value is the browser's
        int comma = value.indexOf(',');
        value = (comma >= 0 ? value.substring(0, comma) : value).trim();
        return value.isEmpty() ? null : value;
    }

    private boolean isSignupMode() {
        return userRegistry.getAll().isEmpty();
    }
}
