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

import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UserSession;
import org.openhab.core.io.rest.RESTResource;
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
    @ApiOperation(value = "Login.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response login(@FormParam("grant_type") String grantType, @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri, @FormParam("client_id") String clientId,
            @FormParam("refresh_token") String refreshToken) {
        try {
            switch (grantType) {
                case "authorization_code":
                    // find an user with the authorization code pending
                    Optional<User> user = userRegistry.getAll().stream()
                            .filter(u -> ((ManagedUser) u).getPendingToken() != null
                                    && ((ManagedUser) u).getPendingToken().getAuthorizationCode().equals(code))
                            .findAny();

                    if (user.isPresent()) {
                        ManagedUser managedUser = (ManagedUser) user.get();
                        if (!managedUser.getPendingToken().getClientId().equals(clientId)) {
                            throw new AuthenticationException("invalid_client");
                        }
                        String access_token = jwtHelper.getJwtAccessToken(managedUser, clientId);

                        // create a new session in the user's profile and clear the pending token information
                        String refresh_token = UUID.randomUUID().toString();
                        String scope = managedUser.getPendingToken().getScope();
                        managedUser.getSessions().add(new UserSession(refreshToken, scope));
                        managedUser.setPendingToken(null);
                        userRegistry.update(managedUser);

                        return Response.ok(new TokenResponseDTO(access_token, "bearer", "36000", refresh_token, scope,
                                managedUser)).build();
                    }

                    throw new AuthenticationException("invalid_grant");

                case "refresh_token":
                    // find an user associated with the provided refresh token
                    Optional<User> refreshTokenUser = userRegistry.getAll().stream().filter(u -> ((ManagedUser) u)
                            .getSessions().stream().anyMatch(s -> s.getRefreshToken().equals(refreshToken))).findAny();

                    if (refreshTokenUser.isPresent()) {
                        ManagedUser refreshTokenManagedUser = (ManagedUser) refreshTokenUser.get();
                        String jwt = jwtHelper.getJwtAccessToken(refreshTokenManagedUser, clientId);
                        return Response.ok(jwt).build();
                    }

                    throw new AuthenticationException("invalid_grant");

                default:
                    throw new AuthenticationException("unsupported_grant_type");
            }
        } catch (AuthenticationException e) {
            logger.warn("Token issuing failed: {}", e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(new TokenResponseDTO(e.getMessage())).build();
        } catch (Exception e) {
            logger.error("Error while authenticating", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
}
