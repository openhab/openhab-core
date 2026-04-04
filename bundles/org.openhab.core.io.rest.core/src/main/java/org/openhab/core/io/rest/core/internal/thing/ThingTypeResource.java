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
package org.openhab.core.io.rest.core.internal.thing;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;

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
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.dto.ChannelDefinitionDTO;
import org.openhab.core.thing.dto.ChannelGroupDefinitionDTO;
import org.openhab.core.thing.dto.StrippedThingTypeDTO;
import org.openhab.core.thing.dto.StrippedThingTypeDTOMapper;
import org.openhab.core.thing.dto.ThingTypeDTO;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.jakartars.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * ThingTypeResource provides access to ThingType via REST.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Chris Jackson - Added parameter groups, advanced, multipleLimit,
 *         limitToOptions
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Miki Jankov - Introducing StrippedThingTypeDTO
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Yannick Schaus - Added filter to getAll
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 * @author Andrew Fiddian-Green - Added semanticEquipmentTag
 */
@Component
@JakartarsResource
@JakartarsName(ThingTypeResource.PATH_THING_TYPES)
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ThingTypeResource.PATH_THING_TYPES)
@Tag(name = ThingTypeResource.PATH_THING_TYPES)
@NonNullByDefault
public class ThingTypeResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_THING_TYPES = "thing-types";

    private final Logger logger = LoggerFactory.getLogger(ThingTypeResource.class);

    private final ChannelTypeRegistry channelTypeRegistry;
    private final ChannelGroupTypeRegistry channelGroupTypeRegistry;
    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final LocaleService localeService;
    private final ThingTypeRegistry thingTypeRegistry;

    @Activate
    public ThingTypeResource( //
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ChannelGroupTypeRegistry channelGroupTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference LocaleService localeService, //
            final @Reference ThingTypeRegistry thingTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
        this.channelGroupTypeRegistry = channelGroupTypeRegistry;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.localeService = localeService;
        this.thingTypeRegistry = thingTypeRegistry;
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getThingTypes", summary = "Gets all available thing types without config description, channels and properties.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StrippedThingTypeDTO.class), uniqueItems = true))) })
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("bindingId") @Parameter(description = "filter by binding Id") @Nullable String bindingId) {
        Locale locale = localeService.getLocale(language);
        Stream<StrippedThingTypeDTO> typeStream = thingTypeRegistry.getThingTypes(locale).stream()
                .map(thingType -> StrippedThingTypeDTOMapper.map(thingType, locale));

        if (bindingId != null) {
            typeStream = typeStream.filter(type -> type.UID.startsWith(bindingId + ':'));
        }

        return Response.ok(new Stream2JSONInputStream(typeStream)).build();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{thingTypeUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getThingTypeById", summary = "Gets thing type by UID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ThingTypeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Thing type not found.") })
    public Response getByUID(@PathParam("thingTypeUID") @Parameter(description = "thingTypeUID") String thingTypeUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        ThingType thingType = thingTypeRegistry.getThingType(new ThingTypeUID(thingTypeUID), locale);
        if (thingType != null) {
            return Response.ok(convertToThingTypeDTO(thingType, locale)).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    private ThingTypeDTO convertToThingTypeDTO(ThingType thingType, Locale locale) {
        final ConfigDescription configDescription;

        final URI descURI = thingType.getConfigDescriptionURI();
        configDescription = descURI == null ? null : configDescriptionRegistry.getConfigDescription(descURI, locale);

        List<ConfigDescriptionParameterDTO> parameters;
        List<ConfigDescriptionParameterGroupDTO> parameterGroups;

        if (configDescription != null) {
            ConfigDescriptionDTO configDescriptionDTO = ConfigDescriptionDTOMapper.map(configDescription);
            parameters = configDescriptionDTO.parameters;
            parameterGroups = configDescriptionDTO.parameterGroups;
        } else {
            parameters = new ArrayList<>(0);
            parameterGroups = new ArrayList<>(0);
        }

        final List<ChannelDefinitionDTO> channelDefinitions = convertToChannelDefinitionDTOs(
                thingType.getChannelDefinitions(), locale);

        return new ThingTypeDTO(thingType.getUID().toString(), thingType.getLabel(), thingType.getDescription(),
                thingType.getCategory(), thingType.isListed(), parameters, channelDefinitions,
                convertToChannelGroupDefinitionDTOs(thingType.getChannelGroupDefinitions(), locale),
                thingType.getSupportedBridgeTypeUIDs(), thingType.getProperties(), thingType instanceof BridgeType,
                parameterGroups, thingType.getExtensibleChannelTypeIds(), thingType.getSemanticEquipmentTag());
    }

    private List<ChannelGroupDefinitionDTO> convertToChannelGroupDefinitionDTOs(
            List<ChannelGroupDefinition> channelGroupDefinitions, Locale locale) {
        List<ChannelGroupDefinitionDTO> channelGroupDefinitionDTOs = new ArrayList<>();
        for (ChannelGroupDefinition channelGroupDefinition : channelGroupDefinitions) {
            String id = channelGroupDefinition.getId();
            ChannelGroupType channelGroupType = channelGroupTypeRegistry
                    .getChannelGroupType(channelGroupDefinition.getTypeUID(), locale);

            // Default to the channelGroupDefinition label/description to override the channelGroupType
            String label = channelGroupDefinition.getLabel();
            String description = channelGroupDefinition.getDescription();
            List<ChannelDefinition> channelDefinitions = List.of();

            if (channelGroupType == null) {
                logger.warn("Cannot find channel group type: {}", channelGroupDefinition.getTypeUID());
            } else {
                if (label == null) {
                    label = channelGroupType.getLabel();
                }
                if (description == null) {
                    description = channelGroupType.getDescription();
                }
                channelDefinitions = channelGroupType.getChannelDefinitions();
            }

            List<ChannelDefinitionDTO> channelDefinitionDTOs = convertToChannelDefinitionDTOs(channelDefinitions,
                    locale);

            channelGroupDefinitionDTOs
                    .add(new ChannelGroupDefinitionDTO(id, label, description, channelDefinitionDTOs));
        }
        return channelGroupDefinitionDTOs;
    }

    private List<ChannelDefinitionDTO> convertToChannelDefinitionDTOs(List<ChannelDefinition> channelDefinitions,
            Locale locale) {
        List<ChannelDefinitionDTO> channelDefinitionDTOs = new ArrayList<>();
        for (ChannelDefinition channelDefinition : channelDefinitions) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channelDefinition.getChannelTypeUID(), locale);

            if (channelType == null) {
                logger.warn("Cannot find channel type: {}", channelDefinition.getChannelTypeUID());
            } else {
                // Default to the channelDefinition label to override the
                // channelType
                String label = channelDefinition.getLabel();
                if (label == null) {
                    label = channelType.getLabel();
                }

                // Default to the channelDefinition description to override the
                // channelType
                String description = channelDefinition.getDescription();
                if (description == null) {
                    description = channelType.getDescription();
                }

                ChannelDefinitionDTO channelDefinitionDTO = new ChannelDefinitionDTO(channelDefinition.getId(),
                        channelDefinition.getChannelTypeUID().toString(), label, description, channelType.getTags(),
                        channelType.getCategory(), channelType.getState(), channelType.isAdvanced(),
                        channelDefinition.getProperties());
                channelDefinitionDTOs.add(channelDefinitionDTO);
            }
        }
        return channelDefinitionDTOs;
    }
}
