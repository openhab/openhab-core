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
package org.openhab.core.ui.icon.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.ui.icon.IconProvider;
import org.openhab.core.ui.icon.IconSet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.jakartars.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;

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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * This is a REST resource that provides information about available icon sets.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JakartarsResource
@JakartarsName(IconSetResource.PATH_ICONSETS)
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(IconSetResource.PATH_ICONSETS)
@Tag(name = IconSetResource.PATH_ICONSETS)
@NonNullByDefault
public class IconSetResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_ICONSETS = "iconsets";

    private final List<IconProvider> iconProviders = new ArrayList<>(5);

    private final LocaleService localeService;

    @Activate
    public IconSetResource(final @Reference LocaleService localeService) {
        this.localeService = localeService;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addIconProvider(IconProvider iconProvider) {
        this.iconProviders.add(iconProvider);
    }

    protected void removeIconProvider(IconProvider iconProvider) {
        this.iconProviders.remove(iconProvider);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getIconSets", summary = "Gets all icon sets.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IconSet.class)))) })
    public Response getAll(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);

        List<IconSet> iconSets = new ArrayList<>(iconProviders.size());
        for (IconProvider iconProvider : iconProviders) {
            iconSets.addAll(iconProvider.getIconSets(locale));
        }
        return Response.ok(iconSets).build();
    }
}
