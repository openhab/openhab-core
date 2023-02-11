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
package org.openhab.core.io.rest.core.internal.addons;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.auth.Role;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.core.config.ConfigurationService;
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
 * This class acts as a REST resource for add-ons and provides methods to install and uninstall them.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 * @author Yannick Schaus - Add service-related parameters & operations
 */
@Component
@JaxrsResource
@JaxrsName(AddonResource.PATH_ADDONS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(AddonResource.PATH_ADDONS)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = AddonResource.PATH_ADDONS)
@NonNullByDefault
public class AddonResource implements RESTResource {

    private static final String THREAD_POOL_NAME = "addonService";

    public static final String PATH_ADDONS = "addons";

    public static final String DEFAULT_ADDON_SERVICE = "karaf";

    private final Logger logger = LoggerFactory.getLogger(AddonResource.class);
    private final Set<AddonService> addonServices = new CopyOnWriteArraySet<>();
    private final EventPublisher eventPublisher;
    private final LocaleService localeService;
    private final ConfigurationService configurationService;
    private final AddonInfoRegistry addonInfoRegistry;
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    @Activate
    public AddonResource(final @Reference EventPublisher eventPublisher, final @Reference LocaleService localeService,
            final @Reference ConfigurationService configurationService,
            final @Reference AddonInfoRegistry addonInfoRegistry,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry) {
        this.eventPublisher = eventPublisher;
        this.localeService = localeService;
        this.configurationService = configurationService;
        this.addonInfoRegistry = addonInfoRegistry;
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonService(AddonService featureService) {
        this.addonServices.add(featureService);
    }

    protected void removeAddonService(AddonService featureService) {
        this.addonServices.remove(featureService);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAddons", summary = "Get all add-ons.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Addon.class)))),
            @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response getAddon(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        if ("all".equals(serviceId)) {
            return Response.ok(new Stream2JSONInputStream(getAllAddons(locale))).build();
        } else {
            AddonService addonService = (serviceId != null) ? getServiceById(serviceId) : getDefaultService();
            if (addonService == null) {
                return Response.status(HttpStatus.NOT_FOUND_404).build();
            }
            return Response.ok(new Stream2JSONInputStream(addonService.getAddons(locale).stream())).build();
        }
    }

    @GET
    @Path("/services")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAddonTypes", summary = "Get all add-on types.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddonType.class)))) })
    public Response getServices(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        Stream<AddonServiceDTO> addonTypeStream = addonServices.stream().map(s -> convertToAddonServiceDTO(s, locale));
        return Response.ok(new Stream2JSONInputStream(addonTypeStream)).build();
    }

    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAddonServices", summary = "Get add-on services.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddonType.class)))),
            @ApiResponse(responseCode = "404", description = "Service not found") })
    public Response getTypes(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        if (serviceId != null) {
            @Nullable
            AddonService service = getServiceById(serviceId);
            if (service != null) {
                Stream<AddonType> addonTypeStream = getAddonTypesForService(service, locale).stream().distinct();
                return Response.ok(new Stream2JSONInputStream(addonTypeStream)).build();
            } else {
                return Response.status(HttpStatus.NOT_FOUND_404).build();
            }
        } else {
            Stream<AddonType> addonTypeStream = getAllAddonTypes(locale).stream().distinct();
            return Response.ok(new Stream2JSONInputStream(addonTypeStream)).build();
        }
    }

    @GET
    @Path("/{addonId: [a-zA-Z_0-9-:]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAddonById", summary = "Get add-on with given ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Addon.class))),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response getById(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("addonId") @Parameter(description = "addon ID") String addonId,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId) {
        logger.debug("Received HTTP GET request at '{}'.", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        AddonService addonService = (serviceId != null) ? getServiceById(serviceId) : getDefaultService();
        if (addonService == null) {
            return Response.status(HttpStatus.NOT_FOUND_404).build();
        }
        Addon responseObject = addonService.getAddon(addonId, locale);
        if (responseObject != null) {
            return Response.ok(responseObject).build();
        }

        return Response.status(HttpStatus.NOT_FOUND_404).build();
    }

    @POST
    @Path("/{addonId: [a-zA-Z_0-9-:]+}/install")
    @Operation(operationId = "installAddonById", summary = "Installs the add-on with the given ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response installAddon(final @PathParam("addonId") @Parameter(description = "addon ID") String addonId,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId) {
        AddonService addonService = (serviceId != null) ? getServiceById(serviceId) : getDefaultService();
        if (addonService == null) {
            return Response.status(HttpStatus.NOT_FOUND_404).build();
        }
        ThreadPoolManager.getPool(THREAD_POOL_NAME).submit(() -> {
            try {
                addonService.install(addonId);
            } catch (Exception e) {
                logger.error("Exception while installing add-on: {}", e.getMessage());
                postFailureEvent(addonId, e.getMessage());
            }
        });
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/url/{url}/install")
    @Operation(operationId = "installAddonFromURL", summary = "Installs the add-on from the given URL.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "The given URL is malformed or not valid.") })
    public Response installAddonByURL(
            final @PathParam("url") @Parameter(description = "addon install URL") String url) {
        try {
            URI addonURI = new URI(url);
            String addonId = getAddonId(addonURI);
            installAddon(addonId, getAddonServiceForAddonId(addonURI));
        } catch (URISyntaxException | IllegalArgumentException e) {
            logger.error("Exception while parsing the addon URL '{}': {}", url, e.getMessage());
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "The given URL is malformed or not valid.");
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/{addonId: [a-zA-Z_0-9-:]+}/uninstall")
    @Operation(operationId = "uninstallAddon", summary = "Uninstalls the add-on with the given ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response uninstallAddon(final @PathParam("addonId") @Parameter(description = "addon ID") String addonId,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId) {
        AddonService addonService = (serviceId != null) ? getServiceById(serviceId) : getDefaultService();
        if (addonService == null) {
            return Response.status(HttpStatus.NOT_FOUND_404).build();
        }
        ThreadPoolManager.getPool(THREAD_POOL_NAME).submit(() -> {
            try {
                addonService.uninstall(addonId);
            } catch (Exception e) {
                logger.error("Exception while uninstalling add-on: {}", e.getMessage());
                postFailureEvent(addonId, e.getMessage());
            }
        });
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/{addonId: [a-zA-Z_0-9-:]+}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getAddonConfiguration", summary = "Get add-on configuration for given add-on ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Add-on does not exist"),
            @ApiResponse(responseCode = "500", description = "Configuration can not be read due to internal error") })
    public Response getConfiguration(final @PathParam("addonId") @Parameter(description = "addon ID") String addonId,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId) {
        try {
            AddonService addonService = (serviceId != null) ? getServiceById(serviceId) : getDefaultService();
            if (addonService == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            Addon addon = addonService.getAddon(addonId, null);
            if (addon == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            String infoUid = addon.getType() + "-" + addon.getId();
            AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(infoUid);
            if (addonInfo == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            Configuration configuration = configurationService.get(addonInfo.getServiceId());
            return configuration != null ? Response.ok(configuration.getProperties()).build()
                    : Response.ok(Map.of()).build();
        } catch (IOException e) {
            logger.error("Cannot get configuration for service {}: {}", addonId, e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{addonId: [a-zA-Z_0-9-:]+}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "updateAddonConfiguration", summary = "Updates an add-on configuration for given ID and returns the old configuration.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "204", description = "No old configuration"),
            @ApiResponse(responseCode = "404", description = "Add-on does not exist"),
            @ApiResponse(responseCode = "500", description = "Configuration can not be updated due to internal error") })
    public Response updateConfiguration(@PathParam("addonId") @Parameter(description = "Add-on id") String addonId,
            @QueryParam("serviceId") @Parameter(description = "service ID") @Nullable String serviceId,
            @Nullable Map<String, Object> configuration) {
        try {
            AddonService addonService = (serviceId != null) ? getServiceById(serviceId) : getDefaultService();
            if (addonService == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            Addon addon = addonService.getAddon(addonId, null);
            if (addon == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            String infoUid = addon.getType() + "-" + addon.getId();
            AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(infoUid);
            if (addonInfo == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            Configuration oldConfiguration = configurationService.get(addonInfo.getServiceId());
            configurationService.update(addonInfo.getServiceId(),
                    new Configuration(normalizeConfiguration(configuration, addonId)));
            return oldConfiguration != null ? Response.ok(oldConfiguration.getProperties()).build()
                    : Response.noContent().build();
        } catch (IOException ex) {
            logger.error("Cannot update configuration for service {}: {}", addonId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            String addonId) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }
        AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(addonId);

        if (addonInfo == null || addonInfo.getConfigDescriptionURI() == null) {
            return properties;
        }

        String configDescriptionURI = addonInfo.getConfigDescriptionURI();
        if (configDescriptionURI != null) {
            ConfigDescription configDesc = configDescriptionRegistry
                    .getConfigDescription(URI.create(configDescriptionURI));
            if (configDesc != null) {
                return ConfigUtil.normalizeTypes(properties, List.of(configDesc));
            }
        }

        return properties;
    }

    private void postFailureEvent(String addonId, @Nullable String msg) {
        Event event = AddonEventFactory.createAddonFailureEvent(addonId, msg);
        eventPublisher.post(event);
    }

    private @Nullable AddonService getDefaultService() {
        return addonServices.stream().filter(addonService -> DEFAULT_ADDON_SERVICE.equals(addonService.getId()))
                .findFirst().orElse(addonServices.stream().findFirst().orElse(null));
    }

    private Stream<Addon> getAllAddons(Locale locale) {
        return addonServices.stream().map(s -> s.getAddons(locale)).flatMap(Collection::stream);
    }

    private Set<AddonType> getAllAddonTypes(Locale locale) {
        final Collator coll = Collator.getInstance(locale);
        coll.setStrength(Collator.PRIMARY);
        Set<AddonType> ret = new TreeSet<>((o1, o2) -> coll.compare(o1.getLabel(), o2.getLabel()));
        for (AddonService addonService : addonServices) {
            ret.addAll(addonService.getTypes(locale));
        }
        return ret;
    }

    private Set<AddonType> getAddonTypesForService(AddonService addonService, Locale locale) {
        final Collator coll = Collator.getInstance(locale);
        coll.setStrength(Collator.PRIMARY);
        Set<AddonType> ret = new TreeSet<>((o1, o2) -> coll.compare(o1.getLabel(), o2.getLabel()));
        ret.addAll(addonService.getTypes(locale));
        return ret;
    }

    private @Nullable AddonService getServiceById(final String serviceId) {
        for (AddonService addonService : addonServices) {
            if (addonService.getId().equals(serviceId)) {
                return addonService;
            }
        }
        return null;
    }

    private String getAddonId(URI addonURI) {
        for (AddonService addonService : addonServices) {
            String addonId = addonService.getAddonId(addonURI);
            if (addonId != null && !addonId.isBlank()) {
                return addonId;
            }
        }

        throw new IllegalArgumentException("No add-on service registered for URI " + addonURI);
    }

    private String getAddonServiceForAddonId(URI addonURI) {
        for (AddonService addonService : addonServices) {
            String addonId = addonService.getAddonId(addonURI);
            if (addonId != null && !addonId.isBlank()) {
                return addonService.getId();
            }
        }

        throw new IllegalArgumentException("No add-on service registered for URI " + addonURI);
    }

    private AddonServiceDTO convertToAddonServiceDTO(AddonService addonService, Locale locale) {
        return new AddonServiceDTO(addonService.getId(), addonService.getName(),
                getAddonTypesForService(addonService, locale));
    }
}
