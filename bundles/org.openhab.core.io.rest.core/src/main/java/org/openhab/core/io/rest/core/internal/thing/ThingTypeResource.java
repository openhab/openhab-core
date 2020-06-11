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
package org.openhab.core.io.rest.core.internal.thing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
 */
@Component
@JaxrsResource
@JaxrsName(ThingTypeResource.PATH_THING_TYPES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ThingTypeResource.PATH_THING_TYPES)
@Api(ThingTypeResource.PATH_THING_TYPES)
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
    @RolesAllowed({ Role.USER })
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all available thing types without config description, channels and properties.", response = StrippedThingTypeDTO.class, responseContainer = "Set")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = StrippedThingTypeDTO.class, responseContainer = "Set"))
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @QueryParam("bindingId") @ApiParam(value = "filter by binding Id") @Nullable String bindingId) {
        Locale locale = localeService.getLocale(language);
        Stream<StrippedThingTypeDTO> typeStream = thingTypeRegistry.getThingTypes(locale).stream()
                .map(thingType -> StrippedThingTypeDTOMapper.map(thingType, locale));

        if (bindingId != null) {
            typeStream = typeStream.filter(type -> type.UID.startsWith(bindingId + ':'));
        }

        return Response.ok(new Stream2JSONInputStream(typeStream)).build();
    }

    @GET
    @RolesAllowed({ Role.USER })
    @Path("/{thingTypeUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets thing type by UID.", response = ThingTypeDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Thing type with provided thingTypeUID does not exist.", response = ThingTypeDTO.class),
            @ApiResponse(code = 404, message = "No content") })
    public Response getByUID(@PathParam("thingTypeUID") @ApiParam(value = "thingTypeUID") String thingTypeUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        ThingType thingType = thingTypeRegistry.getThingType(new ThingTypeUID(thingTypeUID), locale);
        if (thingType != null) {
            return Response.ok(convertToThingTypeDTO(thingType, locale)).build();
        } else {
            return Response.noContent().build();
        }
    }

    private ThingTypeDTO convertToThingTypeDTO(ThingType thingType, Locale locale) {
        final ConfigDescription configDescription;
        if (thingType.getConfigDescriptionURI() != null) {
            configDescription = this.configDescriptionRegistry.getConfigDescription(thingType.getConfigDescriptionURI(),
                    locale);
        } else {
            configDescription = null;
        }

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
                parameterGroups, thingType.getExtensibleChannelTypeIds());
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
            List<ChannelDefinition> channelDefinitions = Collections.emptyList();

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
