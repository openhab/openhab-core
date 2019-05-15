/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.core.internal.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NoContentException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTO;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTOMapper;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterDTO;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.thing.dto.ChannelTypeDTO;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeRegistry;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfileType;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

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
 */
@Path(ChannelTypeResource.PATH_CHANNEL_TYPES)
@RolesAllowed({ Role.ADMIN })
@Api(value = ChannelTypeResource.PATH_CHANNEL_TYPES)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ChannelTypeResource {

    /** The URI path to this resource */
    public static final String PATH_CHANNEL_TYPES = "channel-types";

    @Reference
    protected @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;

    @Reference
    protected @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;

    @Reference
    protected @NonNullByDefault({}) ProfileTypeRegistry profileTypeRegistry;

    @Reference
    protected @NonNullByDefault({}) LocaleService localeService;

    @GET
    @ApiOperation(value = "Gets all available channel types.", response = ChannelTypeDTO.class, responseContainer = "Set")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = ChannelTypeDTO.class, responseContainer = "Set"))
    public Stream<?> getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        return channelTypeRegistry.getChannelTypes(locale).stream().map(c -> convertToChannelTypeDTO(c, locale));
    }

    @GET
    @Path("/{channelTypeUID}")
    @ApiOperation(value = "Gets channel type by UID.", response = ChannelTypeDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Channel type with provided channelTypeUID does not exist.", response = ChannelTypeDTO.class),
            @ApiResponse(code = 404, message = "Not found") })
    public Object getByUID(@PathParam("channelTypeUID") @ApiParam(value = "channelTypeUID") String channelTypeUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language) {
        Locale locale = localeService.getLocale(language);
        ChannelType channelType = channelTypeRegistry.getChannelType(new ChannelTypeUID(channelTypeUID), locale);
        if (channelType != null) {
            return convertToChannelTypeDTO(channelType, locale);
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{channelTypeUID}/linkableItemTypes")
    @ApiOperation(value = "Gets the item types the given trigger channel type UID can be linked to.", response = String.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "Set"),
            @ApiResponse(code = 204, message = "No content: channel type has no linkable items or is no trigger channel."),
            @ApiResponse(code = 404, message = "Given channel type UID not found.") })
    public Stream<String> getLinkableItemTypes(
            @PathParam("channelTypeUID") @ApiParam(value = "channelTypeUID") String channelTypeUID)
            throws NoContentException {
        ChannelTypeUID ctUID = new ChannelTypeUID(channelTypeUID);
        ChannelType channelType = channelTypeRegistry.getChannelType(ctUID);
        if (channelType == null) {
            throw new NotFoundException();
        }
        if (channelType.getKind() != ChannelKind.TRIGGER) {
            throw new NoContentException("channel type is no trigger channel");
        }

        return profileTypeRegistry.getProfileTypes().stream()
                .filter(profileType -> profileType instanceof TriggerProfileType
                        && ((TriggerProfileType) profileType).getSupportedChannelTypeUIDs().contains(ctUID)) //
                .map(profileType -> profileType.getSupportedItemTypes()) //
                .flatMap(supportedItemTypes -> supportedItemTypes.stream());
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
}
