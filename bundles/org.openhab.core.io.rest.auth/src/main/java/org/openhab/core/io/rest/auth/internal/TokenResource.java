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
package org.openhab.core.io.rest.auth.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jose4j.base64url.Base64Url;
import org.openhab.core.auth.AuthenticatedUser;
import org.openhab.core.auth.PendingToken;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiToken;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UserSession;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.auth.internal.TokenEndpointException.ErrorType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class is used to issue JWT tokens to clients.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Wouter Born - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 * @author Yannick Schaus - Add API token operations
 */
@Component(service = { RESTResource.class, TokenResource.class })
@JaxrsResource
@JaxrsName(TokenResource.PATH_AUTH)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TokenResource.PATH_AUTH)
@Tag(name = TokenResource.PATH_AUTH)
@NonNullByDefault
public class TokenResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(TokenResource.class);

    /** The URI path to this resource */
    public static final String PATH_AUTH = "auth";

    /** The name of the HTTP-only cookie holding the session ID */
    public static final String SESSIONID_COOKIE_NAME = "X-OPENHAB-SESSIONID";
    private static final String SESSIONID_COOKIE_FORMAT = SESSIONID_COOKIE_NAME
            + "=%s; Domain=%s; Path=/; Max-Age=2147483647; HttpOnly; SameSite=Strict";

    /** The default lifetime of tokens in minutes before they expire */
    public static final int TOKEN_LIFETIME = 60;

    private final UserRegistry userRegistry;
    private final JwtHelper jwtHelper;

    @Activate
    public TokenResource(final @Reference UserRegistry userRegistry, final @Reference JwtHelper jwtHelper) {
        this.userRegistry = userRegistry;
        this.jwtHelper = jwtHelper;
    }

    @POST
    @Path("/token")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Operation(operationId = "getOAuthToken", summary = "Get access and refresh tokens.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters") })
    public Response getToken(@FormParam("grant_type") String grantType, @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri, @FormParam("client_id") String clientId,
            @FormParam("refresh_token") String refreshToken, @FormParam("code_verifier") String codeVerifier,
            @QueryParam("useCookie") boolean useCookie,
            @Nullable @CookieParam(SESSIONID_COOKIE_NAME) Cookie sessionCookie) {
        try {
            switch (grantType) {
                case "authorization_code":
                    return processAuthorizationCodeGrant(code, redirectUri, clientId, codeVerifier, useCookie);

                case "refresh_token":
                    return processRefreshTokenGrant(clientId, refreshToken, sessionCookie);

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

    @GET
    @Path("/sessions")
    @Operation(operationId = "getSessionsForCurrentUser", summary = "List the sessions associated to the authenticated user.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserSessionDTO.class)))),
            @ApiResponse(responseCode = "400", description = "User authentication is not managed by openHAB"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found") })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getSessions(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "User is not authenticated");
        }

        User user = userRegistry.get(securityContext.getUserPrincipal().getName());
        if (user == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "User not found");
        }
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "User authentication is not managed by openHAB");
        }

        Stream<UserSessionDTO> sessions = authenticatedUser.getSessions().stream().map(this::toUserSessionDTO);
        return Response.ok(new Stream2JSONInputStream(sessions)).build();
    }

    @GET
    @Path("/apitokens")
    @Operation(operationId = "getApiTokens", summary = "List the API tokens associated to the authenticated user.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserApiTokenDTO.class)))),
            @ApiResponse(responseCode = "400", description = "User authentication is not managed by openHAB"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found") })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getApiTokens(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "User is not authenticated");
        }

        User user = userRegistry.get(securityContext.getUserPrincipal().getName());
        if (user == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "User not found");
        }
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "User authentication is not managed by openHAB");
        }

        Stream<UserApiTokenDTO> sessions = authenticatedUser.getApiTokens().stream().map(this::toUserApiTokenDTO);
        return Response.ok(new Stream2JSONInputStream(sessions)).build();
    }

    @DELETE
    @Path("/apitokens/{name}")
    @Operation(operationId = "removeApiToken", summary = "Revoke a specified API token associated to the authenticated user.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "User authentication is not managed by openHAB"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "User or API token not found") })
    public Response removeApiToken(@Context SecurityContext securityContext, @PathParam("name") String name) {
        if (securityContext.getUserPrincipal() == null) {
            return JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "User is not authenticated");
        }

        User user = userRegistry.get(securityContext.getUserPrincipal().getName());
        if (user == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "User not found");
        }
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "User authent by openHAB");
        }

        Optional<UserApiToken> userApiToken = authenticatedUser.getApiTokens().stream()
                .filter(apiToken -> apiToken.getName().equals(name)).findAny();
        if (userApiToken.isEmpty()) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "No API token found with that name");
        }

        userRegistry.removeUserApiToken(user, userApiToken.get());
        return Response.ok().build();
    }

    @POST
    @Path("/logout")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Operation(operationId = "deleteSession", summary = "Delete the session associated with a refresh token.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "User authentication is not managed by openHAB"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "404", description = "User or refresh token not found") })
    public Response deleteSession(@Nullable @FormParam("refresh_token") String refreshToken,
            @Nullable @FormParam("id") String id, @Nullable @CookieParam(SESSIONID_COOKIE_NAME) Cookie sessionCookie,
            @Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "User is not authenticated");
        }

        User user = userRegistry.get(securityContext.getUserPrincipal().getName());
        if (user == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "User not found");
        }
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST,
                    "User authentication is not managed by openHAB");
        }

        Optional<UserSession> session;
        if (refreshToken != null) {
            session = authenticatedUser.getSessions().stream().filter(s -> s.getRefreshToken().equals(refreshToken))
                    .findAny();
        } else if (id != null) {
            session = authenticatedUser.getSessions().stream().filter(s -> s.getSessionId().startsWith(id + "-"))
                    .findAny();
        } else {
            throw new IllegalArgumentException("no refresh_token or id specified");
        }
        if (session.isEmpty()) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Session not found");
        }

        ResponseBuilder response = Response.ok();

        if (sessionCookie != null && sessionCookie.getValue().equals(session.get().getSessionId())) {
            try {
                URI domainUri = new URI(session.get().getRedirectUri());
                // workaround to set the SameSite cookie attribute until we upgrade to
                // jakarta.ws.rs/jakarta.ws.rs-api/3.1.0 or newer
                response.header("Set-Cookie",
                        SESSIONID_COOKIE_FORMAT.formatted(UUID.randomUUID(), domainUri.getHost()));
            } catch (URISyntaxException e) {
                logger.error("Unexpected error deleting session", e);
            }
        }

        userRegistry.removeUserSession(user, session.get());

        return response.build();
    }

    private UserSessionDTO toUserSessionDTO(UserSession session) {
        // we only divulge the prefix of the session ID to the client (otherwise an XSS attacker may find the
        // session ID for a stolen refresh token easily by using the sessions endpoint).
        return new UserSessionDTO(session.getSessionId().split("-")[0], session.getCreatedTime(),
                session.getLastRefreshTime(), session.getClientId(), session.getScope());
    }

    private UserApiTokenDTO toUserApiTokenDTO(UserApiToken apiToken) {
        return new UserApiTokenDTO(apiToken.getName(), apiToken.getCreatedTime(), apiToken.getScope());
    }

    private Response processAuthorizationCodeGrant(String code, String redirectUri, String clientId,
            @Nullable String codeVerifier, boolean useCookie) throws TokenEndpointException, NoSuchAlgorithmException {
        // find a user with the authorization code pending
        Optional<User> optionalUser = userRegistry.getAll().stream().filter(u -> {
            if (!(u instanceof AuthenticatedUser authenticatedUser)) {
                return false;
            }
            @Nullable
            PendingToken pendingToken = authenticatedUser.getPendingToken();
            return (pendingToken != null && pendingToken.getAuthorizationCode().equals(code));
        }).findAny();

        if (optionalUser.isEmpty()) {
            logger.warn("Couldn't find a user with the provided authentication code pending");
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }

        User user = optionalUser.get();
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            logger.warn("User '{}' authentication is not managed by openHAB", user.getName());
            throw new TokenEndpointException(ErrorType.INVALID_CLIENT);
        }
        PendingToken pendingToken = authenticatedUser.getPendingToken();
        if (pendingToken == null) {
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }
        if (!pendingToken.getClientId().equals(clientId)) {
            logger.warn("client_id '{}' doesn't match pending token information '{}'", clientId,
                    pendingToken.getClientId());
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }
        if (!pendingToken.getRedirectUri().equals(redirectUri)) {
            logger.warn("redirect_uri '{}' doesn't match pending token information '{}'", redirectUri,
                    pendingToken.getRedirectUri());
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }

        // create a new session ID and refresh token
        String sessionId = UUID.randomUUID().toString();
        String newRefreshToken = UUID.randomUUID().toString().replace("-", "");
        String scope = pendingToken.getScope();

        // if there is PKCE information in the pending token, check that first
        String codeChallengeMethod = pendingToken.getCodeChallengeMethod();
        if (codeChallengeMethod != null) {
            String codeChallenge = pendingToken.getCodeChallenge();
            if (codeChallenge == null || codeVerifier == null) {
                logger.warn("the PKCE code challenge or code verifier information is missing");
                throw new TokenEndpointException(ErrorType.INVALID_GRANT);
            }
            switch (codeChallengeMethod) {
                case "plain":
                    if (!codeVerifier.equals(codeChallenge)) {
                        logger.warn("PKCE verification failed");
                        throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                    }
                    break;
                case "S256":
                    MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
                    String computedCodeChallenge = Base64Url.encode(sha256Digest.digest(codeVerifier.getBytes()));
                    if (!computedCodeChallenge.equals(codeChallenge)) {
                        logger.warn("PKCE verification failed");
                        throw new TokenEndpointException(ErrorType.INVALID_GRANT);
                    }
                    break;
                default:
                    logger.warn("PKCE transformation algorithm '{}' not supported", codeChallengeMethod);
                    throw new TokenEndpointException(ErrorType.INVALID_REQUEST);
            }
        }

        // create an access token
        String accessToken = jwtHelper.getJwtAccessToken(authenticatedUser, clientId, scope, TOKEN_LIFETIME);

        UserSession newSession = new UserSession(sessionId, newRefreshToken, clientId, redirectUri, scope);

        ResponseBuilder response = Response.ok(new TokenResponseDTO(accessToken, "bearer", TOKEN_LIFETIME * 60,
                newRefreshToken, scope, authenticatedUser));

        // if the client has requested an http-only cookie for the session, set it
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
                // workaround to set the SameSite cookie attribute until we upgrade to
                // jakarta.ws.rs/jakarta.ws.rs-api/3.1.0 or newer
                response.header("Set-Cookie", SESSIONID_COOKIE_FORMAT.formatted(sessionId, domainUri.getHost()));

                // also mark the session as supported by a cookie
                newSession.setSessionCookie(true);
            } catch (Exception e) {
                logger.warn("Error while setting a session cookie: {}", e.getMessage());
                throw new TokenEndpointException(ErrorType.UNAUTHORIZED_CLIENT);
            }
        }

        // add the new session to the user profile and clear the pending token information
        authenticatedUser.getSessions().add(newSession);
        authenticatedUser.setPendingToken(null);
        userRegistry.update(authenticatedUser);

        return response.build();
    }

    private Response processRefreshTokenGrant(String clientId, @Nullable String refreshToken,
            @Nullable Cookie sessionCookie) throws TokenEndpointException {
        if (refreshToken == null) {
            throw new TokenEndpointException(ErrorType.INVALID_REQUEST);
        }

        // find a user associated with the provided refresh token
        Optional<User> optionalRefreshTokenUser = userRegistry.getAll().stream()
                .filter(u -> u instanceof AuthenticatedUser).filter(u -> ((AuthenticatedUser) u).getSessions().stream()
                        .anyMatch(s -> refreshToken.equals(s.getRefreshToken())))
                .findAny();

        if (optionalRefreshTokenUser.isEmpty()) {
            logger.warn("Couldn't find a user with a session matching the provided refresh_token");
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }

        // get the session from the refresh token
        User refreshTokenUser = optionalRefreshTokenUser.get();
        if (!(refreshTokenUser instanceof AuthenticatedUser refreshTokenAuthenticatedUser)) {
            logger.warn("User '{}' authentication is not managed by openHAB", refreshTokenUser.getName());
            throw new TokenEndpointException(ErrorType.INVALID_CLIENT);
        }
        Optional<UserSession> optSession = refreshTokenAuthenticatedUser.getSessions().stream()
                .filter(s -> s.getRefreshToken().equals(refreshToken)).findAny();
        if (optSession.isEmpty()) {
            logger.warn("Not refreshing token for user {}, missing session", refreshTokenAuthenticatedUser.getName());
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }
        UserSession session = optSession.get();

        // if the cookie flag is present on the session, verify that the cookie is present and corresponds
        // to this session
        if (session.hasSessionCookie()
                && (sessionCookie == null || !sessionCookie.getValue().equals(session.getSessionId()))) {
            logger.warn("Not refreshing token for session {} of user {}, missing or invalid session cookie",
                    session.getSessionId(), refreshTokenAuthenticatedUser.getName());
            throw new TokenEndpointException(ErrorType.INVALID_GRANT);
        }

        // issue a new access token
        String refreshedAccessToken = jwtHelper.getJwtAccessToken(refreshTokenAuthenticatedUser, clientId,
                session.getScope(), TOKEN_LIFETIME);

        logger.debug("Refreshing session {} of user {}", session.getSessionId(),
                refreshTokenAuthenticatedUser.getName());

        ResponseBuilder refreshResponse = Response.ok(new TokenResponseDTO(refreshedAccessToken, "bearer",
                TOKEN_LIFETIME * 60, refreshToken, session.getScope(), refreshTokenAuthenticatedUser));

        // update the last refresh time of the session in the user's profile
        session.setLastRefreshTime(new Date());
        userRegistry.update(refreshTokenAuthenticatedUser);

        return refreshResponse.build();
    }
}
