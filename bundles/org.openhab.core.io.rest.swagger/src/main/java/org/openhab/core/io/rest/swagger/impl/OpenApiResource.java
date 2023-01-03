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
package org.openhab.core.io.rest.swagger.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * An endpoint to generate and provide an OpenAPI description.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - made it a RESTResource to register in the root bean
 * @author Yannick Schaus - add support for ReaderListeners, remove dependency
 * @author Wouter Born - Migrated to OpenAPI
 */
@Component(service = OpenApiResource.class)
@JaxrsResource
@JaxrsName("spec")
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path("/spec")
@NonNullByDefault
public class OpenApiResource implements RESTResource {

    public static final String API_TITLE = "openHAB REST API";
    public static final String CONTACT_NAME = "openHAB";
    public static final String CONTACT_URL = "https://www.openhab.org/docs/";

    public static final String OAUTH_AUTHORIZE_ENDPOINT = "/auth/authorize";
    public static final String OAUTH_TOKEN_ENDPOINT = "/rest/auth/token";

    private final Logger logger = LoggerFactory.getLogger(OpenApiResource.class);
    private final BundleContext bundleContext;

    /**
     * Creates a new instance.
     */
    @Activate
    public OpenApiResource(final BundleContext bc, final @Reference Application application) {
        this.bundleContext = bc;
    }

    /**
     * Gets the current JAX-RS Whiteboard provided endpoint information by OpenAPI.
     *
     * @return an OpenAPI description of the endpoints
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object getOpenAPI() {
        try {
            Reader reader = new Reader();

            OpenAPI openAPI = reader.read(getReaderClasses());
            openAPI.setInfo(createInfo());
            openAPI.setServers(List.of(createServer()));
            openAPI.schemaRequirement("oauth2", createOAuth2SecurityScheme());

            String json = Json.mapper().writeValueAsString(openAPI);
            return Response.status(Response.Status.OK)
                    .entity(Json.mapper().readValue(json, new TypeReference<Map<String, Object>>() {
                    })).build();
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing the OpenAPI object to JSON");
            return Response.serverError().build();
        } catch (InvalidSyntaxException e) {
            logger.error("Error while enumerating services for OpenAPI generation");
            return Response.serverError().build();
        }
    }

    private Set<Class<?>> getReaderClasses() throws InvalidSyntaxException {
        return bundleContext.getServiceReferences(RESTResource.class, null).stream()
                .map(reference -> bundleContext.getService(reference).getClass()).collect(Collectors.toSet());
    }

    private Server createServer() {
        ServiceReference<Application> applicationReference = bundleContext.getServiceReference(Application.class);

        Server server = new Server();
        server.setUrl("/" + applicationReference.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE));

        return server;
    }

    private Info createInfo() {
        Contact contact = new Contact();
        contact.setName(CONTACT_NAME);
        contact.setUrl(CONTACT_URL);

        Info info = new Info();
        info.setContact(contact);
        info.setTitle(API_TITLE);
        info.setVersion(RESTConstants.API_VERSION);

        return info;
    }

    private SecurityScheme createOAuth2SecurityScheme() {
        Scopes scopes = new Scopes();
        scopes.addString("admin", "Administration operations");

        OAuthFlow authorizationCode = new OAuthFlow();
        authorizationCode.setAuthorizationUrl(OAUTH_AUTHORIZE_ENDPOINT);
        authorizationCode.setTokenUrl(OAUTH_TOKEN_ENDPOINT);
        authorizationCode.setScopes(scopes);

        OAuthFlows flows = new OAuthFlows();
        flows.setAuthorizationCode(authorizationCode);

        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.OAUTH2);
        securityScheme.setFlows(flows);

        return securityScheme;
    }
}
