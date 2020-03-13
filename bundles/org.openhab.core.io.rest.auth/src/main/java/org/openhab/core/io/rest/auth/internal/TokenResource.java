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
package org.openhab.core.io.rest.auth.internal;

import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UserSession;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.auth.internal.TokenEndpointException.ErrorType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class is used to issue JWT tokens to clients.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Path(TokenResource.PATH_AUTH)
@Api(value = TokenResource.PATH_AUTH)
@Component(service = { RESTResource.class, TokenResource.class })
public class TokenResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(TokenResource.class);

    /** The URI path to this resource */
    public static final String PATH_AUTH = "auth";

    /** The name of the HTTP-only cookie holding the session ID */
    public static final String SESSIONID_COOKIE_NAME = "X-OPENHAB-SESSIONID";

    @Context
    private UriInfo uriInfo;

    @Reference
    private AuthenticationProvider authenticationProvider;

    private UserRegistry userRegistry;

    private JwtHelper jwtHelper;

    @Activate
    public TokenResource(final @Reference UserRegistry userRegistry, final @Reference JwtHelper jwtHelper) {
        this.userRegistry = userRegistry;
        this.jwtHelper = jwtHelper;
    }

    @POST
    @Path("/token")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @ApiOperation(value = "Get access and refresh tokens.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getToken(@FormParam("grant_type") String grantType, @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri, @FormParam("client_id") String clientId,
            @FormParam("refresh_token") String refreshToken, @QueryParam("useCookie") boolean useCookie,
            @CookieParam(SESSIONID_COOKIE_NAME) Cookie sessionCookie) {
        try {
            switch (grantType) {
                case "authorization_code":
                    // find an user with the authorization code pending
                    Optional<User> user = userRegistry.getAll().stream()
                            .filter(u -> ((ManagedUser) u).getPendingToken() != null
                                    && ((ManagedUser) u).getPendingToken().getAuthorizationCode().equals(code))
                            .findAny();

                    if (!user.isPresent()) {
                        logger.warn("Couldn't find an user with the provided authentication code pending");
                        throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                    }

                    ManagedUser managedUser = (ManagedUser) user.get();
                    if (!managedUser.getPendingToken().getClientId().equals(clientId)) {
                        logger.warn("client_id doesn't match pending token information");
                        throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                    }
                    if (!managedUser.getPendingToken().getRedirectUri().equals(redirectUri)) {
                        logger.warn("client_id doesn't match pending token information");
                        throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                    }

                    // create a new session ID and refresh token
                    String sessionId = UUID.randomUUID().toString();
                    String newRefreshToken = UUID.randomUUID().toString().replace("-", "");
                    String scope = managedUser.getPendingToken().getScope();

                    // create an access token
                    String accessToken = jwtHelper.getJwtAccessToken(managedUser, clientId, scope);

                    UserSession newSession = new UserSession(sessionId, newRefreshToken, clientId, redirectUri, scope);

                    ResponseBuilder response = Response
                            .ok(new TokenResponseDTO(accessToken, "bearer", 3600, newRefreshToken, scope, managedUser));

                    // if the client has requested a http-only cookie for the session, set it
                    if (useCookie) {
                        try {
                            // this feature is only available for root redirect URIs: the targeted client is the main
                            // UI; even though the cookie will be set for the entire domain (i.e. no path) so that
                            // other servlets can make use of it
                            URI domainUri = new URI(redirectUri);
                            if (!("".equals(domainUri.getPath()) || "/".equals(domainUri.getPath()))) {
                                throw new IllegalArgumentException(
                                        "Will not honor the request to set a session cookie for this client, because it's only allowed for root redirect URIs");
                            }
                            NewCookie newCookie = new NewCookie(SESSIONID_COOKIE_NAME, sessionId, "/",
                                    domainUri.getHost(), null, 2147483647, false, true);
                            response.cookie(newCookie);

                            // also mark the session as supported by a cookie
                            newSession.setSessionCookie(true);
                        } catch (Exception e) {
                            logger.warn("Error while setting a session cookie: {}", e.getMessage());
                            throw new TokenEndpointException(ErrorType.UNAUTHORIZED_CLIENT);
                        }
                    }

                    // add the new session to the user profile and clear the pending token information
                    managedUser.getSessions().add(newSession);
                    managedUser.setPendingToken(null);
                    userRegistry.update(managedUser);

                    return response.build();

                case "refresh_token":
                    if (refreshToken == null) {
                        throw new TokenEndpointException(ErrorType.INVALID_REQUEST);
                    }

                    // find an user associated with the provided refresh token
                    Optional<User> refreshTokenUser = userRegistry.getAll().stream().filter(u -> ((ManagedUser) u)
                            .getSessions().stream().anyMatch(s -> refreshToken.equals(s.getRefreshToken()))).findAny();

                    if (!refreshTokenUser.isPresent()) {
                        logger.warn("Couldn't find an user with a session matching the provided refresh_token");
                        throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                    }

                    // get the session from the refresh token
                    ManagedUser refreshTokenManagedUser = (ManagedUser) refreshTokenUser.get();
                    UserSession session = refreshTokenManagedUser.getSessions().stream()
                            .filter(s -> s.getRefreshToken().equals(refreshToken)).findAny().get();

                    // if the cookie flag is present on the session, verify that the cookie is present and corresponds
                    // to this session
                    if (session.hasSessionCookie()) {
                        if (sessionCookie == null || !sessionCookie.getValue().equals(session.getSessionId())) {
                            logger.warn(
                                    "Not refreshing token for session {} of user {}, missing or invalid session cookie",
                                    session.getSessionId(), refreshTokenManagedUser.getName());
                            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                        }
                    }

                    // issue a new access token
                    String refreshedAccessToken = jwtHelper.getJwtAccessToken(refreshTokenManagedUser, clientId,
                            session.getScope());

                    logger.debug("Refreshing session {} of user {}", session.getSessionId(),
                            refreshTokenManagedUser.getName());

                    ResponseBuilder refreshResponse = Response.ok(new TokenResponseDTO(refreshedAccessToken, "bearer",
                            3600, refreshToken, session.getScope(), refreshTokenManagedUser));

                    // update the last refresh time of the session in the user's profile
                    session.setLastRefreshTime(new Date());
                    userRegistry.update(refreshTokenManagedUser);

                    return refreshResponse.build();

                default:
                    throw new TokenEndpointException(ErrorType.UNSUPPORTED_GRANT_TYPE);
            }
        } catch (TokenEndpointException e) {
            logger.warn("Token issuing failed: {}", e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(e.getErrorDTO()).build();
        } catch (Exception e) {
            logger.error("Error while authenticating", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/logout")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @ApiOperation(value = "Delete the session associated with a refresh token.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response deleteSession(@FormParam("refresh_token") String refreshToken,
            @Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("User not authenticated");
        }

        ManagedUser user = (ManagedUser) userRegistry.get(securityContext.getUserPrincipal().getName());
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Optional<UserSession> session = user.getSessions().stream()
                .filter(s -> s.getRefreshToken().equals(refreshToken)).findAny();
        if (session.isEmpty()) {
            throw new NotFoundException("Session not found");
        }

        ResponseBuilder response = Response.ok();

        if (session.get().hasSessionCookie()) {
            URI domainUri;
            try {
                domainUri = new URI(session.get().getRedirectUri());
                NewCookie newCookie = new NewCookie(SESSIONID_COOKIE_NAME, "", "/", domainUri.getHost(), null, 0, false,
                        true);
                response.cookie(newCookie);
            } catch (Exception e) {
            }
        }

        user.getSessions().remove(session.get());
        userRegistry.update(user);

        return response.build();
    }

}
