/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.rest.internal;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.smarthome.automation.core.dto.RuleTemplateDTOMapper;
import org.eclipse.smarthome.automation.dto.RuleTemplateDTO;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateRegistry;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTResource;
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
 * This class acts as a REST resource for templates and is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Path("templates")
@Api("templates")
@Component
public class TemplateResource implements RESTResource {

    private TemplateRegistry<RuleTemplate> templateRegistry;
    private LocaleService localeService;

    @Context
    private UriInfo uriInfo;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setTemplateRegistry(TemplateRegistry<RuleTemplate> templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    protected void unsetTemplateRegistry(TemplateRegistry<RuleTemplate> templateRegistry) {
        this.templateRegistry = null;
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
    @ApiOperation(value = "Get all available templates.", response = Template.class, responseContainer = "Collection")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Template.class, responseContainer = "Collection") })
    public Response getAll(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language) {
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
    public Response getByUID(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language,
            @PathParam("templateUID") @ApiParam(value = "templateUID", required = true) String templateUID) {
        Locale locale = localeService.getLocale(language);
        RuleTemplate template = templateRegistry.get(templateUID, locale);
        if (template != null) {
            return Response.ok(RuleTemplateDTOMapper.map(template)).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @Override
    public boolean isSatisfied() {
        return templateRegistry != null && localeService != null;
    }
}
