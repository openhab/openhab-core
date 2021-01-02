/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.openhab.core.automation.RulePredicates.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import org.openhab.core.auth.Role;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.ManagedRuleProvider;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.dto.ActionDTO;
import org.openhab.core.automation.dto.ActionDTOMapper;
import org.openhab.core.automation.dto.ConditionDTO;
import org.openhab.core.automation.dto.ConditionDTOMapper;
import org.openhab.core.automation.dto.ModuleDTO;
import org.openhab.core.automation.dto.RuleDTO;
import org.openhab.core.automation.dto.RuleDTOMapper;
import org.openhab.core.automation.dto.TriggerDTO;
import org.openhab.core.automation.dto.TriggerDTOMapper;
import org.openhab.core.automation.rest.internal.dto.EnrichedRuleDTO;
import org.openhab.core.automation.rest.internal.dto.EnrichedRuleDTOMapper;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.rest.DTOMapper;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for rules.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Use DTOs
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(RuleResource.PATH_RULES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(RuleResource.PATH_RULES)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = RuleResource.PATH_RULES)
@NonNullByDefault
public class RuleResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_RULES = "rules";

    private final Logger logger = LoggerFactory.getLogger(RuleResource.class);

    private final DTOMapper dtoMapper;
    private final RuleManager ruleManager;
    private final RuleRegistry ruleRegistry;
    private final ManagedRuleProvider managedRuleProvider;

    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    @Activate
    public RuleResource( //
            final @Reference DTOMapper dtoMapper, //
            final @Reference RuleManager ruleManager, //
            final @Reference RuleRegistry ruleRegistry, //
            final @Reference ManagedRuleProvider managedRuleProvider) {
        this.dtoMapper = dtoMapper;
        this.ruleManager = ruleManager;
        this.ruleRegistry = ruleRegistry;
        this.managedRuleProvider = managedRuleProvider;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRules", summary = "Get available rules, optionally filtered by tags and/or prefix.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnrichedRuleDTO.class)))) })
    public Response get(@QueryParam("prefix") final @Nullable String prefix,
            @QueryParam("tags") final @Nullable List<String> tags,
            @QueryParam("summary") @Parameter(description = "summary fields only") @Nullable Boolean summary) {
        // match all
        Predicate<Rule> p = r -> true;

        // prefix parameter has been used
        if (prefix != null) {
            // works also for null prefix
            // (empty prefix used if searching for rules without prefix)
            p = p.and(hasPrefix(prefix));
        }

        // if tags is null or empty list returns all rules
        p = p.and(hasAllTags(tags));

        Stream<EnrichedRuleDTO> rules = ruleRegistry.stream().filter(p) // filter according to Predicates
                .map(rule -> EnrichedRuleDTOMapper.map(rule, ruleManager, managedRuleProvider)); // map matching rules
        if (summary != null && summary == true) {
            rules = dtoMapper.limitToFields(rules, "uid,templateUID,name,visibility,description,status,tags,editable");
        }

        return Response.ok(new Stream2JSONInputStream(rules)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createRule", summary = "Creates a rule.", responses = {
            @ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = "Location", description = "Newly created Rule")),
            @ApiResponse(responseCode = "409", description = "Creation of the rule is refused. Rule with the same UID already exists."),
            @ApiResponse(responseCode = "400", description = "Creation of the rule is refused. Missing required parameter.") })
    public Response create(@Parameter(description = "rule data", required = true) RuleDTO rule) throws IOException {
        try {
            final Rule newRule = ruleRegistry.add(RuleDTOMapper.map(rule));
            return Response.status(Status.CREATED)
                    .header("Location", "rules/" + URLEncoder.encode(newRule.getUID(), StandardCharsets.UTF_8)).build();
        } catch (IllegalArgumentException e) {
            String errMessage = "Creation of the rule is refused: " + e.getMessage();
            logger.warn("{}", errMessage);
            return JSONResponse.createErrorResponse(Status.CONFLICT, errMessage);
        } catch (RuntimeException e) {
            String errMessage = "Creation of the rule is refused: " + e.getMessage();
            logger.warn("{}", errMessage);
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, errMessage);
        }
    }

    @GET
    @Path("/{ruleUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleById", summary = "Gets the rule corresponding to the given UID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedRuleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rule not found") })
    public Response getByUID(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return Response.ok(EnrichedRuleDTOMapper.map(rule, ruleManager, managedRuleProvider)).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{ruleUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "deleteRule", summary = "Removes an existing rule corresponding to the given UID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response remove(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID) {
        Rule removedRule = ruleRegistry.remove(ruleUID);
        if (removedRule == null) {
            logger.info("Received HTTP DELETE request at '{}' for the unknown rule '{}'.", uriInfo.getPath(), ruleUID);
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @PUT
    @Path("/{ruleUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateRule", summary = "Updates an existing rule corresponding to the given UID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response update(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @Parameter(description = "rule data", required = true) RuleDTO rule) throws IOException {
        rule.uid = ruleUID;
        final Rule oldRule = ruleRegistry.update(RuleDTOMapper.map(rule));
        if (oldRule == null) {
            logger.info("Received HTTP PUT request for update at '{}' for the unknown rule '{}'.", uriInfo.getPath(),
                    ruleUID);
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/{ruleUID}/config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleConfiguration", summary = "Gets the rule configuration values.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response getConfiguration(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID)
            throws IOException {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            logger.info("Received HTTP GET request for config at '{}' for the unknown rule '{}'.", uriInfo.getPath(),
                    ruleUID);
            return Response.status(Status.NOT_FOUND).build();
        } else {
            return Response.ok(rule.getConfiguration().getProperties()).build();
        }
    }

    @PUT
    @Path("/{ruleUID}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateRuleConfiguration", summary = "Sets the rule configuration values.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response updateConfiguration(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @Parameter(description = "config") Map<String, @Nullable Object> configurationParameters)
            throws IOException {
        Map<String, @Nullable Object> config = ConfigUtil.normalizeTypes(configurationParameters);
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            logger.info("Received HTTP PUT request for update config at '{}' for the unknown rule '{}'.",
                    uriInfo.getPath(), ruleUID);
            return Response.status(Status.NOT_FOUND).build();
        } else {
            rule = RuleBuilder.create(rule).withConfiguration(new Configuration(config)).build();
            ruleRegistry.update(rule);
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        }
    }

    @POST
    @Path("/{ruleUID}/enable")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "enableRule", summary = "Sets the rule enabled status.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response enableRule(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @Parameter(description = "enable", required = true) String enabled) throws IOException {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            logger.info("Received HTTP PUT request for set enabled at '{}' for the unknown rule '{}'.",
                    uriInfo.getPath(), ruleUID);
            return Response.status(Status.NOT_FOUND).build();
        } else {
            ruleManager.setEnabled(ruleUID, !"false".equalsIgnoreCase(enabled));
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        }
    }

    @POST
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{ruleUID}/runnow")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "runRuleNow", summary = "Executes actions of the rule.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response runNow(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID)
            throws IOException {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            logger.info("Received HTTP PUT request for run now at '{}' for the unknown rule '{}'.", uriInfo.getPath(),
                    ruleUID);
            return Response.status(Status.NOT_FOUND).build();
        } else {
            ruleManager.runNow(ruleUID);
            return Response.ok().build();
        }
    }

    @GET
    @Path("/{ruleUID}/triggers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleTriggers", summary = "Gets the rule triggers.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TriggerDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response getTriggers(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return Response.ok(TriggerDTOMapper.map(rule.getTriggers())).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{ruleUID}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleConditions", summary = "Gets the rule conditions.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConditionDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response getConditions(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return Response.ok(ConditionDTOMapper.map(rule.getConditions())).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{ruleUID}/actions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleActions", summary = "Gets the rule actions.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ActionDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found.") })
    public Response getActions(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return Response.ok(ActionDTOMapper.map(rule.getActions())).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{ruleUID}/{moduleCategory}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleModuleById", summary = "Gets the rule's module corresponding to the given Category and ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ModuleDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    public Response getModuleById(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @PathParam("moduleCategory") @Parameter(description = "moduleCategory") String moduleCategory,
            @PathParam("id") @Parameter(description = "id") String id) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            final ModuleDTO dto = getModuleDTO(rule, moduleCategory, id);
            if (dto != null) {
                return Response.ok(dto).build();
            }
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{ruleUID}/{moduleCategory}/{id}/config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRuleModuleConfig", summary = "Gets the module's configuration.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    public Response getModuleConfig(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @PathParam("moduleCategory") @Parameter(description = "moduleCategory") String moduleCategory,
            @PathParam("id") @Parameter(description = "id") String id) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            Module module = getModule(rule, moduleCategory, id);
            if (module != null) {
                return Response.ok(module.getConfiguration().getProperties()).build();
            }
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{ruleUID}/{moduleCategory}/{id}/config/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "getRuleModuleConfigParameter", summary = "Gets the module's configuration parameter.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    public Response getModuleConfigParam(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @PathParam("moduleCategory") @Parameter(description = "moduleCategory") String moduleCategory,
            @PathParam("id") @Parameter(description = "id") String id,
            @PathParam("param") @Parameter(description = "param") String param) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            Module module = getModule(rule, moduleCategory, id);
            if (module != null) {
                return Response.ok(module.getConfiguration().getProperties().get(param)).build();
            }
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @PUT
    @Path("/{ruleUID}/{moduleCategory}/{id}/config/{param}")
    @Operation(operationId = "setRuleModuleConfigParameter", summary = "Sets the module's configuration parameter value.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setModuleConfigParam(@PathParam("ruleUID") @Parameter(description = "ruleUID") String ruleUID,
            @PathParam("moduleCategory") @Parameter(description = "moduleCategory") String moduleCategory,
            @PathParam("id") @Parameter(description = "id") String id,
            @PathParam("param") @Parameter(description = "param") String param,
            @Parameter(description = "value", required = true) String value) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            Module module = getModule(rule, moduleCategory, id);
            if (module != null) {
                Configuration configuration = module.getConfiguration();
                configuration.put(param, ConfigUtil.normalizeType(value));
                module = ModuleBuilder.create(module).withConfiguration(configuration).build();
                ruleRegistry.update(rule);
                return Response.ok(null, MediaType.TEXT_PLAIN).build();
            }
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    protected @Nullable <T extends Module> T getModuleById(final @Nullable Collection<T> coll, final String id) {
        if (coll == null) {
            return null;
        }
        for (final T module : coll) {
            if (module.getId().equals(id)) {
                return module;
            }
        }
        return null;
    }

    protected @Nullable Trigger getTrigger(Rule rule, String id) {
        return getModuleById(rule.getTriggers(), id);
    }

    protected @Nullable Condition getCondition(Rule rule, String id) {
        return getModuleById(rule.getConditions(), id);
    }

    protected @Nullable Action getAction(Rule rule, String id) {
        return getModuleById(rule.getActions(), id);
    }

    protected @Nullable Module getModule(Rule rule, String moduleCategory, String id) {
        if ("triggers".equals(moduleCategory)) {
            return getTrigger(rule, id);
        } else if ("conditions".equals(moduleCategory)) {
            return getCondition(rule, id);
        } else if ("actions".equals(moduleCategory)) {
            return getAction(rule, id);
        } else {
            return null;
        }
    }

    protected @Nullable ModuleDTO getModuleDTO(Rule rule, String moduleCategory, String id) {
        if ("triggers".equals(moduleCategory)) {
            final Trigger trigger = getTrigger(rule, id);
            return trigger == null ? null : TriggerDTOMapper.map(trigger);
        } else if ("conditions".equals(moduleCategory)) {
            final Condition condition = getCondition(rule, id);
            return condition == null ? null : ConditionDTOMapper.map(condition);
        } else if ("actions".equals(moduleCategory)) {
            final Action action = getAction(rule, id);
            return action == null ? null : ActionDTOMapper.map(action);
        } else {
            return null;
        }
    }
}
