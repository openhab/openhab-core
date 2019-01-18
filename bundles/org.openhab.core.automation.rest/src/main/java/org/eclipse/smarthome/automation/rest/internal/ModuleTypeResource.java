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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.smarthome.automation.core.dto.ActionTypeDTOMapper;
import org.eclipse.smarthome.automation.core.dto.ConditionTypeDTOMapper;
import org.eclipse.smarthome.automation.core.dto.TriggerTypeDTOMapper;
import org.eclipse.smarthome.automation.dto.ModuleTypeDTO;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.CompositeActionType;
import org.eclipse.smarthome.automation.type.CompositeConditionType;
import org.eclipse.smarthome.automation.type.CompositeTriggerType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.automation.type.TriggerType;
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
 * This class acts as a REST resource for module types and is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Use DTOs
 * @author Ana Dimova - extends Module type DTOs with composites
 */
@Path("module-types")
@Api("module-types")
@Component
public class ModuleTypeResource implements RESTResource {

    private ModuleTypeRegistry moduleTypeRegistry;
    private LocaleService localeService;

    @Context
    private UriInfo uriInfo;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setModuleTypeRegistry(ModuleTypeRegistry moduleTypeRegistry) {
        this.moduleTypeRegistry = moduleTypeRegistry;
    }

    protected void unsetModuleTypeRegistry(ModuleTypeRegistry moduleTypeRegistry) {
        this.moduleTypeRegistry = null;
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
    @ApiOperation(value = "Get all available module types.", response = ModuleTypeDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ModuleTypeDTO.class, responseContainer = "List") })
    public Response getAll(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language,
            @QueryParam("tags") @ApiParam(value = "tags for filtering", required = false) String tagList,
            @QueryParam("type") @ApiParam(value = "filtering by action, condition or trigger", required = false) String type) {
        final Locale locale = localeService.getLocale(language);
        final String[] tags = tagList != null ? tagList.split(",") : null;
        final List<ModuleTypeDTO> modules = new ArrayList<ModuleTypeDTO>();

        if (type == null || type.equals("trigger")) {
            modules.addAll(TriggerTypeDTOMapper.map(moduleTypeRegistry.getTriggers(locale, tags)));
        }
        if (type == null || type.equals("condition")) {
            modules.addAll(ConditionTypeDTOMapper.map(moduleTypeRegistry.getConditions(locale, tags)));
        }
        if (type == null || type.equals("action")) {
            modules.addAll(ActionTypeDTOMapper.map(moduleTypeRegistry.getActions(locale, tags)));
        }
        return Response.ok(modules).build();
    }

    @GET
    @Path("/{moduleTypeUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets a module type corresponding to the given UID.", response = ModuleTypeDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ModuleTypeDTO.class),
            @ApiResponse(code = 404, message = "Module Type corresponding to the given UID does not found.") })
    public Response getByUID(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language,
            @PathParam("moduleTypeUID") @ApiParam(value = "moduleTypeUID", required = true) String moduleTypeUID) {
        Locale locale = localeService.getLocale(language);
        final ModuleType moduleType = moduleTypeRegistry.get(moduleTypeUID, locale);
        if (moduleType != null) {
            return Response.ok(getModuleTypeDTO(moduleType)).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    private ModuleTypeDTO getModuleTypeDTO(final ModuleType moduleType) {
        if (moduleType instanceof ActionType) {
            if (moduleType instanceof CompositeActionType) {
                return ActionTypeDTOMapper.map((CompositeActionType) moduleType);
            }
            return ActionTypeDTOMapper.map((ActionType) moduleType);
        } else if (moduleType instanceof ConditionType) {
            if (moduleType instanceof CompositeConditionType) {
                return ConditionTypeDTOMapper.map((CompositeConditionType) moduleType);
            }
            return ConditionTypeDTOMapper.map((ConditionType) moduleType);
        } else if (moduleType instanceof TriggerType) {
            if (moduleType instanceof CompositeTriggerType) {
                return TriggerTypeDTOMapper.map((CompositeTriggerType) moduleType);
            }
            return TriggerTypeDTOMapper.map((TriggerType) moduleType);
        } else {
            throw new IllegalArgumentException(
                    String.format("Cannot handle given module type class (%s)", moduleType.getClass()));
        }
    }

    @Override
    public boolean isSatisfied() {
        return moduleTypeRegistry != null && localeService != null;
    }

}
