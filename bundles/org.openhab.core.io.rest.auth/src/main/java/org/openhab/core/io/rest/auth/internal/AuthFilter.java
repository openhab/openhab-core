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
package org.openhab.core.io.rest.auth.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiTokenCredentials;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter is responsible for parsing credentials provided with a request, and hydrating a {@link SecurityContext}
 * from these credentials if they are valid.
 *
 * @author Yannick Schaus - initial contribution
 * @author Yannick Schaus - Allow basic authentication
 * @author Yannick Schaus - Add support for API tokens
 */
@PreMatching
@Component(configurationPid = "org.openhab.restauth", property = Constants.SERVICE_PID + "=org.openhab.restauth")
@ConfigurableService(category = "system", label = "API Security", description_uri = AuthFilter.CONFIG_URI)
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@Priority(Priorities.AUTHENTICATION)
@Provider
public class AuthFilter implements ContainerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private static final String ALT_AUTH_HEADER = "X-OPENHAB-TOKEN";
    private static final String API_TOKEN_PREFIX = "oh.";

    protected static final String CONFIG_URI = "system:restauth";
    private static final String CONFIG_ALLOW_BASIC_AUTH = "allowBasicAuth";
    private static final String CONFIG_IMPLICIT_USER_ROLE = "implicitUserRole";
    private static final String CONFIG_CACHE_EXPIRATION = "cacheExpiration";

    private boolean allowBasicAuth = false;
    private boolean implicitUserRole = true;
    private Long cacheExpiration = 6L;

    private ExpiringUserSecurityContextCache authCache = new ExpiringUserSecurityContextCache(
            Duration.ofHours(cacheExpiration).toMillis());

    static private byte[] RANDOM_BYTES = new byte[32];
    static {
        new Random().nextBytes(RANDOM_BYTES);
        ;
    }

    @Reference
    private JwtHelper jwtHelper;

    @Reference
    private UserRegistry userRegistry;

    private RegistryChangeListener<User> userRegistryListener = new RegistryChangeListener<User>() {

        @Override
        public void added(User element) {
            return;
        }

        @Override
        public void removed(User element) {
            authCache.clear();
        }

        @Override
        public void updated(User oldElement, User element) {
            authCache.clear();
        }
    };

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
        userRegistry.addRegistryChangeListener(userRegistryListener);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            Object value = properties.get(CONFIG_ALLOW_BASIC_AUTH);
            allowBasicAuth = value != null && "true".equals(value.toString());
            value = properties.get(CONFIG_IMPLICIT_USER_ROLE);
            implicitUserRole = value == null || !"false".equals(value.toString());
            value = properties.get(CONFIG_CACHE_EXPIRATION);
            if (value != null) {
                try {
                    cacheExpiration = Long.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    logger.warn("Ignoring invalid configuration value '{}' for cacheExpiration parameter.", value);
                }
            }
            authCache.clear();
        }
    }

    @Deactivate
    protected void deactivate() {
        userRegistry.removeRegistryChangeListener(userRegistryListener);
    }

    @Nullable
    private String getCacheKey(String credentials) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(RANDOM_BYTES);
            return new String(md.digest(credentials.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is available for all java distributions so this code will actually never run
            // If it does we'll just flood the cache with random values
            logger.warn("SHA-256 is not available. Cache for basic auth disabled!");
            return null;
        }
    }

    private SecurityContext authenticateBearerToken(String token) throws AuthenticationException {
        if (token.startsWith(API_TOKEN_PREFIX)) {
            UserApiTokenCredentials credentials = new UserApiTokenCredentials(token);
            Authentication auth = userRegistry.authenticate(credentials);
            User user = userRegistry.get(auth.getUsername());
            if (user == null) {
                throw new AuthenticationException("User not found in registry");
            }
            return new UserSecurityContext(user, auth, "ApiToken");
        } else {
            Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(token);
            return new JwtSecurityContext(auth);
        }
    }

    private SecurityContext authenticateBasicAuth(String credentialString) throws AuthenticationException {
        final String cacheKey = getCacheKey(credentialString);
        if (cacheKey != null) {
            final UserSecurityContext cachedValue = authCache.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }
        }

        String[] decodedCredentials;
        try {
            decodedCredentials = new String(Base64.getDecoder().decode(credentialString), "UTF-8").split(":");
        } catch (UnsupportedEncodingException e) {
            throw new AuthenticationException("Invalid base64 credentials for basic auth");
        }
        if (decodedCredentials.length != 2) {
            throw new AuthenticationException("Invalid Basic authentication credential format");
        }

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(decodedCredentials[0],
                decodedCredentials[1]);
        Authentication auth = userRegistry.authenticate(credentials);
        User user = userRegistry.get(auth.getUsername());
        if (user == null) {
            throw new AuthenticationException("User not found in registry");
        }

        UserSecurityContext context = new UserSecurityContext(user, auth, "Basic");

        if (cacheKey != null) {
            authCache.put(cacheKey, context);
        }

        return context;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            String altTokenHeader = requestContext.getHeaderString(ALT_AUTH_HEADER);
            if (altTokenHeader != null) {
                requestContext.setSecurityContext(authenticateBearerToken(altTokenHeader));
                return;
            }

            String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                String[] authParts = authHeader.split(" ");
                if (authParts.length == 2) {
                    String authType = authParts[0];
                    String authValue = authParts[1];

                    if ("Bearer".equalsIgnoreCase(authType)) {
                        requestContext.setSecurityContext(authenticateBearerToken(authValue));
                        return;
                    } else if ("Basic".equalsIgnoreCase(authType)) {
                        if (!allowBasicAuth) {
                            throw new AuthenticationException(
                                    "Basic authentication with username/password is not allowed");
                        }
                        requestContext.setSecurityContext(authenticateBasicAuth(authValue));
                    }
                }
            } else if (implicitUserRole) {
                requestContext.setSecurityContext(new AnonymousUserSecurityContext());
            }
        } catch (AuthenticationException e) {
            logger.warn("Unauthorized API request: {}", e.getMessage());
            requestContext.abortWith(JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "Invalid credentials"));
        }
    }
}
