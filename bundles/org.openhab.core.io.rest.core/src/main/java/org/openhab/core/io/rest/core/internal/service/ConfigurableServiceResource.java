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
package org.openhab.core.io.rest.core.internal.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.config.ConfigurationService;
import org.openhab.core.io.rest.core.internal.RESTCoreActivator;
import org.openhab.core.io.rest.core.service.ConfigurableServiceDTO;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * {@link ConfigurableServiceResource} provides access to configurable services. It lists the available services and
 * allows to get, update and delete the configuration for a service ID. See also {@link ConfigurableService}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(ConfigurableServiceResource.PATH_SERVICES)
@RolesAllowed({ Role.ADMIN })
@Api(value = ConfigurableServiceResource.PATH_SERVICES)
@Component(service = { RESTResource.class, ConfigurableServiceResource.class })
public class ConfigurableServiceResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_SERVICES = "services";

    // all singleton services without multi-config services
    private static final String CONFIGURABLE_SERVICE_FILTER = "(&("
            + ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=*)(!("
            + ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE + "=*)))";

    // all multi-config services without singleton services
    private static final String CONFIGURABLE_MULTI_CONFIG_SERVICE_FILTER = "("
            + ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE + "=*)";

    private final Logger logger = LoggerFactory.getLogger(ConfigurableServiceResource.class);

    private ConfigurationService configurationService;
    private ConfigDescriptionRegistry configDescRegistry;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all configurable services.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ConfigurableServiceDTO.class, responseContainer = "List") })
    public List<ConfigurableServiceDTO> getAll() {
        List<ConfigurableServiceDTO> services = getConfigurableServices();
        return services;
    }

    @GET
    @Path("/{serviceId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get configurable service for given service ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ConfigurableServiceDTO.class),
            @ApiResponse(code = 404, message = "Not found") })
    public Response getById(@PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        ConfigurableServiceDTO configurableService = getServiceById(serviceId);
        if (configurableService != null) {
            return Response.ok(configurableService).build();
        } else {
            return Response.status(404).build();
        }
    }

    private ConfigurableServiceDTO getServiceById(String serviceId) {
        ConfigurableServiceDTO multiService = getMultiConfigServiceById(serviceId);
        if (multiService != null) {
            return multiService;
        }

        List<ConfigurableServiceDTO> configurableServices = getConfigurableServices();
        for (ConfigurableServiceDTO configurableService : configurableServices) {
            if (configurableService.id.equals(serviceId)) {
                return configurableService;
            }
        }
        return null;
    }

    private ConfigurableServiceDTO getMultiConfigServiceById(String serviceId) {
        String filter = "(&(" + Constants.SERVICE_PID + "=" + serviceId + ")(" + ConfigurationAdmin.SERVICE_FACTORYPID
                + "=*))";
        List<ConfigurableServiceDTO> services = getServicesByFilter(filter);
        if (services.size() == 1) {
            return services.get(0);
        }
        return null;
    }

    @GET
    @Path("/{serviceId}/contexts")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get existing multiple context service configurations for the given factory PID.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ConfigurableServiceDTO.class, responseContainer = "List") })
    public List<ConfigurableServiceDTO> getMultiConfigServicesByFactoryPid(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        List<ConfigurableServiceDTO> services = collectServicesById(serviceId);
        return services;
    }

    private List<ConfigurableServiceDTO> collectServicesById(String serviceId) {
        String filter = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + serviceId + ")";
        return getServicesByFilter(filter);
    }

    @GET
    @Path("/{serviceId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get service configuration for given service ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 500, message = "Configuration can not be read due to internal error") })
    public Response getConfiguration(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        try {
            Configuration configuration = configurationService.get(serviceId);
            return configuration != null ? Response.ok(configuration.getProperties()).build()
                    : Response.ok(Collections.emptyMap()).build();
        } catch (IOException ex) {
            logger.error("Cannot get configuration for service {}: ", serviceId, ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{serviceId}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Updates a service configuration for given service ID and returns the old configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 204, message = "No old configuration"),
            @ApiResponse(code = 500, message = "Configuration can not be updated due to internal error") })
    public Response updateConfiguration(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId,
            Map<String, Object> configuration) {
        try {
            Configuration oldConfiguration = configurationService.get(serviceId);
            configurationService.update(serviceId, new Configuration(normalizeConfiguration(configuration, serviceId)));
            return oldConfiguration != null ? Response.ok(oldConfiguration.getProperties()).build()
                    : Response.noContent().build();
        } catch (IOException ex) {
            logger.error("Cannot update configuration for service {}: {}", serviceId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> normalizeConfiguration(Map<String, Object> properties, String serviceId) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ConfigurableServiceDTO service = getServiceById(serviceId);
        if (service == null) {
            return properties;
        }

        URI uri;
        try {
            uri = new URI(service.configDescriptionURI);
        } catch (URISyntaxException e) {
            logger.warn("Not a valid URI: {}", service.configDescriptionURI);
            return properties;
        }

        ConfigDescription configDesc = configDescRegistry.getConfigDescription(uri);
        if (configDesc == null) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, Collections.singletonList(configDesc));
    }

    @DELETE
    @Path("/{serviceId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Deletes a service configuration for given service ID and returns the old configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 204, message = "No old configuration"),
            @ApiResponse(code = 500, message = "Configuration can not be deleted due to internal error") })
    public Response deleteConfiguration(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        try {
            Configuration oldConfiguration = configurationService.get(serviceId);
            configurationService.delete(serviceId);
            return oldConfiguration != null ? Response.ok(oldConfiguration).build() : Response.noContent().build();
        } catch (IOException ex) {
            logger.error("Cannot delete configuration for service {}: {}", serviceId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<ConfigurableServiceDTO> getServicesByFilter(String filter) {
        List<ConfigurableServiceDTO> services = new ArrayList<>();
        ServiceReference<?>[] serviceReferences = null;
        try {
            serviceReferences = RESTCoreActivator.getBundleContext().getServiceReferences((String) null, filter);
        } catch (InvalidSyntaxException ex) {
            logger.error("Cannot get service references because the syntax of the filter '{}' is invalid.", filter);
        }

        if (serviceReferences != null) {
            for (ServiceReference<?> serviceReference : serviceReferences) {
                String id = getServiceId(serviceReference);
                String label = (String) serviceReference.getProperty(ConfigurableService.SERVICE_PROPERTY_LABEL);
                if (label == null) { // for multi context services the label can be changed and must be read from config
                                     // admin.
                    label = configurationService.getProperty(id, ConfigConstants.SERVICE_CONTEXT);
                }
                String category = (String) serviceReference.getProperty(ConfigurableService.SERVICE_PROPERTY_CATEGORY);
                String configDescriptionURI = (String) serviceReference
                        .getProperty(ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI);

                if (configDescriptionURI == null) {
                    String factoryPid = (String) serviceReference.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
                    configDescriptionURI = getConfigDescriptionByFactoryPid(factoryPid);
                }

                boolean multiple = Boolean.parseBoolean(
                        (String) serviceReference.getProperty(ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE));

                services.add(new ConfigurableServiceDTO(id, label, category, configDescriptionURI, multiple));
            }
        }
        return services;
    }

    private String getConfigDescriptionByFactoryPid(String factoryPid) {
        String configDescriptionURI = null;

        String filter = "(" + Constants.SERVICE_PID + "=" + factoryPid + ")";

        try {
            ServiceReference<?>[] refs = RESTCoreActivator.getBundleContext().getServiceReferences((String) null,
                    filter);

            if (refs != null && refs.length > 0) {
                configDescriptionURI = (String) refs[0]
                        .getProperty(ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI);
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot get service references because the syntax of the filter '{}' is invalid.", filter);
        }
        return configDescriptionURI;
    }

    private List<ConfigurableServiceDTO> getConfigurableServices() {
        List<ConfigurableServiceDTO> services = new ArrayList<>();

        services.addAll(getServicesByFilter(CONFIGURABLE_SERVICE_FILTER));
        services.addAll(getServicesByFilter(CONFIGURABLE_MULTI_CONFIG_SERVICE_FILTER));

        return services;
    }

    private String getServiceId(ServiceReference<?> serviceReference) {
        final String cn = (String) serviceReference.getProperty(ComponentConstants.COMPONENT_NAME);
        Object pid = serviceReference.getProperty(Constants.SERVICE_PID);
        if (pid == null) {
            return cn;
        }

        final String serviceId;
        if (pid instanceof String) {
            serviceId = (String) pid;
        } else if (pid instanceof String[]) {
            final String[] pids = (String[]) pid;
            serviceId = getServicePID(cn, Arrays.asList(pids));
        } else if (pid instanceof Collection) {
            Collection<?> pids = (Collection<?>) pid;
            serviceId = getServicePID(cn, pids.stream().map(entry -> entry.toString()).collect(Collectors.toList()));
        } else {
            logger.warn("The component \"{}\" is using an unhandled service PID type ({}). Use component name.", cn,
                    pid.getClass());
            serviceId = cn;
        }
        if (serviceId.isEmpty()) {
            logger.debug("Missing service PID for component \"{}\", use component name.", cn);
            return cn;
        } else {
            return serviceId;
        }
    }

    private String getServicePID(final String cn, final List<String> pids) {
        switch (pids.size()) {
            case 0:
                return "";
            case 1:
                return pids.get(0);
            default: // multiple entries
                final String first = pids.get(0);
                boolean differences = false;
                for (int i = 1; i < pids.size(); ++i) {
                    if (!first.equals(pids.get(i))) {
                        differences = true;
                        break;
                    }
                }
                if (differences) {
                    logger.warn(
                            "The component \"{}\" is using different service PIDs ({}). Different service PIDs are not supported, the first one ({}) is used.",
                            cn, pids, first);
                }
                return first;
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescRegistry = configDescriptionRegistry;
    }

    protected void unsetConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescRegistry = null;
    }

    @Override
    public boolean isSatisfied() {
        return configurationService != null && configDescRegistry != null;
    }

}
