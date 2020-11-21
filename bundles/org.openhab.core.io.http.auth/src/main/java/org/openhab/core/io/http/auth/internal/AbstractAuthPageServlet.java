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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for servlets to perform sensible operations requiring user authentication.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
public abstract class AbstractAuthPageServlet extends HttpServlet {

    protected static final long serialVersionUID = 5340598701104679840L;

    private final Logger logger = LoggerFactory.getLogger(AbstractAuthPageServlet.class);

    protected HttpService httpService;
    protected UserRegistry userRegistry;
    protected AuthenticationProvider authProvider;
    protected @Nullable Instant lastAuthenticationFailure;
    protected int authenticationFailureCount = 0;

    protected Map<String, Instant> csrfTokens = new HashMap<>();

    protected String pageTemplate;

    public AbstractAuthPageServlet(BundleContext bundleContext, @Reference HttpService httpService,
            @Reference UserRegistry userRegistry, @Reference AuthenticationProvider authProvider) {
        this.httpService = httpService;
        this.userRegistry = userRegistry;
        this.authProvider = authProvider;

        pageTemplate = "";
        URL resource = bundleContext.getBundle().getResource("pages/authorize.html");
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                pageTemplate = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot load page template", e);
            }
        }
    }

    protected abstract String getPageBody(Map<String, String[]> params, String message, boolean hideForm);

    protected abstract String getFormFields(Map<String, String[]> params);

    protected String addCsrfToken() {
        String csrfToken = UUID.randomUUID().toString().replace("-", "");
        csrfTokens.put(csrfToken, Instant.now());
        // remove old tokens (created earlier than 10 minutes ago) - this gives users a 10-minute window to sign in
        csrfTokens.entrySet().removeIf(e -> e.getValue().isBefore(Instant.now().minus(Duration.ofMinutes(10))));
        return csrfToken;
    }

    protected void removeCsrfToken(String csrfToken) {
        csrfTokens.remove(csrfToken);
    }

    protected User login(String username, String password) throws AuthenticationException {
        // Enforce a dynamic cooldown period after a failed authentication attempt: the number of
        // consecutive failures in seconds
        if (lastAuthenticationFailure != null && lastAuthenticationFailure
                .isAfter(Instant.now().minus(Duration.ofSeconds(authenticationFailureCount)))) {
            throw new AuthenticationException("Too many consecutive login attempts");
        }

        // Authenticate the user with the supplied credentials
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        Authentication auth = authProvider.authenticate(credentials);
        logger.debug("Login successful: {}", auth.getUsername());
        lastAuthenticationFailure = null;
        authenticationFailureCount = 0;
        User user = userRegistry.get(auth.getUsername());
        if (user == null) {
            throw new AuthenticationException("User not found");
        }
        return user;
    }

    protected void processFailedLogin(HttpServletResponse resp, Map<String, String[]> params, @Nullable String message)
            throws IOException {
        lastAuthenticationFailure = Instant.now();
        authenticationFailureCount += 1;
        resp.setContentType("text/html;charset=UTF-8");
        logger.warn("Authentication failed: {}", message);
        resp.getWriter().append(getPageBody(params, "Please try again.", false)); // TODO: i18n
        resp.getWriter().close();
    }
}
