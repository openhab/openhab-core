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
package org.openhab.core.io.rest.core.internal.channel;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionDTOMapper;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.thing.dto.ChannelTypeDTO;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.profiles.TriggerProfileType;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Provides access to ChannelType via REST.
 *
 * @author Chris Jackson - Initial contribution
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Yannick Schaus - Added filter to getAll
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(ChannelTypeResource.PATH_CHANNEL_TYPES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ChannelTypeResource.PATH_CHANNEL_TYPES)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = ChannelTypeResource.PATH_CHANNEL_TYPES)
@NonNullByDefault
public class ChannelTypeResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_CHANNEL_TYPES = "channel-types";

    private final ChannelTypeRegistry channelTypeRegistry;
    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final LocaleService localeService;
    private final ProfileTypeRegistry profileTypeRegistry;

    @Activate
    public ChannelTypeResource( //
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference LocaleService localeService, //
            final @Reference ProfileTypeRegistry profileTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.localeService = localeService;
        this.profileTypeRegistry = profileTypeRegistry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getChannelTypes", summary = "Gets all available channel types.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChannelTypeDTO.class), uniqueItems = true))) })
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("prefixes") @Parameter(description = "filter UIDs by prefix (multiple comma-separated prefixes allowed, for example: 'system,mqtt')") @Nullable String prefixes) {
        Locale locale = localeService.getLocale(language);

        Stream<ChannelTypeDTO> channelStream = channelTypeRegistry.getChannelTypes(locale).stream()
                .map(c -> convertToChannelTypeDTO(c, locale));

        if (prefixes != null) {
            Predicate<ChannelTypeDTO> filter = ct -> false;
            for (String prefix : prefixes.split(",")) {
                filter = filter.or(ct -> ct.UID.startsWith(prefix + ":"));
            }
            channelStream = channelStream.filter(filter);
        }

        return Response.ok(new Stream2JSONInputStream(channelStream)).build();
    }

    @GET
    @Path("/{channelTypeUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getChannelTypeByUID", summary = "Gets channel type by UID.", responses = {
            @ApiResponse(responseCode = "200", description = "Channel type with provided channelTypeUID does not exist.", content = @Content(schema = @Schema(implementation = ChannelTypeDTO.class))),
            @ApiResponse(responseCode = "404", description = "No content") })
    public Response getByUID(
            @PathParam("channelTypeUID") @Parameter(description = "channelTypeUID") String channelTypeUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        ChannelType channelType = channelTypeRegistry.getChannelType(new ChannelTypeUID(channelTypeUID), locale);
        if (channelType != null) {
            return Response.ok(convertToChannelTypeDTO(channelType, locale)).build();
        } else {
            return Response.noContent().build();
        }
    }

    @GET
    @Path("/{channelTypeUID}/linkableItemTypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getLinkableItemTypesByChannelTypeUID", summary = "Gets the item types the given trigger channel type UID can be linked to.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class), uniqueItems = true))),
            @ApiResponse(responseCode = "204", description = "No content: channel type has no linkable items or is no trigger channel."),
            @ApiResponse(responseCode = "404", description = "Given channel type UID not found.") })
    public Response getLinkableItemTypes(
            @PathParam("channelTypeUID") @Parameter(description = "channelTypeUID") String channelTypeUID) {
        ChannelTypeUID ctUID = new ChannelTypeUID(channelTypeUID);
        ChannelType channelType = channelTypeRegistry.getChannelType(ctUID);
        if (channelType == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        if (channelType.getKind() != ChannelKind.TRIGGER) {
            return Response.noContent().build();
        }

        Set<String> result = new HashSet<>();
        for (ProfileType profileType : profileTypeRegistry.getProfileTypes()) {
            if (profileType instanceof TriggerProfileType) {
                if (((TriggerProfileType) profileType).getSupportedChannelTypeUIDs().contains(ctUID)) {
                    for (String itemType : profileType.getSupportedItemTypes()) {
                        result.add(itemType);
                    }
                }
            }
        }
        if (result.isEmpty()) {
            return Response.noContent().build();
        }

        return Response.ok(result).build();
    }

    private ChannelTypeDTO convertToChannelTypeDTO(ChannelType channelType, Locale locale) {
        final URI descURI = channelType.getConfigDescriptionURI();
        final ConfigDescription configDescription = descURI == null ? null
                : configDescriptionRegistry.getConfigDescription(descURI, locale);
        final ConfigDescriptionDTO configDescriptionDTO = configDescription == null ? null
                : ConfigDescriptionDTOMapper.map(configDescription);

        final List<ConfigDescriptionParameterDTO> parameters = configDescriptionDTO == null ? List.of()
                : configDescriptionDTO.parameters;
        final List<ConfigDescriptionParameterGroupDTO> parameterGroups = configDescriptionDTO == null ? List.of()
                : configDescriptionDTO.parameterGroups;

        return new ChannelTypeDTO(channelType.getUID().toString(), channelType.getLabel(), channelType.getDescription(),
                channelType.getCategory(), channelType.getItemType(), channelType.getKind(), parameters,
                parameterGroups, channelType.getState(), channelType.getTags(), channelType.isAdvanced(),
                channelType.getCommandDescription());
    }
}
