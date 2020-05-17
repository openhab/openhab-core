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
package org.openhab.core.automation.rest.internal;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for templates and is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsResource
@JaxrsName(TemplateResource.PATH_TEMPLATES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TemplateResource.PATH_TEMPLATES)
@Api(TemplateResource.PATH_TEMPLATES)
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
    @ApiOperation(value = "Get all available templates.", response = Template.class, responseContainer = "Collection")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Template.class, responseContainer = "Collection") })
    public Response getAll(@HeaderParam("Accept-Language") @ApiParam(value = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        Collection<RuleTemplateDTO> result = templateRegistry.getAll(locale).stream()
                .map(template -> RuleTemplateDTOMapper.map(template)).collect(Collectors.toList());
        return Response.ok(result).build();
    }

    @GET
    @Path("/{templateUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets a template corresponding to the given UID.", response = Template.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Template.class),
            @ApiResponse(code = 404, message = "Template corresponding to the given UID does not found.") })
    public Response getByUID(@HeaderParam("Accept-Language") @ApiParam(value = "language") @Nullable String language,
            @PathParam("templateUID") @ApiParam(value = "templateUID") String templateUID) {
        Locale locale = localeService.getLocale(language);
        RuleTemplate template = templateRegistry.get(templateUID, locale);
        if (template != null) {
            return Response.ok(RuleTemplateDTOMapper.map(template)).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
