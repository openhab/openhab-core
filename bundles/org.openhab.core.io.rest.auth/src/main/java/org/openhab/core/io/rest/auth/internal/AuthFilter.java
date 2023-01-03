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
package org.openhab.core.io.rest.auth.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiTokenCredentials;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigParser;
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
 * @author Sebastian Gerber - Add basic auth caching
 * @author Kai Kreuzer - Add null annotations, constructor initialization
 * @author Miguel Álvarez - Add trusted networks for implicit user role
 */
@PreMatching
@Component(configurationPid = "org.openhab.restauth", property = Constants.SERVICE_PID + "=org.openhab.restauth")
@ConfigurableService(category = "system", label = "API Security", description_uri = AuthFilter.CONFIG_URI)
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@Priority(Priorities.AUTHENTICATION)
@Provider
@NonNullByDefault
public class AuthFilter implements ContainerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    static final String ALT_AUTH_HEADER = "X-OPENHAB-TOKEN";
    static final String API_TOKEN_PREFIX = "oh.";
    protected static final String CONFIG_URI = "system:restauth";
    static final String CONFIG_ALLOW_BASIC_AUTH = "allowBasicAuth";
    static final String CONFIG_IMPLICIT_USER_ROLE = "implicitUserRole";
    static final String CONFIG_TRUSTED_NETWORKS = "trustedNetworks";
    static final String CONFIG_CACHE_EXPIRATION = "cacheExpiration";

    private boolean allowBasicAuth = false;
    private boolean implicitUserRole = true;
    private List<CIDR> trustedNetworks = List.of();
    private Long cacheExpiration = 6L;

    private ExpiringUserSecurityContextCache authCache = new ExpiringUserSecurityContextCache(
            Duration.ofHours(cacheExpiration).toMillis());

    private static final byte[] RANDOM_BYTES = new byte[32];

    private final JwtHelper jwtHelper;
    private final UserRegistry userRegistry;

    @Context
    private @NonNullByDefault({}) HttpServletRequest servletRequest;

    private RegistryChangeListener<User> userRegistryListener = new RegistryChangeListener<>() {

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
    public AuthFilter(@Reference JwtHelper jwtHelper, @Reference UserRegistry userRegistry) {
        this.jwtHelper = jwtHelper;
        this.userRegistry = userRegistry;
        new Random().nextBytes(RANDOM_BYTES);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
        userRegistry.addRegistryChangeListener(userRegistryListener);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            allowBasicAuth = ConfigParser.valueAsOrElse(properties.get(CONFIG_ALLOW_BASIC_AUTH), Boolean.class, false);
            implicitUserRole = ConfigParser.valueAsOrElse(properties.get(CONFIG_IMPLICIT_USER_ROLE), Boolean.class,
                    true);
            trustedNetworks = parseTrustedNetworks(
                    ConfigParser.valueAsOrElse(properties.get(CONFIG_TRUSTED_NETWORKS), String.class, ""));
            try {
                cacheExpiration = ConfigParser.valueAsOrElse(properties.get(CONFIG_CACHE_EXPIRATION), Long.class, 6L);
            } catch (NumberFormatException e) {
                logger.warn("Ignoring invalid configuration value '{}' for cacheExpiration parameter.",
                        properties.get(CONFIG_CACHE_EXPIRATION));
            }
            authCache.clear();
        }
    }

    @Deactivate
    protected void deactivate() {
        userRegistry.removeRegistryChangeListener(userRegistryListener);
    }

    private @Nullable String getCacheKey(String credentials) {
        if (cacheExpiration == 0) {
            // caching is disabled
            return null;
        }
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

        String[] decodedCredentials = new String(Base64.getDecoder().decode(credentialString), StandardCharsets.UTF_8)
                .split(":");
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
    public void filter(@Nullable ContainerRequestContext requestContext) throws IOException {
        if (requestContext != null) {
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
                            String[] decodedCredentials = new String(Base64.getDecoder().decode(authValue), "UTF-8")
                                    .split(":");
                            if (decodedCredentials.length > 2) {
                                throw new AuthenticationException("Invalid Basic authentication credential format");
                            }
                            switch (decodedCredentials.length) {
                                case 1:
                                    requestContext.setSecurityContext(authenticateBearerToken(decodedCredentials[0]));
                                    break;
                                case 2:
                                    if (!allowBasicAuth) {
                                        throw new AuthenticationException(
                                                "Basic authentication with username/password is not allowed");
                                    }
                                    requestContext.setSecurityContext(authenticateBasicAuth(authValue));
                            }
                        }
                    }
                } else if (isImplicitUserRole(requestContext)) {
                    requestContext.setSecurityContext(new AnonymousUserSecurityContext());
                }
            } catch (AuthenticationException e) {
                logger.warn("Unauthorized API request from {}: {}", getClientIp(requestContext), e.getMessage());
                requestContext.abortWith(JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "Invalid credentials"));
            }
        }
    }

    private boolean isImplicitUserRole(ContainerRequestContext requestContext) {
        if (implicitUserRole) {
            return true;
        }
        try {
            byte[] clientAddress = InetAddress.getByName(getClientIp(requestContext)).getAddress();
            return trustedNetworks.stream().anyMatch(networkCIDR -> networkCIDR.isInRange(clientAddress));
        } catch (IOException e) {
            logger.debug("Error validating trusted networks: {}", e.getMessage());
            return false;
        }
    }

    private List<CIDR> parseTrustedNetworks(String value) {
        var cidrList = new ArrayList<CIDR>();
        for (var cidrString : value.split(",")) {
            try {
                if (!cidrString.isBlank()) {
                    cidrList.add(new CIDR(cidrString.trim()));
                }
            } catch (UnknownHostException e) {
                logger.warn("Error parsing trusted network cidr: {}", cidrString);
            }
        }
        return cidrList;
    }

    private String getClientIp(ContainerRequestContext requestContext) throws UnknownHostException {
        String ipForwarded = Objects.requireNonNullElse(requestContext.getHeaderString("x-forwarded-for"), "");
        String clientIp = ipForwarded.split(",")[0];
        return clientIp.isBlank() ? servletRequest.getRemoteAddr() : clientIp;
    }

    private static class CIDR {
        private static final Pattern CIDR_PATTERN = Pattern.compile("(?<networkAddress>.*?)/(?<prefixLength>\\d+)");
        private final byte[] networkBytes;
        private final int prefix;

        public CIDR(String cidr) throws UnknownHostException {
            Matcher m = CIDR_PATTERN.matcher(cidr);
            if (!m.matches()) {
                throw new UnknownHostException();
            }
            this.prefix = Integer.parseInt(m.group("prefixLength"));
            this.networkBytes = InetAddress.getByName(m.group("networkAddress")).getAddress();
        }

        public boolean isInRange(byte[] address) {
            if (networkBytes.length != address.length) {
                return false;
            }
            int p = this.prefix;
            int i = 0;
            while (p > 8) {
                if (networkBytes[i] != address[i]) {
                    return false;
                }
                ++i;
                p -= 8;
            }
            final int m = (65280 >> p) & 255;
            return (networkBytes[i] & m) == (address[i] & m);
        }
    }
}
