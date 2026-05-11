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
package org.openhab.core.automation.rest.internal;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.dto.RuleTemplateDTO;
import org.openhab.core.automation.dto.RuleTemplateDTOMapper;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * This class acts as a REST resource for templates and is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JakartarsResource
@JakartarsName(TemplateResource.PATH_TEMPLATES)
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TemplateResource.PATH_TEMPLATES)
@Tag(name = TemplateResource.PATH_TEMPLATES)
@NonNullByDefault
public class TemplateResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_TEMPLATES = "templates";

    private final LocaleService localeService;
    private final TemplateRegistry<@NonNull RuleTemplate> templateRegistry;

    @Activate
    public TemplateResource( //
            final @Reference LocaleService localeService,
            final @Reference TemplateRegistry<@NonNull RuleTemplate> templateRegistry) {
        this.localeService = localeService;
        this.templateRegistry = templateRegistry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTemplates", summary = "Get all available templates.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Template.class)))) })
    public Response getAll(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        Collection<RuleTemplateDTO> result = templateRegistry.getAll(locale).stream()
                .map(template -> RuleTemplateDTOMapper.map(template)).toList();
        return Response.ok(result).build();
    }

    @GET
    @Path("/{templateUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTemplateById", summary = "Gets a template corresponding to the given UID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Template.class))),
            @ApiResponse(responseCode = "404", description = "Template corresponding to the given UID does not found.") })
    public Response getByUID(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("templateUID") @Parameter(description = "templateUID") String templateUID) {
        Locale locale = localeService.getLocale(language);
        RuleTemplate template = templateRegistry.get(templateUID, locale);
        if (template != null) {
            return Response.ok(RuleTemplateDTOMapper.map(template)).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
