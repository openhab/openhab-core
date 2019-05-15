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
package org.openhab.core.automation.rest.internal;

import static org.openhab.core.automation.RulePredicates.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigUtil;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.io.rest.RESTService;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

/**
 * This class acts as a REST resource for rules and is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Use DTOs
 */
@Path("rules")
@Api("rules")
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class RuleResource {
    @Reference
    protected @NonNullByDefault({}) RuleRegistry ruleRegistry;
    @Reference
    protected @NonNullByDefault({}) RuleManager ruleManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get available rules, optionally filtered by tags and/or prefix.", response = EnrichedRuleDTO.class, responseContainer = "Collection")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = EnrichedRuleDTO.class, responseContainer = "Collection") })
    public Stream<EnrichedRuleDTO> get(@QueryParam("prefix") final @Nullable String prefix,
            @QueryParam("tags") final List<String> tags) {
        // match all
        Predicate<Rule> p = r -> true;

        // prefix parameter has been used
        if (null != prefix) {
            // works also for null prefix
            // (empty prefix used if searching for rules without prefix)
            p = p.and(hasPrefix(prefix));
        }

        // if tags is null or empty list returns all rules
        p = p.and(hasAllTags(tags));

        return ruleRegistry.stream().filter(p) // filter according to Predicates
                .map(rule -> EnrichedRuleDTOMapper.map(rule, ruleManager)); // map matching rules
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a rule.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created", responseHeaders = @ResponseHeader(name = "Location", description = "Newly created Rule", response = String.class)),
            @ApiResponse(code = 409, message = "Creation of the rule is refused. Rule with the same UID already exists."),
            @ApiResponse(code = 400, message = "Creation of the rule is refused. Missing required parameter.") })
    public void create(@Context HttpServletResponse response,
            @ApiParam(value = "rule data", required = true) RuleDTO rule) throws IOException {
        try {
            final Rule newRule = ruleRegistry.add(RuleDTOMapper.map(rule));
            response.setStatus(Response.Status.CREATED.getStatusCode());
            response.setHeader("Location", "rules/" + URLEncoder.encode(newRule.getUID(), "UTF-8"));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Creation of the rule is refused", e, 409);
        } catch (RuntimeException e) {
            throw new BadRequestException("Creation of the rule is refused", e);
        }
    }

    @GET
    @Path("/{ruleUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the rule corresponding to the given UID.", response = EnrichedRuleDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = EnrichedRuleDTO.class),
            @ApiResponse(code = 404, message = "Rule not found") })
    public EnrichedRuleDTO getByUID(
            @PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return EnrichedRuleDTOMapper.map(rule, ruleManager);
        } else {
            throw new NotFoundException();
        }
    }

    @DELETE
    @Path("/{ruleUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Removes an existing rule corresponding to the given UID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public void remove(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID) {
        Rule removedRule = ruleRegistry.remove(ruleUID);
        if (removedRule == null) {
            throw new NotFoundException();
        }
    }

    @PUT
    @Path("/{ruleUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates an existing rule corresponding to the given UID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public void update(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @ApiParam(value = "rule data", required = true) RuleDTO rule) throws IOException {
        rule.uid = ruleUID;
        final Rule oldRule = ruleRegistry.update(RuleDTOMapper.map(rule));
        if (oldRule == null) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{ruleUID}/config")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the rule configuration values.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public Map<String, Object> getConfiguration(
            @PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID) throws IOException {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            throw new NotFoundException();
        } else {
            return rule.getConfiguration().getProperties();
        }
    }

    @PUT
    @Path("/{ruleUID}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Sets the rule configuration values.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public void updateConfiguration(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @ApiParam(value = "config") Map<String, Object> configurationParameters) throws IOException {
        Map<String, Object> config = ConfigUtil.normalizeTypes(configurationParameters);
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            throw new NotFoundException();
        } else {
            rule = RuleBuilder.create(rule).withConfiguration(new Configuration(config)).build();
            ruleRegistry.update(rule);
        }
    }

    @POST
    @Path("/{ruleUID}/enable")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sets the rule enabled status.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public void enableRule(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @ApiParam(value = "enable", required = true) String enabled) throws IOException {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            throw new NotFoundException();
        } else {
            ruleManager.setEnabled(ruleUID, !"false".equalsIgnoreCase(enabled));
        }
    }

    @POST
    @Path("/{ruleUID}/runnow")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Executes actions of the rule.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public void runNow(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID)
            throws IOException {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule == null) {
            throw new NotFoundException();
        } else {
            ruleManager.runNow(ruleUID);
        }
    }

    @GET
    @Path("/{ruleUID}/triggers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the rule triggers.", response = TriggerDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = TriggerDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public List<TriggerDTO> getTriggers(
            @PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return TriggerDTOMapper.map(rule.getTriggers());
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{ruleUID}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the rule conditions.", response = ConditionDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ConditionDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public Response getConditions(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return Response.ok(ConditionDTOMapper.map(rule.getConditions())).build();
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{ruleUID}/actions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the rule actions.", response = ActionDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ActionDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found.") })
    public Response getActions(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            return Response.ok(ActionDTOMapper.map(rule.getActions())).build();
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{ruleUID}/{moduleCategory}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the rule's module corresponding to the given Category and ID.", response = ModuleDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ModuleDTO.class),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    public ModuleDTO getModuleById(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @PathParam("moduleCategory") @ApiParam(value = "moduleCategory", required = true) String moduleCategory,
            @PathParam("id") @ApiParam(value = "id", required = true) String id) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            final ModuleDTO dto = getModuleDTO(rule, moduleCategory, id);
            if (dto != null) {
                return dto;
            }
        }
        throw new NotFoundException();
    }

    @GET
    @Path("/{ruleUID}/{moduleCategory}/{id}/config")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the module's configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    public Map<String, Object> getModuleConfig(
            @PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @PathParam("moduleCategory") @ApiParam(value = "moduleCategory", required = true) String moduleCategory,
            @PathParam("id") @ApiParam(value = "id", required = true) String id) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            Module module = getModule(rule, moduleCategory, id);
            if (module != null) {
                return module.getConfiguration().getProperties();
            }
        }
        throw new NotFoundException();
    }

    @GET
    @Path("/{ruleUID}/{moduleCategory}/{id}/config/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Gets the module's configuration parameter.", response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    public Object getModuleConfigParam(
            @PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @PathParam("moduleCategory") @ApiParam(value = "moduleCategory", required = true) String moduleCategory,
            @PathParam("id") @ApiParam(value = "id", required = true) String id,
            @PathParam("param") @ApiParam(value = "param", required = true) String param) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            Module module = getModule(rule, moduleCategory, id);
            if (module != null) {
                return module.getConfiguration().getProperties().get(param);
            }
        }
        throw new NotFoundException();
    }

    @PUT
    @Path("/{ruleUID}/{moduleCategory}/{id}/config/{param}")
    @ApiOperation(value = "Sets the module's configuration parameter value.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Rule corresponding to the given UID does not found or does not have a module with such Category and ID.") })
    @Consumes(MediaType.TEXT_PLAIN)
    public void setModuleConfigParam(@PathParam("ruleUID") @ApiParam(value = "ruleUID", required = true) String ruleUID,
            @PathParam("moduleCategory") @ApiParam(value = "moduleCategory", required = true) String moduleCategory,
            @PathParam("id") @ApiParam(value = "id", required = true) String id,
            @PathParam("param") @ApiParam(value = "param", required = true) String param,
            @ApiParam(value = "value", required = true) String value) {
        Rule rule = ruleRegistry.get(ruleUID);
        if (rule != null) {
            Module module = getModule(rule, moduleCategory, id);
            if (module != null) {
                Configuration configuration = module.getConfiguration();
                configuration.put(param, ConfigUtil.normalizeType(value));
                module = ModuleBuilder.create(module).withConfiguration(configuration).build();
                ruleRegistry.update(rule);
            }
        }
        throw new NotFoundException();
    }

    protected @Nullable <T extends Module> T getModuleById(@Nullable final Collection<T> coll, final String id) {
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
        if (moduleCategory.equals("triggers")) {
            return getTrigger(rule, id);
        } else if (moduleCategory.equals("conditions")) {
            return getCondition(rule, id);
        } else if (moduleCategory.equals("actions")) {
            return getAction(rule, id);
        } else {
            return null;
        }
    }

    protected @Nullable ModuleDTO getModuleDTO(Rule rule, String moduleCategory, String id) {
        if (moduleCategory.equals("triggers")) {
            final Trigger trigger = getTrigger(rule, id);
            return trigger == null ? null : TriggerDTOMapper.map(trigger);
        } else if (moduleCategory.equals("conditions")) {
            final Condition condition = getCondition(rule, id);
            return condition == null ? null : ConditionDTOMapper.map(condition);
        } else if (moduleCategory.equals("actions")) {
            final Action action = getAction(rule, id);
            return action == null ? null : ActionDTOMapper.map(action);
        } else {
            return null;
        }
    }
}
