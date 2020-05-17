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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.dto.ActionTypeDTOMapper;
import org.openhab.core.automation.dto.ConditionTypeDTOMapper;
import org.openhab.core.automation.dto.ModuleTypeDTO;
import org.openhab.core.automation.dto.TriggerTypeDTOMapper;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.type.TriggerType;
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
 * This class acts as a REST resource for module types and is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Use DTOs
 * @author Ana Dimova - extends Module type DTOs with composites
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsResource
@JaxrsName(ModuleTypeResource.PATH_MODULE_TYPES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ModuleTypeResource.PATH_MODULE_TYPES)
@Api(ModuleTypeResource.PATH_MODULE_TYPES)
@NonNullByDefault
public class ModuleTypeResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_MODULE_TYPES = "module-types";

    private final LocaleService localeService;
    private final ModuleTypeRegistry moduleTypeRegistry;

    @Activate
    public ModuleTypeResource( //
            final @Reference LocaleService localeService, //
            final @Reference ModuleTypeRegistry moduleTypeRegistry) {
        this.localeService = localeService;
        this.moduleTypeRegistry = moduleTypeRegistry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available module types.", response = ModuleTypeDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ModuleTypeDTO.class, responseContainer = "List") })
    public Response getAll(@HeaderParam("Accept-Language") @ApiParam(value = "language") @Nullable String language,
            @QueryParam("tags") @ApiParam(value = "tags for filtering") @Nullable String tagList,
            @QueryParam("type") @ApiParam(value = "filtering by action, condition or trigger") @Nullable String type) {
        final Locale locale = localeService.getLocale(language);
        final String[] tags = tagList != null ? tagList.split(",") : new String[0];
        final List<ModuleTypeDTO> modules = new ArrayList<>();

        if (type == null || "trigger".equals(type)) {
            modules.addAll(TriggerTypeDTOMapper.map(moduleTypeRegistry.getTriggers(locale, tags)));
        }
        if (type == null || "condition".equals(type)) {
            modules.addAll(ConditionTypeDTOMapper.map(moduleTypeRegistry.getConditions(locale, tags)));
        }
        if (type == null || "action".equals(type)) {
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
    public Response getByUID(@HeaderParam("Accept-Language") @ApiParam(value = "language") @Nullable String language,
            @PathParam("moduleTypeUID") @ApiParam(value = "moduleTypeUID") String moduleTypeUID) {
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
}
