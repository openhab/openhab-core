package org.openhab.core.io.rest.auth.internal;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
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
        String authHeader = requestContext.getHeaderString("Authorization");

        if (authHeader != null) {
            String[] authParts = authHeader.split(" ");
            if (authParts.length == 2) {
                if ("Bearer".equals(authParts[0])) {
                    Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(authParts[1]);
                    requestContext.setSecurityContext(new JwtSecurityContext(auth));
                }
            }
        }
    }
}
