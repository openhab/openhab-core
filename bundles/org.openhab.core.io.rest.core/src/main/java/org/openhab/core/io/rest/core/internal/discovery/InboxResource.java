/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.discovery;

import static org.openhab.core.config.discovery.inbox.InboxPredicates.forThingUID;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTO;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTOMapper;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.syntaxgenerator.ThingSyntaxGenerator;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for the inbox and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector and
 *         removed ThingSetupManager
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Chris Jackson - Updated to use JSONResponse. Fixed null response from
 *         approve. Improved error reporting.
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 * @author Laurent Garnier - Added optional parameter newThingId to approve API
 * @author Laurent Garnier - Added API to generate syntax
 */
@Component(service = { RESTResource.class, InboxResource.class })
@JaxrsResource
@JaxrsName(InboxResource.PATH_INBOX)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(InboxResource.PATH_INBOX)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = InboxResource.PATH_INBOX)
@NonNullByDefault
public class InboxResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(InboxResource.class);

    /** The URI path to this resource */
    public static final String PATH_INBOX = "inbox";

    private final Inbox inbox;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final Map<String, ThingSyntaxGenerator> thingSyntaxGenerators = new ConcurrentHashMap<>();

    @Activate
    public InboxResource(final @Reference Inbox inbox, final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        this.inbox = inbox;
        this.thingTypeRegistry = thingTypeRegistry;
        this.configDescRegistry = configDescRegistry;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingSyntaxGenerator(ThingSyntaxGenerator thingSyntaxGenerator) {
        thingSyntaxGenerators.put(thingSyntaxGenerator.getFormat(), thingSyntaxGenerator);
    }

    protected void removeThingSyntaxGenerator(ThingSyntaxGenerator thingSyntaxGenerator) {
        thingSyntaxGenerators.remove(thingSyntaxGenerator.getFormat());
    }

    @POST
    @Path("/{thingUID}/approve")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "approveInboxItemById", summary = "Approves the discovery result by adding the thing to the registry.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid new thing ID."),
            @ApiResponse(responseCode = "404", description = "Thing unable to be approved."),
            @ApiResponse(responseCode = "409", description = "No binding found that supports this thing.") })
    public Response approve(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @Parameter(description = "thing label") @Nullable String label,
            @QueryParam("newThingId") @Parameter(description = "new thing ID") @Nullable String newThingId) {
        ThingUID thingUIDObject = new ThingUID(thingUID);
        String notEmptyLabel = label != null && !label.isEmpty() ? label : null;
        String notEmptyNewThingId = newThingId != null && !newThingId.isEmpty() ? newThingId : null;
        Thing thing;
        try {
            thing = inbox.approve(thingUIDObject, notEmptyLabel, notEmptyNewThingId);
        } catch (IllegalArgumentException e) {
            logger.error("Thing {} unable to be approved: {}", thingUID, e.getLocalizedMessage());
            String errMsg = e.getMessage();
            return errMsg != null
                    && (errMsg.contains("must not contain multiple segments") || errMsg.startsWith("Invalid thing UID"))
                            ? JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Invalid new thing ID.")
                            : JSONResponse.createErrorResponse(Status.NOT_FOUND, "Thing unable to be approved.");
        }

        // inbox.approve returns null if no handler is found that supports this thing
        if (thing == null) {
            return JSONResponse.createErrorResponse(Status.CONFLICT, "No binding found that can create the thing");
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/{thingUID}")
    @Operation(operationId = "removeItemFromInbox", summary = "Removes the discovery result from the inbox.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Discovery result not found in the inbox.") })
    public Response delete(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        if (inbox.remove(new ThingUID(thingUID))) {
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Thing not found in inbox");
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getDiscoveredInboxItems", summary = "Get all discovered things.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DiscoveryResultDTO.class)))) })
    public Response getAll(
            @QueryParam("includeIgnored") @DefaultValue("true") @Parameter(description = "If true, include ignored inbox entries. Defaults to true") boolean includeIgnored) {
        Stream<DiscoveryResult> discoveryStream = inbox.getAll().stream();
        if (!includeIgnored) {
            discoveryStream = discoveryStream
                    .filter(discoveryResult -> discoveryResult.getFlag() != DiscoveryResultFlag.IGNORED);
        }
        return Response.ok(new Stream2JSONInputStream(discoveryStream.map(DiscoveryResultDTOMapper::map))).build();
    }

    @POST
    @Path("/{thingUID}/ignore")
    @Operation(operationId = "flagInboxItemAsIgnored", summary = "Flags a discovery result as ignored for further processing.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response ignore(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.IGNORED);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/{thingUID}/unignore")
    @Operation(operationId = "removeIgnoreFlagOnInboxItem", summary = "Removes ignore flag from a discovery result.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response unignore(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.NEW);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/{thingUID}/syntax/generate")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "generateSyntaxForDiscoveryResult", summary = "Generate syntax for the thing associated to the discovery result.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Unsupported syntax format."),
            @ApiResponse(responseCode = "404", description = "Discovery result not found in the inbox or thing type not found.") })
    public Response generateSyntaxForDiscoveryResult(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @DefaultValue("DSL") @QueryParam("format") @Parameter(description = "syntax format") String format,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        ThingSyntaxGenerator generator = thingSyntaxGenerators.get(format);
        if (generator == null) {
            String message = "No syntax available for format " + format + "!";
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }

        List<DiscoveryResult> results = inbox.getAll().stream().filter(forThingUID(new ThingUID(thingUID))).toList();
        if (results.isEmpty()) {
            String message = "Discovery result for thing with UID " + thingUID + " not found in the inbox!";
            return Response.status(Response.Status.NOT_FOUND).entity(message).build();
        }
        DiscoveryResult result = results.get(0);
        ThingType thingType = thingTypeRegistry.getThingType(result.getThingTypeUID());
        if (thingType == null) {
            String message = "Thing type with UID " + result.getThingTypeUID() + " does not exist!";
            return Response.status(Response.Status.NOT_FOUND).entity(message).build();
        }

        return Response
                .ok(generator.generateSyntax(List.of(simulateThing(result, thingType)), hideDefaultParameters, false))
                .build();
    }

    /*
     * Create a thing from a discovery result without inserting it in the thing registry
     */
    private Thing simulateThing(DiscoveryResult result, ThingType thingType) {
        Map<String, Object> configParams = new HashMap<>();
        List<ConfigDescriptionParameter> configDescriptionParameters = List.of();
        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription desc = configDescRegistry.getConfigDescription(descURI);
            if (desc != null) {
                configDescriptionParameters = desc.getParameters();
            }
        }
        for (ConfigDescriptionParameter param : configDescriptionParameters) {
            Object value = result.getProperties().get(param.getName());
            if (value != null) {
                configParams.put(param.getName(), ConfigUtil.normalizeType(value, param));
            }
        }
        Configuration config = new Configuration(configParams);
        return ThingFactory.createThing(thingType, result.getThingUID(), config, result.getBridgeUID(),
                configDescRegistry);
    }
}
