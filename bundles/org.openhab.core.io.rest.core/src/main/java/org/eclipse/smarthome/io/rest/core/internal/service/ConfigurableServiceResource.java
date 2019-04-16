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
package org.eclipse.smarthome.io.rest.core.internal.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.ConfigUtil;
import org.eclipse.smarthome.config.core.ConfigurableService;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.rest.core.config.ConfigurationService;
import org.eclipse.smarthome.io.rest.core.service.ConfigurableServiceDTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
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
 *
 */
@Path(ConfigurableServiceResource.PATH_SERVICES)
@RolesAllowed({ Role.ADMIN })
@Api(value = ConfigurableServiceResource.PATH_SERVICES)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ConfigurableServiceResource {
    public static final String PATH_SERVICES = "services";

    // all singleton services without multi-config services
    private static final String CONFIGURABLE_SERVICE_FILTER = "(&("
            + ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=*)(!("
            + ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE + "=*)))";

    // all multi-config services without singleton services
    private static final String CONFIGURABLE_MULTI_CONFIG_SERVICE_FILTER = "("
            + ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE + "=*)";

    private final Logger logger = LoggerFactory.getLogger(ConfigurableServiceResource.class);

    @Reference
    private @NonNullByDefault({}) ConfigurationService configurationService;
    @Reference
    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescRegistry;

    private final String serviceFilter = "(&(" + Constants.SERVICE_PID + "={})(" + ConfigurationAdmin.SERVICE_FACTORYPID
            + "=*))";

    private final String factoryFilter = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "={})";

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all configurable services.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ConfigurableServiceDTO.class, responseContainer = "List") })
    public Stream<ConfigurableServiceDTO> getAll() {
        return Stream.concat(getServicesByFilter(CONFIGURABLE_SERVICE_FILTER),
                getServicesByFilter(CONFIGURABLE_MULTI_CONFIG_SERVICE_FILTER));
    }

    @GET
    @Path("/{serviceId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get configurable service for given service ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ConfigurableServiceDTO.class),
            @ApiResponse(code = 404, message = "Not found") })
    public ConfigurableServiceDTO getById(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        try {
            return getServiceById(serviceId);
        } catch (NoSuchElementException ignored) {
            throw new NotFoundException();
        }
    }

    private ConfigurableServiceDTO getServiceById(String serviceId) throws NoSuchElementException {
        return getServicesByFilter(String.format(serviceFilter, serviceId)).findAny().orElse(
                getAll().filter(configurableService -> configurableService.id.equals(serviceId)).findAny().get());
    }

    @GET
    @Path("/{serviceId}/contexts")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get existing multiple context service configurations for the given factory PID.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ConfigurableServiceDTO.class, responseContainer = "List") })
    public Stream<ConfigurableServiceDTO> getMultiConfigServicesByFactoryPid(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        return getServicesByFilter(String.format(factoryFilter, serviceId));
    }

    @SuppressWarnings("null")
    @GET
    @Path("/{serviceId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get service configuration for given service ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 500, message = "Configuration can not be read due to internal error") })
    public Map<String, Object> getConfiguration(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        try {
            Configuration configuration = configurationService.get(serviceId);
            return configuration != null ? configuration.getProperties() : Collections.emptyMap();
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    @SuppressWarnings("null")
    @PUT
    @Path("/{serviceId}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Updates a service configuration for given service ID and returns the old configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 204, message = "No old configuration"),
            @ApiResponse(code = 500, message = "Configuration can not be updated due to internal error") })
    public Map<String, Object> updateConfiguration(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId,
            Map<String, Object> configuration) {
        try {
            Configuration oldConfiguration = configurationService.get(serviceId);
            configurationService.update(serviceId, new Configuration(normalizeConfiguration(configuration, serviceId)));
            return oldConfiguration != null ? oldConfiguration.getProperties() : Collections.emptyMap();
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            String serviceId) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }

        final ConfigurableServiceDTO service;
        try {
            service = getServiceById(serviceId);
        } catch (NoSuchElementException ignored) {
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

    @SuppressWarnings("null")
    @DELETE
    @Path("/{serviceId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Deletes a service configuration for given service ID and returns the old configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 500, message = "Configuration can not be deleted due to internal error") })
    public Map<String, Object> deleteConfiguration(
            @PathParam("serviceId") @ApiParam(value = "service ID", required = true) String serviceId) {
        try {
            Configuration oldConfiguration = configurationService.get(serviceId);
            configurationService.delete(serviceId);
            return oldConfiguration != null ? oldConfiguration.getProperties() : Collections.emptyMap();
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    private Stream<ConfigurableServiceDTO> getServicesByFilter(String filter) {
        ServiceReference<?>[] serviceReferences = null;
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        try {
            serviceReferences = context.getServiceReferences((String) null, filter);
        } catch (InvalidSyntaxException ex) {
            logger.error("Cannot get service references because the syntax of the filter '{}' is invalid.", filter);
        }

        if (serviceReferences == null) {
            return Stream.empty();
        }

        return Arrays.stream(serviceReferences).map(serviceReference -> {
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

            return new ConfigurableServiceDTO(id, label, category, configDescriptionURI, multiple);
        });
    }

    private @Nullable String getConfigDescriptionByFactoryPid(String factoryPid) {
        String configDescriptionURI = null;

        String filter = "(" + Constants.SERVICE_PID + "=" + factoryPid + ")";

        try {
            BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            ServiceReference<?>[] refs = context.getServiceReferences((String) null, filter);

            if (refs != null && refs.length > 0) {
                configDescriptionURI = (String) refs[0]
                        .getProperty(ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI);
            }
        } catch (InvalidSyntaxException e) {
            throw new BadRequestException(
                    "Cannot get service references because the syntax of the filter '" + filter + "' is invalid.");
        }
        return configDescriptionURI;
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
}
