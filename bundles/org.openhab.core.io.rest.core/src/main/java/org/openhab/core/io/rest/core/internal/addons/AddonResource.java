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
package org.openhab.core.io.rest.core.internal.addons;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.auth.Role;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
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

    private final Logger logger = LoggerFactory.getLogger(AddonResource.class);
    private final Set<AddonService> addonServices = new CopyOnWriteArraySet<>();
    private final EventPublisher eventPublisher;
    private final LocaleService localeService;

    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    @Activate
    public AddonResource(final @Reference EventPublisher eventPublisher, final @Reference LocaleService localeService) {
        this.eventPublisher = eventPublisher;
        this.localeService = localeService;
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
    @Operation(summary = "Get all add-ons.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))) })
    public Response getAddon(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        return Response.ok(new Stream2JSONInputStream(getAllAddons(locale))).build();
    }

    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all add-on types.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))) })
    public Response getTypes(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        Stream<AddonType> addonTypeStream = getAllAddonTypes(locale).stream().distinct();
        return Response.ok(new Stream2JSONInputStream(addonTypeStream)).build();
    }

    @GET
    @Path("/{addonId: [a-zA-Z_0-9-:]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get add-on with given ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response getById(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("addonId") @Parameter(description = "addon ID") String addonId) {
        logger.debug("Received HTTP GET request at '{}'.", uriInfo.getPath());
        Locale locale = localeService.getLocale(language);
        AddonService addonService = getAddonService(addonId);
        Addon responseObject = addonService.getAddon(addonId, locale);
        if (responseObject != null) {
            return Response.ok(responseObject).build();
        }

        return Response.status(404).build();
    }

    @POST
    @Path("/{addonId: [a-zA-Z_0-9-:]+}/install")
    @Operation(summary = "Installs the add-on with the given ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response installAddon(final @PathParam("addonId") @Parameter(description = "addon ID") String addonId) {
        ThreadPoolManager.getPool(THREAD_POOL_NAME).submit(() -> {
            try {
                AddonService addonService = getAddonService(addonId);
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
    @Operation(summary = "Installs the add-on from the given URL.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "The given URL is malformed or not valid.") })
    public Response installAddonByURL(
            final @PathParam("url") @Parameter(description = "addon install URL") String url) {
        try {
            URI addonURI = new URI(url);
            String addonId = getAddonId(addonURI);
            installAddon(addonId);
        } catch (URISyntaxException | IllegalArgumentException e) {
            logger.error("Exception while parsing the addon URL '{}': {}", url, e.getMessage());
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "The given URL is malformed or not valid.");
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/{addonId: [a-zA-Z_0-9-:]+}/uninstall")
    @Operation(summary = "Uninstalls the add-on with the given ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response uninstallAddon(final @PathParam("addonId") @Parameter(description = "addon ID") String addonId) {
        ThreadPoolManager.getPool(THREAD_POOL_NAME).submit(() -> {
            try {
                AddonService addonService = getAddonService(addonId);
                addonService.uninstall(addonId);
            } catch (Exception e) {
                logger.error("Exception while uninstalling add-on: {}", e.getMessage());
                postFailureEvent(addonId, e.getMessage());
            }
        });
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    private void postFailureEvent(String addonId, String msg) {
        Event event = AddonEventFactory.createAddonFailureEvent(addonId, msg);
        eventPublisher.post(event);
    }

    private Stream<Addon> getAllAddons(Locale locale) {
        return addonServices.stream().map(s -> s.getAddons(locale)).flatMap(l -> l.stream());
    }

    private Set<AddonType> getAllAddonTypes(Locale locale) {
        final Collator coll = Collator.getInstance(locale);
        coll.setStrength(Collator.PRIMARY);
        Set<AddonType> ret = new TreeSet<>(new Comparator<AddonType>() {
            @Override
            public int compare(AddonType o1, AddonType o2) {
                return coll.compare(o1.getLabel(), o2.getLabel());
            }
        });
        for (AddonService addonService : addonServices) {
            ret.addAll(addonService.getTypes(locale));
        }
        return ret;
    }

    private AddonService getAddonService(final String addonId) {
        for (AddonService addonService : addonServices) {
            for (Addon addon : addonService.getAddons(Locale.getDefault())) {
                if (addonId.equals(addon.getId())) {
                    return addonService;
                }
            }
        }
        throw new IllegalArgumentException("No add-on service registered for " + addonId);
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
}
