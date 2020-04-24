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
package org.openhab.core.io.rest.internal.filter;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;

/**
 * A filter that only affects resources implementing the {@link SatisfiableRESTResource} interface.
 * If the current request is going to be fulfilled by a Resource implementing this interface and the
 * {@link SatisfiableRESTResource#isSatisfied()} returns false then the request will be aborted with HTTP Status Code
 * 503 - Service Unavailable.
 *
 * @author Ivan Iliev - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
public class SatisfiableResourceFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        UriInfo uriInfo = ctx.getUriInfo();

        if (uriInfo != null) {
            List<Object> matchedResources = uriInfo.getMatchedResources();

            if (matchedResources != null && !matchedResources.isEmpty()) {
                // current resource is always first as per documentation
                Object matchedResource = matchedResources.get(0);

                if (matchedResource instanceof RESTResource && !((RESTResource) matchedResource).isSatisfied()) {
                    ctx.abortWith(Response.status(Status.SERVICE_UNAVAILABLE).build());
                }
            }
        }
    }
}
