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
package org.openhab.core.io.rest.core.internal.thing;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.status.ConfigStatusInfo;
import org.openhab.core.config.core.status.ConfigStatusMessage;
import org.openhab.core.config.core.status.ConfigStatusService;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.io.rest.DTOMapper;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.core.thing.EnrichedThingDTO;
import org.openhab.core.io.rest.core.thing.EnrichedThingDTOMapper;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.dto.ChannelDTO;
import org.openhab.core.thing.dto.ChannelDTOMapper;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.firmware.FirmwareRegistry;
import org.openhab.core.thing.firmware.FirmwareStatusInfo;
import org.openhab.core.thing.firmware.FirmwareUpdateService;
import org.openhab.core.thing.firmware.dto.FirmwareDTO;
import org.openhab.core.thing.firmware.dto.FirmwareStatusDTO;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.thing.util.ThingHelper;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for things and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector and
 *         refactored create and update methods
 * @author Thomas Höfer - added validation of configuration and localization of thing status
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Jörg Plewe - refactoring, error handling
 * @author Chris Jackson - added channel configuration updates,
 *         return empty set for config/status if no status available,
 *         add editable flag to thing responses
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Dimitar Ivanov - replaced Firmware UID with thing UID and firmware version
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(ThingResource.PATH_THINGS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ThingResource.PATH_THINGS)
@Tag(name = ThingResource.PATH_THINGS)
@NonNullByDefault
public class ThingResource implements RESTResource {

    private final Logger logger = LoggerFactory.getLogger(ThingResource.class);

    /** The URI path to this resource */
    public static final String PATH_THINGS = "things";

    private final DTOMapper dtoMapper;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ConfigStatusService configStatusService;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final FirmwareRegistry firmwareRegistry;
    private final FirmwareUpdateService firmwareUpdateService;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final LocaleService localeService;
    private final ManagedThingProvider managedThingProvider;
    private final ThingManager thingManager;
    private final ThingRegistry thingRegistry;
    private final ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;
    private final ThingTypeRegistry thingTypeRegistry;

    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    @Activate
    public ThingResource( //
            final @Reference DTOMapper dtoMapper, final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigStatusService configStatusService,
            final @Reference ConfigDescriptionRegistry configDescRegistry,
            final @Reference FirmwareRegistry firmwareRegistry,
            final @Reference FirmwareUpdateService firmwareUpdateService,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, //
            final @Reference ItemFactory itemFactory, //
            final @Reference ItemRegistry itemRegistry, //
            final @Reference LocaleService localeService,
            final @Reference ManagedItemChannelLinkProvider managedItemChannelLinkProvider,
            final @Reference ManagedItemProvider managedItemProvider,
            final @Reference ManagedThingProvider managedThingProvider, //
            final @Reference ThingManager thingManager, //
            final @Reference ThingRegistry thingRegistry,
            final @Reference ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService,
            final @Reference ThingTypeRegistry thingTypeRegistry) {
        this.dtoMapper = dtoMapper;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configStatusService = configStatusService;
        this.configDescRegistry = configDescRegistry;
        this.firmwareRegistry = firmwareRegistry;
        this.firmwareUpdateService = firmwareUpdateService;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.localeService = localeService;
        this.managedThingProvider = managedThingProvider;
        this.thingManager = thingManager;
        this.thingRegistry = thingRegistry;
        this.thingStatusInfoI18nLocalizationService = thingStatusInfoI18nLocalizationService;
        this.thingTypeRegistry = thingTypeRegistry;
    }

    /**
     * create a new Thing
     *
     * @param thingBean
     * @return Response holding the newly created Thing or error information
     */
    @POST
    @RolesAllowed({ Role.ADMIN })
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createThingInRegistry", summary = "Creates a new thing and adds it to the registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = EnrichedThingDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Thing uid does not match bridge uid."),
                    @ApiResponse(responseCode = "400", description = "A uid must be provided, if no binding can create a thing of this type."),
                    @ApiResponse(responseCode = "409", description = "A thing with the same uid already exists.") })
    public Response create(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @Parameter(description = "thing data", required = true) ThingDTO thingBean) {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUID = thingBean.UID == null ? null : new ThingUID(thingBean.UID);
        ThingTypeUID thingTypeUID = new ThingTypeUID(thingBean.thingTypeUID);

        if (thingUID != null) {
            // check if a thing with this UID already exists
            Thing thing = thingRegistry.get(thingUID);
            if (thing != null) {
                // report a conflict
                return getThingResponse(Status.CONFLICT, thing, locale,
                        "Thing " + thingUID.toString() + " already exists!");
            }
        }

        ThingUID bridgeUID = null;

        if (thingBean.bridgeUID != null) {
            bridgeUID = new ThingUID(thingBean.bridgeUID);
            if (thingUID != null && (!thingUID.getBindingId().equals(bridgeUID.getBindingId())
                    || !thingUID.getBridgeIds().contains(bridgeUID.getId()))) {
                return Response.status(Status.BAD_REQUEST)
                        .entity("Thing UID '" + thingUID + "' does not match bridge UID '" + bridgeUID + "'").build();
            }
        }

        // turn the ThingDTO's configuration into a Configuration
        Configuration configuration = new Configuration(
                normalizeConfiguration(thingBean.configuration, thingTypeUID, thingUID));
        if (thingUID != null) {
            normalizeChannels(thingBean, thingUID);
        }

        Thing thing = thingRegistry.createThingOfType(thingTypeUID, thingUID, bridgeUID, thingBean.label,
                configuration);

        if (thing != null) {
            if (thingBean.properties != null) {
                for (Entry<String, String> entry : thingBean.properties.entrySet()) {
                    thing.setProperty(entry.getKey(), entry.getValue());
                }
            }
            if (thingBean.channels != null) {
                List<Channel> channels = new ArrayList<>();
                for (ChannelDTO channelDTO : thingBean.channels) {
                    channels.add(ChannelDTOMapper.map(channelDTO));
                }
                ThingHelper.addChannelsToThing(thing, channels);
            }
            if (thingBean.location != null) {
                thing.setLocation(thingBean.location);
            }
        } else if (thingUID != null) {
            // if there wasn't any ThingFactory capable of creating the thing,
            // we create the Thing exactly the way we received it, i.e. we
            // cannot take its thing type into account for automatically
            // populating channels and properties.
            thing = ThingDTOMapper.map(thingBean, thingTypeRegistry.getThingType(thingTypeUID) instanceof BridgeType);
        } else {
            return getThingResponse(Status.BAD_REQUEST, thing, locale,
                    "A UID must be provided, since no binding can create the thing!");
        }

        thingRegistry.add(thing);
        return getThingResponse(Status.CREATED, thing, locale, null);
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getThings", summary = "Get all available things.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnrichedThingDTO.class), uniqueItems = true))) })
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("summary") @Parameter(description = "summary fields only") @Nullable Boolean summary) {
        final Locale locale = localeService.getLocale(language);

        Stream<EnrichedThingDTO> thingStream = thingRegistry.stream().map(t -> convertToEnrichedThingDTO(t, locale))
                .distinct();
        if (summary != null && summary) {
            thingStream = dtoMapper.limitToFields(thingStream,
                    "UID,label,bridgeUID,thingTypeUID,statusInfo,firmwareStatus,location,editable");
        }
        return Response.ok(new Stream2JSONInputStream(thingStream)).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getThingById", summary = "Gets thing by UID.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedThingDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Thing not found.") })
    public Response getByUID(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        final Locale locale = localeService.getLocale(language);

        Thing thing = thingRegistry.get((new ThingUID(thingUID)));

        // return Thing data if it does exist
        if (thing != null) {
            return getThingResponse(Status.OK, thing, locale, null);
        } else {
            return getThingNotFoundResponse(thingUID);
        }
    }

    /**
     * Delete a Thing, if possible. Thing deletion might be impossible if the
     * Thing is not managed, will return CONFLICT. Thing deletion might happen
     * delayed, will return ACCEPTED.
     *
     * @param thingUID
     * @param force
     * @return Response with status/error information
     */
    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}")
    @Operation(operationId = "removeThingById", summary = "Removes a thing from the registry. Set \'force\' to __true__ if you want the thing to be removed immediately.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK, was deleted."),
                    @ApiResponse(responseCode = "202", description = "ACCEPTED for asynchronous deletion."),
                    @ApiResponse(responseCode = "404", description = "Thing not found."),
                    @ApiResponse(responseCode = "409", description = "Thing could not be deleted because it's not editable.") })
    public Response remove(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @DefaultValue("false") @QueryParam("force") @Parameter(description = "force") boolean force) {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        // check whether thing exists and throw 404 if not
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            logger.info("Received HTTP DELETE request for update at '{}' for the unknown thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        // ask whether the Thing exists as a managed thing, so it can get
        // updated, 409 otherwise
        Thing managed = managedThingProvider.get(thingUIDObject);
        if (managed == null) {
            logger.info("Received HTTP DELETE request for update at '{}' for an unmanaged thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingResponse(Status.CONFLICT, thing, locale,
                    "Cannot delete Thing " + thingUID + " as it is not editable.");
        }

        // only move on if Thing is known to be managed, so it can get updated
        if (force) {
            if (thingRegistry.forceRemove(thingUIDObject) == null) {
                return getThingResponse(Status.INTERNAL_SERVER_ERROR, thing, locale,
                        "Cannot delete Thing " + thingUID + " for unknown reasons.");
            }
        } else {
            if (thingRegistry.remove(thingUIDObject) != null) {
                return getThingResponse(Status.ACCEPTED, thing, locale, null);
            }
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    /**
     * Update Thing.
     *
     * @param thingUID
     * @param thingBean
     * @return Response with the updated Thing or error information
     * @throws IOException
     */
    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateThing", summary = "Updates a thing.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedThingDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Thing not found."),
                    @ApiResponse(responseCode = "409", description = "Thing could not be updated as it is not editable.") })
    public Response update(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @Parameter(description = "thing", required = true) ThingDTO thingBean) throws IOException {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        // ask whether the Thing exists at all, 404 otherwise
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            logger.info("Received HTTP PUT request for update at '{}' for the unknown thing '{}'.", uriInfo.getPath(),
                    thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        // ask whether the Thing exists as a managed thing, so it can get
        // updated, 409 otherwise
        Thing managed = managedThingProvider.get(thingUIDObject);
        if (managed == null) {
            logger.info("Received HTTP PUT request for update at '{}' for an unmanaged thing '{}'.", uriInfo.getPath(),
                    thingUID);
            return getThingResponse(Status.CONFLICT, thing, locale,
                    "Cannot update Thing " + thingUID + " as it is not editable.");
        }

        // check configuration
        thingBean.configuration = normalizeConfiguration(thingBean.configuration, thing.getThingTypeUID(),
                thing.getUID());
        normalizeChannels(thingBean, thing.getUID());

        thing = ThingHelper.merge(thing, thingBean);

        // update, returns null in case Thing cannot be found
        Thing oldthing = managedThingProvider.update(thing);
        if (oldthing == null) {
            return getThingNotFoundResponse(thingUID);
        }

        // everything went well
        return getThingResponse(Status.OK, thing, locale, null);
    }

    /**
     * Updates Thing configuration.
     *
     * @param thingUID
     * @param configurationParameters
     * @return Response with the updated Thing or error information
     * @throws IOException
     */
    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateThingConfig", summary = "Updates thing's configuration.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedThingDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Configuration of the thing is not valid."),
                    @ApiResponse(responseCode = "404", description = "Thing not found"),
                    @ApiResponse(responseCode = "409", description = "Thing could not be updated as it is not editable.") })
    public Response updateConfiguration(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thing") String thingUID,
            @Parameter(description = "configuration parameters") @Nullable Map<String, Object> configurationParameters)
            throws IOException {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        // ask whether the Thing exists at all, 404 otherwise
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            logger.info("Received HTTP PUT request for update configuration at '{}' for the unknown thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        // ask whether the Thing exists as a managed thing, so it can get updated, 409 otherwise
        Thing managed = managedThingProvider.get(thingUIDObject);
        if (managed == null) {
            logger.info("Received HTTP PUT request for update configuration at '{}' for an unmanaged thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingResponse(Status.CONFLICT, thing, locale,
                    "Cannot update Thing " + thingUID + " as it is not editable.");
        }
        // check if handler of Thing is available, so it can be updated, 409 otherwise
        ThingHandler thingHandler = thing.getHandler();
        if (thingHandler == null) {
            logger.info("Received HTTP PUT request for update configuration at '{}' for an uninitialized thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingResponse(Status.CONFLICT, thing, locale,
                    "Cannot update Thing " + thingUID + " as it is not initialized.");
        }

        // only move on if Thing is known to be managed and handler is available, so it can get updated
        try {
            // note that we create a Configuration instance here in order to have normalized types
            thingRegistry.updateConfiguration(thingUIDObject,
                    new Configuration(
                            normalizeConfiguration(configurationParameters, thing.getThingTypeUID(), thing.getUID()))
                            .getProperties());
        } catch (ConfigValidationException ex) {
            logger.debug("Config description validation exception occurred for thingUID {} - Messages: {}", thingUID,
                    ex.getValidationMessages());
            return Response.status(Status.BAD_REQUEST).entity(ex.getValidationMessages(locale)).build();
        } catch (Exception ex) {
            logger.error("Exception during HTTP PUT request for update config at '{}'", uriInfo.getPath(), ex);
            return JSONResponse.createResponse(Status.INTERNAL_SERVER_ERROR, null, ex.getMessage());
        }

        return getThingResponse(Status.OK, thing, locale, null);
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getThingStatus", summary = "Gets thing status.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ThingStatusInfo.class))),
                    @ApiResponse(responseCode = "404", description = "Thing not found.") })
    public Response getStatus(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thing") String thingUID) throws IOException {
        ThingUID thingUIDObject = new ThingUID(thingUID);

        // Check if the Thing exists, 404 if not
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            logger.info("Received HTTP GET request for thing config status at '{}' for the unknown thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                localeService.getLocale(language));
        return Response.ok().entity(thingStatusInfo).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/enable")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "enableThing", summary = "Sets the thing enabled status.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedThingDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Thing not found.") })
    public Response setEnabled(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thing") String thingUID,
            @Parameter(description = "enabled") String enabled) throws IOException {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        // Check if the Thing exists, 404 if not
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            logger.info("Received HTTP PUT request for set enabled at '{}' for the unknown thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        thingManager.setEnabled(thingUIDObject, Boolean.valueOf(enabled));

        // everything went well
        return getThingResponse(Status.OK, thing, locale, null);
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/config/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getThingConfigStatus", summary = "Gets thing config status.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConfigStatusMessage.class)))),
                    @ApiResponse(responseCode = "404", description = "Thing not found.") })
    public Response getConfigStatus(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") String language,
            @PathParam("thingUID") @Parameter(description = "thing") String thingUID) throws IOException {
        ThingUID thingUIDObject = new ThingUID(thingUID);

        // Check if the Thing exists, 404 if not
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            logger.info("Received HTTP GET request for thing config status at '{}' for the unknown thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        ConfigStatusInfo info = configStatusService.getConfigStatus(thingUID, localeService.getLocale(language));
        if (info != null) {
            return Response.ok().entity(info.getConfigStatusMessages()).build();
        }
        return Response.ok().entity(Collections.EMPTY_SET).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/firmware/{firmwareVersion}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateThingFirmware", summary = "Update thing firmware.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Firmware update preconditions not satisfied."),
                    @ApiResponse(responseCode = "404", description = "Thing not found.") })
    public Response updateFirmware(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thing") String thingUID,
            @PathParam("firmwareVersion") @Parameter(description = "version") String firmwareVersion)
            throws IOException {
        Thing thing = thingRegistry.get(new ThingUID(thingUID));
        if (thing == null) {
            logger.info("Received HTTP PUT request for firmware update at '{}' for the unknown thing '{}'.",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }

        if (firmwareVersion.isEmpty()) {
            logger.info(
                    "Received HTTP PUT request for firmware update at '{}' for thing '{}' with unknown firmware version '{}'.",
                    uriInfo.getPath(), thingUID, firmwareVersion);
            return JSONResponse.createResponse(Status.BAD_REQUEST, null, "Firmware version is empty");
        }

        try {
            firmwareUpdateService.updateFirmware(thing.getUID(), firmwareVersion, localeService.getLocale(language));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return JSONResponse.createResponse(Status.BAD_REQUEST, null,
                    "Firmware update preconditions not satisfied.");
        }

        return Response.status(Status.OK).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/firmware/status")
    @Operation(operationId = "getThingFirmwareStatus", summary = "Gets thing's firmware status.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = FirmwareStatusDTO.class))),
                    @ApiResponse(responseCode = "204", description = "No firmware status provided by this Thing.") })
    public Response getFirmwareStatus(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thing") String thingUID) throws IOException {
        ThingUID thingUIDObject = new ThingUID(thingUID);
        FirmwareStatusDTO firmwareStatusDto = getThingFirmwareStatusInfo(thingUIDObject);
        if (firmwareStatusDto == null) {
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.ok(firmwareStatusDto, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}/firmwares")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAvailableFirmwaresForThing", summary = "Get all available firmwares for provided thing UID", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FirmwareDTO.class), uniqueItems = true))),
                    @ApiResponse(responseCode = "204", description = "No firmwares found.") })
    public Response getFirmwares(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        ThingUID aThingUID = new ThingUID(thingUID);
        Thing thing = thingRegistry.get(aThingUID);
        if (thing == null) {
            logger.info(
                    "Received HTTP GET request for listing available firmwares at {} for unknown thing with UID '{}'",
                    uriInfo.getPath(), thingUID);
            return getThingNotFoundResponse(thingUID);
        }
        Collection<Firmware> firmwares = firmwareRegistry.getFirmwares(thing, localeService.getLocale(language));

        if (firmwares.isEmpty()) {
            return Response.status(Status.NO_CONTENT).build();
        }

        Stream<FirmwareDTO> firmwareStream = firmwares.stream().map(this::convertToFirmwareDTO);
        return Response.ok().entity(new Stream2JSONInputStream(firmwareStream)).build();
    }

    private FirmwareDTO convertToFirmwareDTO(Firmware firmware) {
        return new FirmwareDTO(firmware.getThingTypeUID().getAsString(), firmware.getVendor(), firmware.getModel(),
                firmware.isModelRestricted(), firmware.getDescription(), firmware.getVersion(),
                firmware.getPrerequisiteVersion(), firmware.getChangelog());
    }

    private @Nullable FirmwareStatusDTO getThingFirmwareStatusInfo(ThingUID thingUID) {
        FirmwareStatusInfo info = firmwareUpdateService.getFirmwareStatusInfo(thingUID);
        if (info != null) {
            return buildFirmwareStatusDTO(info);
        }

        return null;
    }

    private FirmwareStatusDTO buildFirmwareStatusDTO(FirmwareStatusInfo info) {
        return new FirmwareStatusDTO(info.getFirmwareStatus().name(), info.getUpdatableFirmwareVersion());
    }

    /**
     * helper: Response to be sent to client if a Thing cannot be found
     *
     * @param thingUID
     * @return Response configured for NOT_FOUND
     */
    private static Response getThingNotFoundResponse(String thingUID) {
        String message = "Thing " + thingUID + " does not exist!";
        return JSONResponse.createResponse(Status.NOT_FOUND, null, message);
    }

    /**
     * helper: create a Response holding a Thing and/or error information.
     *
     * @param status
     * @param thing
     * @param errormessage an optional error message (may be null), ignored if the status family is successful
     * @return Response
     */
    private Response getThingResponse(Status status, @Nullable Thing thing, Locale locale,
            @Nullable String errormessage) {
        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                locale);
        boolean managed = thing != null && managedThingProvider.get(thing.getUID()) != null;
        EnrichedThingDTO enrichedThingDTO = thing != null
                ? EnrichedThingDTOMapper.map(thing, thingStatusInfo, this.getThingFirmwareStatusInfo(thing.getUID()),
                        getLinkedItemsMap(thing), managed)
                : null;

        return JSONResponse.createResponse(status, enrichedThingDTO, errormessage);
    }

    private EnrichedThingDTO convertToEnrichedThingDTO(Thing thing, Locale locale) {
        boolean managed = managedThingProvider.get(thing.getUID()) != null;
        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                locale);
        return EnrichedThingDTOMapper.map(thing, thingStatusInfo, this.getThingFirmwareStatusInfo(thing.getUID()),
                getLinkedItemsMap(thing), managed);
    }

    private Map<String, Set<String>> getLinkedItemsMap(Thing thing) {
        Map<String, Set<String>> linkedItemsMap = new HashMap<>();
        for (Channel channel : thing.getChannels()) {
            Set<String> linkedItems = itemChannelLinkRegistry.getLinkedItemNames(channel.getUID());
            linkedItemsMap.put(channel.getUID().getId(), linkedItems);
        }
        return linkedItemsMap;
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);

        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription typeConfigDesc = configDescRegistry.getConfigDescription(descURI);
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }

        if (thingUID != null) {
            ConfigDescription thingConfigDesc = configDescRegistry
                    .getConfigDescription(getConfigDescriptionURI(thingUID));
            if (thingConfigDesc != null) {
                configDescriptions.add(thingConfigDesc);
            }
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private @Nullable Map<String, Object> normalizeConfiguration(Map<String, Object> properties,
            ChannelTypeUID channelTypeUID, ChannelUID channelUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);
        URI descURI = channelType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription typeConfigDesc = configDescRegistry.getConfigDescription(descURI);
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }
        if (getConfigDescriptionURI(channelUID) != null) {
            ConfigDescription channelConfigDesc = configDescRegistry
                    .getConfigDescription(getConfigDescriptionURI(channelUID));
            if (channelConfigDesc != null) {
                configDescriptions.add(channelConfigDesc);
            }
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private void normalizeChannels(ThingDTO thingBean, ThingUID thingUID) {
        if (thingBean.channels != null) {
            for (ChannelDTO channelBean : thingBean.channels) {
                if (channelBean.channelTypeUID != null) {
                    channelBean.configuration = normalizeConfiguration(channelBean.configuration,
                            new ChannelTypeUID(channelBean.channelTypeUID), new ChannelUID(thingUID, channelBean.id));
                }
            }
        }
    }

    private URI getConfigDescriptionURI(ThingUID thingUID) {
        String uriString = "thing:" + thingUID;
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI syntax: " + uriString);
        }
    }

    private URI getConfigDescriptionURI(ChannelUID channelUID) {
        String uriString = "channel:" + channelUID;
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI syntax: " + uriString);
        }
    }
}
