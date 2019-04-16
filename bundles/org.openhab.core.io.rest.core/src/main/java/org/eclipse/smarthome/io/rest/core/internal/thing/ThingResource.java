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
package org.eclipse.smarthome.io.rest.core.internal.thing;

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
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.ConfigUtil;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.status.ConfigStatusInfo;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.config.core.status.ConfigStatusService;
import org.eclipse.smarthome.config.core.validation.ConfigValidationException;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.items.ItemFactory;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ManagedItemProvider;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingManager;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ChannelDTOMapper;
import org.eclipse.smarthome.core.thing.dto.StrippedThingTypeDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTOMapper;
import org.eclipse.smarthome.core.thing.firmware.FirmwareRegistry;
import org.eclipse.smarthome.core.thing.firmware.FirmwareStatusInfo;
import org.eclipse.smarthome.core.thing.firmware.FirmwareUpdateService;
import org.eclipse.smarthome.core.thing.firmware.dto.FirmwareDTO;
import org.eclipse.smarthome.core.thing.firmware.dto.FirmwareStatusDTO;
import org.eclipse.smarthome.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.link.ManagedItemChannelLinkProvider;
import org.eclipse.smarthome.core.thing.type.BridgeType;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.eclipse.smarthome.core.thing.util.ThingHelper;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTOMapper;
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
 */
@Path(ThingResource.PATH_THINGS)
@Api(value = ThingResource.PATH_THINGS)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ThingResource {
    public static final String PATH_THINGS = "things";

    @Reference
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    @Reference
    private @NonNullByDefault({}) ItemFactory itemFactory;
    @Reference
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    @Reference
    private @NonNullByDefault({}) ManagedItemChannelLinkProvider managedItemChannelLinkProvider;
    @Reference
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;
    @Reference
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    @Reference
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    @Reference
    private @NonNullByDefault({}) ConfigStatusService configStatusService;
    @Reference
    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescRegistry;
    @Reference
    private @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    @Reference
    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;
    @Reference
    private @NonNullByDefault({}) ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;
    @Reference
    private @NonNullByDefault({}) FirmwareUpdateService firmwareUpdateService;
    @Reference
    private @NonNullByDefault({}) FirmwareRegistry firmwareRegistry;
    @Reference
    private @NonNullByDefault({}) ThingManager thingManager;
    @Reference
    private @NonNullByDefault({}) LocaleService localeService;

    /**
     * create a new Thing
     *
     * @param thingBean
     * @return Response holding the newly created Thing or error information
     */
    @POST
    @RolesAllowed({ Role.ADMIN })
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new thing and adds it to the registry.")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created", response = String.class),
            @ApiResponse(code = 400, message = "A uid must be provided, if no binding can create a thing of this type."),
            @ApiResponse(code = 409, message = "A thing with the same uid already exists.") })
    public EnrichedThingDTO create(@Context HttpServletResponse response,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "thing data", required = true) ThingDTO thingBean) {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUID = thingBean.UID == null ? null : new ThingUID(thingBean.UID);
        ThingTypeUID thingTypeUID = new ThingTypeUID(thingBean.thingTypeUID);

        if (thingUID != null) {
            // check if a thing with this UID already exists
            Thing thing = thingRegistry.get(thingUID);
            if (thing != null) {
                throw new WebApplicationException("Thing " + thingUID.toString() + " already exists!", 409);
            }
        }

        ThingUID bridgeUID = null;

        if (thingBean.bridgeUID != null) {
            bridgeUID = new ThingUID(thingBean.bridgeUID);
        }

        // turn the ThingDTO's configuration into a Configuration
        Configuration configuration = new Configuration(
                normalizeConfiguration(thingBean.configuration, thingTypeUID, thingUID));
        normalizeChannels(thingBean, thingUID);

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
            throw new BadRequestException("A UID must be provided, since no binding can create the thing!");
        }

        thingRegistry.add(thing);

        response.setStatus(Response.Status.CREATED.getStatusCode());
        return convertToEnrichedThingDTO(thing, locale);
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available things.", response = EnrichedThingDTO.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = EnrichedThingDTO.class, responseContainer = "Set") })
    public Stream<EnrichedThingDTO> getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) {
        final Locale locale = localeService.getLocale(language);
        return thingRegistry.stream().map(t -> convertToEnrichedThingDTO(t, locale)).distinct();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/{thingUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets thing by UID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ThingDTO.class),
            @ApiResponse(code = 404, message = "Thing not found.") })
    public EnrichedThingDTO getByUID(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("thingUID") @ApiParam(value = "thingUID") String thingUID) {
        final Locale locale = localeService.getLocale(language);

        Thing thing = thingRegistry.get((new ThingUID(thingUID)));

        if (thing != null) {
            return convertToEnrichedThingDTO(thing, locale);
        } else {
            throw new NotFoundException();
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
    @ApiOperation(value = "Removes a thing from the registry. Set \'force\' to __true__ if you want the thing te be removed immediately.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK, was deleted or is prepared for deletion."),
            @ApiResponse(code = 404, message = "Thing not found."),
            @ApiResponse(code = 409, message = "Thing could not be deleted because it's not editable.") })
    public void remove(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("thingUID") @ApiParam(value = "thingUID") String thingUID,
            @DefaultValue("false") @QueryParam("force") @ApiParam(value = "force") boolean force) {
        ThingUID thingUIDObject = new ThingUID(thingUID);

        // check whether thing exists and throw 404 if not
        Thing thing = thingRegistry.get(thingUIDObject);
        if (thing == null) {
            throw new NotFoundException();
        }

        // ask whether the Thing exists as a managed thing, so it can get
        // updated, 409 otherwise
        Thing managed = managedThingProvider.get(thingUIDObject);
        if (null == managed) {
            throw new WebApplicationException("Cannot delete Thing " + thingUID + " as it is not editable.", 409);
        }

        // only move on if Thing is known to be managed, so it can get updated
        if (force) {
            if (null == thingRegistry.forceRemove(thingUIDObject)) {
                throw new InternalServerErrorException("Cannot delete Thing " + thingUID + " for unknown reasons.");
            }
        } else {
            thingRegistry.remove(thingUIDObject);
        }
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
    @ApiOperation(value = "Updates a thing.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ThingDTO.class),
            @ApiResponse(code = 404, message = "Thing not found."),
            @ApiResponse(code = 409, message = "Thing could not be updated as it is not editable.") })
    public EnrichedThingDTO update(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("thingUID") @ApiParam(value = "thingUID") String thingUID,
            @ApiParam(value = "thing", required = true) ThingDTO thingBean) throws IOException {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        // ask whether the Thing exists at all, 404 otherwise
        Thing thing = thingRegistry.get(thingUIDObject);
        if (null == thing) {
            throw new NotFoundException();
        }

        // ask whether the Thing exists as a managed thing, so it can get
        // updated, 409 otherwise
        Thing managed = managedThingProvider.get(thingUIDObject);
        if (null == managed) {
            throw new WebApplicationException("Cannot update Thing " + thingUID + " as it is not editable.", 409);
        }

        // check configuration
        thingBean.configuration = normalizeConfiguration(thingBean.configuration, thing.getThingTypeUID(),
                thing.getUID());
        normalizeChannels(thingBean, thing.getUID());

        thing = ThingHelper.merge(thing, thingBean);

        // update, returns null in case Thing cannot be found
        Thing oldthing = managedThingProvider.update(thing);
        if (null == oldthing) {
            throw new NotFoundException();
        }

        return convertToEnrichedThingDTO(thing, locale);
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
    @ApiOperation(value = "Updates thing's configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ThingDTO.class),
            @ApiResponse(code = 400, message = "Configuration of the thing is not valid."),
            @ApiResponse(code = 404, message = "Thing not found"),
            @ApiResponse(code = 409, message = "Thing could not be updated as it is not editable.") })
    public EnrichedThingDTO updateConfiguration(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @PathParam("thingUID") @ApiParam(value = "thing") String thingUID,
            @ApiParam(value = "configuration parameters") Map<String, Object> configurationParameters)
            throws IOException {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        // ask whether the Thing exists at all, 404 otherwise
        Thing thing = thingRegistry.get(thingUIDObject);
        if (null == thing) {
            throw new NotFoundException();
        }

        // ask whether the Thing exists as a managed thing, so it can get
        // updated, 409 otherwise
        Thing managed = managedThingProvider.get(thingUIDObject);
        if (null == managed) {
            throw new WebApplicationException("Cannot update Thing " + thingUID + " as it is not editable.", 409);
        }

        // only move on if Thing is known to be managed, so it can get updated
        try {
            // note that we create a Configuration instance here in order to
            // have normalized types
            thingRegistry.updateConfiguration(thingUIDObject,
                    new Configuration(
                            normalizeConfiguration(configurationParameters, thing.getThingTypeUID(), thing.getUID()))
                                    .getProperties());
        } catch (ConfigValidationException ex) {
            throw new BadRequestException(ex);
        } catch (Exception ex) {
            throw new InternalServerErrorException(ex);
        }

        return convertToEnrichedThingDTO(thing, locale);
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{thingUID}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets thing's status.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Thing not found.") })
    public Response getStatus(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @PathParam("thingUID") @ApiParam(value = "thing") String thingUID) throws IOException {
        ThingUID thingUIDObject = new ThingUID(thingUID);

        Thing thing = thingRegistry.get(thingUIDObject);
        if (null == thing) {
            throw new NotFoundException();
        }

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                localeService.getLocale(language));
        return Response.ok().entity(thingStatusInfo).build();
    }

    @PUT
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{thingUID}/enable")
    @ApiOperation(value = "Sets the thing enabled status.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Thing not found.") })
    public EnrichedThingDTO setEnabled(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @PathParam("thingUID") @ApiParam(value = "thing") String thingUID,
            @ApiParam(value = "enabled") String enabled) throws IOException {
        final Locale locale = localeService.getLocale(language);

        ThingUID thingUIDObject = new ThingUID(thingUID);

        Thing thing = thingRegistry.get(thingUIDObject);
        if (null == thing) {
            throw new NotFoundException();
        }

        thingManager.setEnabled(thingUIDObject, Boolean.valueOf(enabled));

        return convertToEnrichedThingDTO(thing, locale);
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{thingUID}/config/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets thing's config status.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Thing not found.") })
    public Collection<ConfigStatusMessage> getConfigStatus(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @PathParam("thingUID") @ApiParam(value = "thing") String thingUID) throws IOException {
        ThingUID thingUIDObject = new ThingUID(thingUID);

        Thing thing = thingRegistry.get(thingUIDObject);
        if (null == thing) {
            throw new NotFoundException();
        }

        ConfigStatusInfo info = configStatusService.getConfigStatus(thingUID, localeService.getLocale(language));
        if (info != null) {
            return info.getConfigStatusMessages();
        } else {
            return Collections.emptyList();
        }
    }

    @PUT
    @Path("/{thingUID}/firmware/{firmwareVersion}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update thing firmware.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Firmware update preconditions not satisfied."),
            @ApiResponse(code = 404, message = "Thing not found.") })
    public Response updateFirmware(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("thingUID") @ApiParam(value = "thing") String thingUID,
            @PathParam("firmwareVersion") @ApiParam(value = "version") String firmwareVersion) throws IOException {
        Thing thing = thingRegistry.get(new ThingUID(thingUID));
        if (thing == null) {
            throw new NotFoundException();
        }

        if (StringUtils.isEmpty(firmwareVersion)) {
            throw new BadRequestException("Firmware version is empty");
        }

        ThingUID uid = thing.getUID();
        try {
            firmwareUpdateService.updateFirmware(uid, firmwareVersion, localeService.getLocale(language));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BadRequestException("Firmware update preconditions not satisfied.");
        }

        return Response.status(Status.OK).build();
    }

    @GET
    @Path("/{thingUID}/firmware/status")
    @ApiOperation(value = "Gets thing's firmware status.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No firmware status provided by this Thing.") })
    public Response getFirmwareStatus(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @PathParam("thingUID") @ApiParam(value = "thing") String thingUID) throws IOException {
        ThingUID thingUIDObject = new ThingUID(thingUID);
        FirmwareStatusDTO firmwareStatusDto = getThingFirmwareStatusInfo(thingUIDObject);
        if (firmwareStatusDto == null) {
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.ok(firmwareStatusDto, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/{thingUID}/firmwares")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available firmwares for provided thing UID", response = StrippedThingTypeDTO.class, responseContainer = "Set")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No firmwares found.") })
    public Stream<?> getFirmwares(@PathParam("thingUID") @ApiParam(value = "thingUID") String thingUID,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = HttpHeaders.ACCEPT_LANGUAGE) String language) {
        ThingUID aThingUID = new ThingUID(thingUID);
        Thing thing = thingRegistry.get(aThingUID);
        if (null == thing) {
            throw new NotFoundException();
        }
        final Locale locale = localeService.getLocale(language);
        return firmwareRegistry.getFirmwares(thing, locale).stream().map(this::convertToFirmwareDTO);
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

    private EnrichedThingDTO convertToEnrichedThingDTO(Thing thing, Locale locale) {
        return EnrichedThingDTOMapper.map(thing,
                thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, locale),
                getThingFirmwareStatusInfo(thing.getUID()), getLinkedItemsMap(thing),
                managedThingProvider.get(thing.getUID()) != null);
    }

    private Map<String, Set<String>> getLinkedItemsMap(Thing thing) {
        Map<String, Set<String>> linkedItemsMap = new HashMap<>();
        for (Channel channel : thing.getChannels()) {
            Set<String> linkedItems = itemChannelLinkRegistry.getLinkedItemNames(channel.getUID());
            linkedItemsMap.put(channel.getUID().getId(), linkedItems);
        }
        return linkedItemsMap;
    }

    public static void updateConfiguration(Thing thing, Configuration configuration) {
        for (String parameterName : configuration.keySet()) {
            thing.getConfiguration().put(parameterName, configuration.get(parameterName));
        }
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID) {
        if (properties == null || properties.isEmpty() || thingUID == null) {
            return properties;
        }

        ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);
        if (thingType.getConfigDescriptionURI() != null) {
            ConfigDescription typeConfigDesc = configDescRegistry
                    .getConfigDescription(thingType.getConfigDescriptionURI());
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }

        final URI uriString;
        try {
            uriString = new URI("thing:" + thingUID);
        } catch (URISyntaxException e) {
            throw new BadRequestException(e);
        }

        ConfigDescription thingConfigDesc = configDescRegistry.getConfigDescription(uriString);
        if (thingConfigDesc != null) {
            configDescriptions.add(thingConfigDesc);
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            ChannelTypeUID channelTypeUID, ChannelUID channelUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);
        if (channelType.getConfigDescriptionURI() != null) {
            ConfigDescription typeConfigDesc = configDescRegistry
                    .getConfigDescription(channelType.getConfigDescriptionURI());
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }

        final URI uriString;
        try {
            uriString = new URI("channel:" + channelUID);
        } catch (URISyntaxException e) {
            throw new BadRequestException(e);
        }

        ConfigDescription channelConfigDesc = configDescRegistry.getConfigDescription(uriString);
        if (channelConfigDesc != null) {
            configDescriptions.add(channelConfigDesc);
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private void normalizeChannels(ThingDTO thingBean, @Nullable ThingUID thingUID) {
        if (thingBean.channels != null && thingUID != null) {
            thingBean.channels.stream()
                    .map(channelBean -> channelBean.configuration = normalizeConfiguration(channelBean.configuration,
                            new ChannelTypeUID(channelBean.channelTypeUID), new ChannelUID(thingUID, channelBean.id)));
        }
    }
}
