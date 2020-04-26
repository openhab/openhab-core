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
package org.openhab.core.io.rest.core.internal.channel;

import java.util.ArrayList;
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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionDTOMapper;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.io.rest.LocaleService;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Provides access to ChannelType via REST.
 *
 * @author Chris Jackson - Initial contribution
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Yannick Schaus - Added filter to getAll
 */
@Path(ChannelTypeResource.PATH_CHANNEL_TYPES)
@RolesAllowed({ Role.ADMIN })
@Api(value = ChannelTypeResource.PATH_CHANNEL_TYPES)
@Component
public class ChannelTypeResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_CHANNEL_TYPES = "channel-types";

    private ChannelTypeRegistry channelTypeRegistry;
    private ConfigDescriptionRegistry configDescriptionRegistry;

    private ProfileTypeRegistry profileTypeRegistry;

    private LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    protected void unsetConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setProfileTypeRegistry(ProfileTypeRegistry profileTypeRegistry) {
        this.profileTypeRegistry = profileTypeRegistry;
    }

    protected void unsetProfileTypeRegistry(ProfileTypeRegistry profileTypeRegistry) {
        this.profileTypeRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all available channel types.", response = ChannelTypeDTO.class, responseContainer = "Set")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = ChannelTypeDTO.class, responseContainer = "Set"))
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language,
            @QueryParam("prefixes") @ApiParam(value = "filter UIDs by prefix (multiple comma-separated prefixes allowed, for example: 'system,mqtt')", required = false) @Nullable String prefixes) {
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
    @ApiOperation(value = "Gets channel type by UID.", response = ChannelTypeDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Channel type with provided channelTypeUID does not exist.", response = ChannelTypeDTO.class),
            @ApiResponse(code = 404, message = "No content") })
    public Response getByUID(@PathParam("channelTypeUID") @ApiParam(value = "channelTypeUID") String channelTypeUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language) {
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
    @ApiOperation(value = "Gets the item types the given trigger channel type UID can be linked to.", response = String.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "Set"),
            @ApiResponse(code = 204, message = "No content: channel type has no linkable items or is no trigger channel."),
            @ApiResponse(code = 404, message = "Given channel type UID not found.") })
    public Response getLinkableItemTypes(
            @PathParam("channelTypeUID") @ApiParam(value = "channelTypeUID") String channelTypeUID) {
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
        final ConfigDescription configDescription;
        if (channelType.getConfigDescriptionURI() != null) {
            configDescription = this.configDescriptionRegistry
                    .getConfigDescription(channelType.getConfigDescriptionURI(), locale);
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

        return new ChannelTypeDTO(channelType.getUID().toString(), channelType.getLabel(), channelType.getDescription(),
                channelType.getCategory(), channelType.getItemType(), channelType.getKind(), parameters,
                parameterGroups, channelType.getState(), channelType.getTags(), channelType.isAdvanced(),
                channelType.getCommandDescription());
    }

    @Override
    public boolean isSatisfied() {
        return channelTypeRegistry != null && configDescriptionRegistry != null && profileTypeRegistry != null
                && localeService != null;
    }
}
