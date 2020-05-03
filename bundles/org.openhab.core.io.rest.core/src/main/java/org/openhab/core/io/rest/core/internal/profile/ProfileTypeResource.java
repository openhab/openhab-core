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
package org.openhab.core.io.rest.core.internal.profile;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.profiles.StateProfileType;
import org.openhab.core.thing.profiles.TriggerProfileType;
import org.openhab.core.thing.profiles.dto.ProfileTypeDTO;
import org.openhab.core.thing.profiles.dto.ProfileTypeDTOMapper;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST resource to obtain profile-types
 *
 * @author Stefan Triller - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsResource
@JaxrsName(ProfileTypeResource.PATH_PROFILE_TYPES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ProfileTypeResource.PATH_PROFILE_TYPES)
@RolesAllowed({ Role.ADMIN })
@Api(ProfileTypeResource.PATH_PROFILE_TYPES)
public class ProfileTypeResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_PROFILE_TYPES = "profile-types";

    private final Logger logger = LoggerFactory.getLogger(ProfileTypeResource.class);

    private ProfileTypeRegistry profileTypeRegistry;
    private ChannelTypeRegistry channelTypeRegistry;
    private LocaleService localeService;

    @GET
    @RolesAllowed({ Role.USER })
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all available profile types.", response = ProfileTypeDTO.class, responseContainer = "Set")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = ProfileTypeDTO.class, responseContainer = "Set"))
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language,
            @QueryParam("channelTypeUID") @ApiParam(value = "channel type filter", required = false) @Nullable String channelTypeUID,
            @QueryParam("itemType") @ApiParam(value = "item type filter", required = false) @Nullable String itemType) {
        Locale locale = localeService.getLocale(language);
        return Response.ok(new Stream2JSONInputStream(getProfileTypes(locale, channelTypeUID, itemType))).build();
    }

    protected Stream<ProfileTypeDTO> getProfileTypes(Locale locale, String channelTypeUID, String itemType) {
        return profileTypeRegistry.getProfileTypes(locale).stream().filter(matchesChannelUID(channelTypeUID, locale))
                .filter(matchesItemType(itemType)).map(t -> convertToProfileTypeDTO(t, locale));
    }

    private Predicate<ProfileType> matchesChannelUID(String channelTypeUID, Locale locale) {
        if (channelTypeUID == null) {
            return t -> true;
        }
        ChannelType channelType = channelTypeRegistry.getChannelType(new ChannelTypeUID(channelTypeUID), locale);
        if (channelType == null) {
            // requested to filter against an unknown channel type -> do not return a ProfileType
            return t -> false;
        }
        switch (channelType.getKind()) {
            case STATE:
                return t -> stateProfileMatchesProfileType(t, channelType);
            case TRIGGER:
                return t -> triggerProfileMatchesProfileType(t, channelType);
        }
        return t -> false;
    }

    private Predicate<ProfileType> matchesItemType(String itemType) {
        if (itemType == null) {
            return t -> true;
        }
        return t -> profileTypeMatchesItemType(t, itemType);
    }

    private boolean profileTypeMatchesItemType(ProfileType pt, String itemType) {
        Collection<String> supportedItemTypesOnProfileType = pt.getSupportedItemTypes();
        if (supportedItemTypesOnProfileType.isEmpty()
                || supportedItemTypesOnProfileType.contains(ItemUtil.getMainItemType(itemType))
                || supportedItemTypesOnProfileType.contains(itemType)) {
            return true;
        }
        return false;
    }

    private boolean triggerProfileMatchesProfileType(ProfileType profileType, ChannelType channelType) {
        if (profileType instanceof TriggerProfileType) {
            TriggerProfileType triggerProfileType = (TriggerProfileType) profileType;

            if (triggerProfileType.getSupportedChannelTypeUIDs().isEmpty()) {
                return true;
            }

            if (triggerProfileType.getSupportedChannelTypeUIDs().contains(channelType.getUID())) {
                return true;
            }
        }
        return false;
    }

    private boolean stateProfileMatchesProfileType(ProfileType profileType, ChannelType channelType) {
        if (profileType instanceof StateProfileType) {
            StateProfileType stateProfileType = (StateProfileType) profileType;

            if (stateProfileType.getSupportedItemTypesOfChannel().isEmpty()) {
                return true;
            }

            Collection<String> supportedItemTypesOfChannelOnProfileType = stateProfileType
                    .getSupportedItemTypesOfChannel();
            if (supportedItemTypesOfChannelOnProfileType.contains(ItemUtil.getMainItemType(channelType.getItemType()))
                    || supportedItemTypesOfChannelOnProfileType.contains(channelType.getItemType())) {
                return true;
            }
        }
        return false;
    }

    private ProfileTypeDTO convertToProfileTypeDTO(ProfileType profileType, Locale locale) {
        final ProfileTypeDTO profileTypeDTO = ProfileTypeDTOMapper.map(profileType);
        if (profileTypeDTO != null) {
            return profileTypeDTO;
        } else {
            logger.warn("Cannot create DTO for profileType '{}'. Skipping it.", profileTypeDTO);
        }

        return null;
    }

    @Override
    public boolean isSatisfied() {
        return (this.profileTypeRegistry != null && channelTypeRegistry != null);
    }

    @Reference
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setProfileTypeRegistry(ProfileTypeRegistry registry) {
        this.profileTypeRegistry = registry;
    }

    protected void unsetProfileTypeRegistry(ProfileTypeRegistry registry) {
        this.profileTypeRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setChannelTypeRegistry(ChannelTypeRegistry registry) {
        this.channelTypeRegistry = registry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry registry) {
        this.channelTypeRegistry = null;
    }
}
