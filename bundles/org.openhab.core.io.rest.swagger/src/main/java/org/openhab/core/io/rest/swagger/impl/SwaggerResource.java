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
package org.openhab.core.io.rest.swagger.impl;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
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

import de.maggu2810.jaxrswb.swagger1.gen.JaxRsWhiteboardSwaggerGenerator;

/**
 * An endpoint to generate and provide a Swagger description.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - made it a RESTResource to register in the root bean
 */
@Component(service = SwaggerResource.class)
@JaxrsResource
@JaxrsName("spec")
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path("/spec")
@NonNullByDefault
public class SwaggerResource implements RESTResource {

    private final Logger logger = LoggerFactory.getLogger(SwaggerResource.class);
    private final JaxRsWhiteboardSwaggerGenerator generator;

    /**
     * Creates a new instance.
     *
     * @param generator the generator
     */
    @Activate
    public SwaggerResource(final @Reference JaxRsWhiteboardSwaggerGenerator generator) {
        this.generator = generator;
    }

    /**
     * Gets the current JAX-RS Whiteboard provided endpoint information by Swagger 1.
     *
     * @return a Swagger 1 description of the endpoints
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getSwagger() {
        final Map<String, Object> map;
        try {
            map = generator.generateMap();
        } catch (final IOException ex) {
            logger.warn("Error on Swagger DTO generation.", ex);
            return Response.serverError().build();
        }
        return Response.status(Response.Status.OK).entity(map).build();
    }
}
