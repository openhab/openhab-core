package org.openhab.core.io.rest.auth.internal;

import java.io.IOException;

import javax.annotation.Priority;
import javax.security.sasl.AuthenticationException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.openhab.core.auth.Authentication;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Provider
@Component(immediate = true, service = AuthFilter.class)
public class AuthFilter implements ContainerRequestFilter {

    @Reference
    private JwtHelper jwtHelper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

            if (authHeader != null) {
                String[] authParts = authHeader.split(" ");
                if (authParts.length == 2) {
                    if ("Bearer".equals(authParts[0])) {
                        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(authParts[1]);
                        requestContext.setSecurityContext(new JwtSecurityContext(auth));
                        return;
                    }
                }
            }

            // support the api_key query parameter of the Swagger UI
            if (requestContext.getUriInfo().getRequestUri().toString().contains("api_key=")) {
                String apiKey = requestContext.getUriInfo().getQueryParameters(true).getFirst("api_key");
                if (apiKey != null) {
                    Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(apiKey);
                    requestContext.setSecurityContext(new JwtSecurityContext(auth));
                    return;
                }
            }
        } catch (AuthenticationException e) {
            throw new NotAuthorizedException("Invalid token");
        }
    }
}
