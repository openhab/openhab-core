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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

/**
 * An endpoint to generate and provide a Swagger description.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - made it a RESTResource to register in the root bean
 * @author Yannick Schaus - add support for ReaderListeners, remove dependency
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
    private final BundleContext bundleContext;

    /**
     * Creates a new instance.
     */
    @Activate
    public SwaggerResource(final BundleContext bc, final @Reference Application application) {
        this.bundleContext = bc;
    }

    /**
     * Gets the current JAX-RS Whiteboard provided endpoint information by Swagger.
     *
     * @return a Swagger description of the endpoints
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object getSwagger() {
        Swagger swagger = new Swagger();
        Set<Class<?>> classes = new HashSet<Class<?>>();
        Reader swaggerReader = new Reader(swagger);

        try {
            ServiceReference<Application> applicationReference = bundleContext.getServiceReference(Application.class);
            swagger.setBasePath(
                    "/" + applicationReference.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE));

            Collection<ServiceReference<RESTResource>> resourcesReferences = bundleContext
                    .getServiceReferences(RESTResource.class, null);
            resourcesReferences.forEach(sr -> {
                Object service = bundleContext.getService(sr);
                classes.add(service.getClass());
            });

            Collection<ServiceReference<ReaderListener>> readerListenersReferences = bundleContext
                    .getServiceReferences(ReaderListener.class, null);
            readerListenersReferences.forEach(sr -> {
                Object service = bundleContext.getService(sr);
                classes.add(service.getClass());
            });

            swaggerReader.read(classes);
            String json = Json.mapper().writeValueAsString(swagger);
            return Response.status(Response.Status.OK)
                    .entity(Json.mapper().readValue(json, new TypeReference<Map<String, Object>>() {
                    })).build();
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing the Swagger object to JSON");
            return Response.serverError().build();
        } catch (IOException e) {
            logger.error("Error while deserializing the Swagger JSON output");
            return Response.serverError().build();
        } catch (InvalidSyntaxException e) {
            logger.error("Error while enumerating services for Swagger generation");
            return Response.serverError().build();
        }
    }
}
