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
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationManager;
import org.openhab.core.auth.Credentials;
import org.openhab.core.io.http.Handler;
import org.openhab.core.io.http.HandlerContext;
import org.openhab.core.io.http.HandlerPriorities;
import org.openhab.core.io.http.auth.CredentialsExtractor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request handler which allows to verify authentication.
 *
 * @author Łukasz Dywicki - Initial contribution.
 */
@Component(configurationPid = "org.openhab.auth")
public class AuthenticationHandler implements Handler {

    private static final String AUTHENTICATION_ENABLED = "enabled";
    private static final String AUTHENTICATION_ENDPOINT = "loginUri";
    private static final String DEFAULT_LOGIN_URI = "/login";
    static final String REDIRECT_PARAM_NAME = "redirect";

    private final Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    private final List<CredentialsExtractor<HttpServletRequest>> extractors = new CopyOnWriteArrayList<>();

    private AuthenticationManager authenticationManager;

    // configuration properties
    private boolean enabled = false;
    private String loginUri = DEFAULT_LOGIN_URI;

    @Override
    public int getPriority() {
        return HandlerPriorities.AUTHENTICATION;
    }

    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
            final HandlerContext context) throws Exception {
        String requestUri = request.getRequestURI();
        if (this.enabled && isSecured(requestUri, request.getMethod())) {
            if (authenticationManager == null) {
                throw new AuthenticationException("Failed to authenticate request.");
            }

            int found = 0, failed = 0;
            for (CredentialsExtractor<HttpServletRequest> extractor : extractors) {
                Optional<Credentials> extracted = extractor.retrieveCredentials(request);
                if (extracted.isPresent()) {
                    found++;
                    Credentials credentials = extracted.get();
                    try {
                        Authentication authentication = authenticationManager.authenticate(credentials);
                        request.setAttribute(Authentication.class.getName(), authentication);
                        context.execute(request, response);
                        return;
                    } catch (AuthenticationException e) {
                        failed++;
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to authenticate using credentials {}", credentials, e);
                        } else {
                            logger.info("Failed to authenticate using credentials {}", credentials);
                        }
                    }
                }
            }

            throw new AuthenticationException("Could not authenticate request. Found " + found
                    + " credentials in request out of which " + failed + " were invalid");
        }
    }

    @Override
    public void handleError(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        Object error = request.getAttribute(HandlerContext.ERROR_ATTRIBUTE);

        if (response.getStatus() == 403 || response.getStatus() == 401) {
            // already handled
            return;
        }

        if (error instanceof AuthenticationException) {
            // force client redirect
            String redirectUri = loginUri + "?" + REDIRECT_PARAM_NAME + "=" + request.getRequestURI();
            response.setHeader("Location", redirectUri);
            try {
                PrintWriter writer = response.getWriter();
                writer.println("<html><head>");
                writer.println("<meta http-equiv=\"refresh\" content=\"0; url=" + redirectUri + "\" />");
                writer.println("</head><body>Redirecting to login page</body></html>");
                writer.flush();
            } catch (IOException e) {
                logger.warn("Couldn't generate or send client response", e);
            }
        } else {
            // let other handler handle error
            context.execute(request, response);
        }
    }

    private boolean isSecured(String requestUri, String method) {
        if (requestUri.startsWith(loginUri) && !"post".equalsIgnoreCase(method)) {
            return false;
        }

        // TODO add decision logic so not all URIs gets secured but only these which are told to be
        return true;
    }

    @Activate
    void activate(Map<String, Object> properties) {
        modified(properties);
    }

    @Modified
    void modified(Map<String, Object> properties) {
        Object authenticationEnabled = properties.get(AUTHENTICATION_ENABLED);
        if (authenticationEnabled != null) {
            this.enabled = Boolean.valueOf(authenticationEnabled.toString());
        }

        Object loginUri = properties.get(AUTHENTICATION_ENDPOINT);
        if (loginUri != null && loginUri instanceof String) {
            this.loginUri = (String) loginUri;
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public void unsetAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(context=javax.servlet.http.HttpServletRequest)")
    public void addCredentialsExtractor(CredentialsExtractor<HttpServletRequest> extractor) {
        this.extractors.add(extractor);
    }

    public void removeCredentialsExtractor(CredentialsExtractor<HttpServletRequest> extractor) {
        this.extractors.remove(extractor);
    }

}
