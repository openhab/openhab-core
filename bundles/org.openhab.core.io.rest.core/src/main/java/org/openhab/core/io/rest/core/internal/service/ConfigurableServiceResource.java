/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ConfigurableServiceUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.config.ConfigurationService;
import org.openhab.core.io.rest.core.service.ConfigurableServiceDTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * {@link ConfigurableServiceResource} provides access to configurable services.
 * It lists the available services and allows to get, update and delete the
 * configuration for a service ID. See also {@link ConfigurableService}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component(service = { RESTResource.class, ConfigurableServiceResource.class })
@JaxrsResource
@JaxrsName(ConfigurableServiceResource.PATH_SERVICES)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ConfigurableServiceResource.PATH_SERVICES)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = ConfigurableServiceResource.PATH_SERVICES)
@NonNullByDefault
public class ConfigurableServiceResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_SERVICES = "services";

    private final Logger logger = LoggerFactory.getLogger(ConfigurableServiceResource.class);

    private final BundleContext bundleContext;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final ConfigurationService configurationService;
    private final TranslationProvider i18nProvider;
    private final LocaleService localeService;

    @Activate
    public ConfigurableServiceResource( //
            final BundleContext bundleContext, //
            final @Reference ConfigurationService configurationService, //
            final @Reference ConfigDescriptionRegistry configDescRegistry, //
            final @Reference TranslationProvider translationProvider, //
            final @Reference LocaleService localeService) {
        this.bundleContext = bundleContext;
        this.configDescRegistry = configDescRegistry;
        this.configurationService = configurationService;
        this.i18nProvider = translationProvider;
        this.localeService = localeService;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getServices", summary = "Get all configurable services.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConfigurableServiceDTO.class)))) })
    public List<ConfigurableServiceDTO> getAll(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language) {
        Locale locale = localeService.getLocale(language);
        return getConfigurableServices(locale);
    }

    @GET
    @Path("/{serviceId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getServicesById", summary = "Get configurable service for given service ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ConfigurableServiceDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response getById(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("serviceId") @Parameter(description = "service ID") String serviceId) {
        Locale locale = localeService.getLocale(language);
        ConfigurableServiceDTO configurableService = getServiceById(serviceId, locale);
        if (configurableService != null) {
            return Response.ok(configurableService).build();
        } else {
            return Response.status(404).build();
        }
    }

    private @Nullable ConfigurableServiceDTO getServiceById(String serviceId, Locale locale) {
        ConfigurableServiceDTO multiService = getMultiConfigServiceById(serviceId, locale);
        if (multiService != null) {
            return multiService;
        }

        List<ConfigurableServiceDTO> configurableServices = getConfigurableServices(locale);
        for (ConfigurableServiceDTO configurableService : configurableServices) {
            if (configurableService.id.equals(serviceId)) {
                return configurableService;
            }
        }
        return null;
    }

    private @Nullable ConfigurableServiceDTO getMultiConfigServiceById(String serviceId, Locale locale) {
        String filter = "(&(" + Constants.SERVICE_PID + "=" + serviceId + ")(" + ConfigurationAdmin.SERVICE_FACTORYPID
                + "=*))";
        List<ConfigurableServiceDTO> services = getServicesByFilter(filter, locale);
        if (services.size() == 1) {
            return services.get(0);
        }
        return null;
    }

    @GET
    @Path("/{serviceId}/contexts")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getServiceContext", summary = "Get existing multiple context service configurations for the given factory PID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConfigurableServiceDTO.class)))) })
    public List<ConfigurableServiceDTO> getMultiConfigServicesByFactoryPid(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("serviceId") @Parameter(description = "service ID") String serviceId) {
        Locale locale = localeService.getLocale(language);
        return collectServicesById(serviceId, locale);
    }

    private List<ConfigurableServiceDTO> collectServicesById(String serviceId, Locale locale) {
        String filter = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + serviceId + ")";
        return getServicesByFilter(filter, locale);
    }

    @GET
    @Path("/{serviceId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getServiceConfig", summary = "Get service configuration for given service ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Configuration can not be read due to internal error") })
    public Response getConfiguration(@PathParam("serviceId") @Parameter(description = "service ID") String serviceId) {
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
    @Operation(operationId = "updateServiceConfig", summary = "Updates a service configuration for given service ID and returns the old configuration.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "204", description = "No old configuration"),
            @ApiResponse(responseCode = "500", description = "Configuration can not be updated due to internal error") })
    public Response updateConfiguration(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("serviceId") @Parameter(description = "service ID") String serviceId,
            @Nullable Map<String, Object> configuration) {
        Locale locale = localeService.getLocale(language);
        try {
            Configuration oldConfiguration = configurationService.get(serviceId);
            configurationService.update(serviceId,
                    new Configuration(normalizeConfiguration(configuration, serviceId, locale)));
            return oldConfiguration != null ? Response.ok(oldConfiguration.getProperties()).build()
                    : Response.noContent().build();
        } catch (IOException ex) {
            logger.error("Cannot update configuration for service {}: {}", serviceId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            String serviceId, Locale locale) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ConfigurableServiceDTO service = getServiceById(serviceId, locale);
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

        return ConfigUtil.normalizeTypes(properties, List.of(configDesc));
    }

    @DELETE
    @Path("/{serviceId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "deleteServiceConfig", summary = "Deletes a service configuration for given service ID and returns the old configuration.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "204", description = "No old configuration"),
            @ApiResponse(responseCode = "500", description = "Configuration can not be deleted due to internal error") })
    public Response deleteConfiguration(
            @PathParam("serviceId") @Parameter(description = "service ID") String serviceId) {
        try {
            Configuration oldConfiguration = configurationService.get(serviceId);
            configurationService.delete(serviceId);
            return oldConfiguration != null ? Response.ok(oldConfiguration).build() : Response.noContent().build();
        } catch (IOException ex) {
            logger.error("Cannot delete configuration for service {}: {}", serviceId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<ConfigurableServiceDTO> getServicesByFilter(String filter, Locale locale) {
        List<ConfigurableServiceDTO> services = new ArrayList<>();
        ServiceReference<?>[] serviceReferences = null;
        try {
            serviceReferences = bundleContext.getServiceReferences((String) null, filter);
        } catch (InvalidSyntaxException ex) {
            logger.error("Cannot get service references because the syntax of the filter '{}' is invalid.", filter);
        }

        if (serviceReferences != null) {
            for (ServiceReference<?> serviceReference : serviceReferences) {
                String id = getServiceId(serviceReference);
                ConfigurableService configurableService = ConfigurableServiceUtil
                        .asConfigurableService((key) -> serviceReference.getProperty(key));

                String defaultLabel = configurableService.label();
                if (defaultLabel.isEmpty()) { // for multi context services the label can be changed and must be read
                                              // from config admin.
                    defaultLabel = configurationService.getProperty(id, OpenHAB.SERVICE_CONTEXT);
                }

                String key = I18nUtil.stripConstantOr(defaultLabel,
                        () -> inferKey(configurableService.description_uri(), "label"));

                String label = i18nProvider.getText(serviceReference.getBundle(), key, defaultLabel, locale);

                String category = configurableService.category();

                String configDescriptionURI = configurableService.description_uri();
                if (configDescriptionURI.isEmpty()) {
                    String factoryPid = (String) serviceReference.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
                    configDescriptionURI = getConfigDescriptionByFactoryPid(factoryPid);
                }

                boolean multiple = configurableService.factory();

                services.add(new ConfigurableServiceDTO(id, label == null ? defaultLabel : label, category,
                        configDescriptionURI, multiple));
            }
        }
        return services;
    }

    private @Nullable String getConfigDescriptionByFactoryPid(String factoryPid) {
        String configDescriptionURI = null;

        String filter = "(" + Constants.SERVICE_PID + "=" + factoryPid + ")";

        try {
            ServiceReference<?>[] refs = bundleContext.getServiceReferences((String) null, filter);

            if (refs != null && refs.length > 0) {
                ConfigurableService configurableService = ConfigurableServiceUtil
                        .asConfigurableService((key) -> refs[0].getProperty(key));
                configDescriptionURI = configurableService.description_uri();
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot get service references because the syntax of the filter '{}' is invalid.", filter);
        }
        return configDescriptionURI;
    }

    private List<ConfigurableServiceDTO> getConfigurableServices(Locale locale) {
        List<ConfigurableServiceDTO> services = new ArrayList<>();

        services.addAll(getServicesByFilter(ConfigurableServiceUtil.CONFIGURABLE_SERVICE_FILTER, locale));
        services.addAll(getServicesByFilter(ConfigurableServiceUtil.CONFIGURABLE_MULTI_CONFIG_SERVICE_FILTER, locale));

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

    private String inferKey(String uri, String lastSegment) {
        return "service." + uri.replaceAll(":", ".") + "." + lastSegment;
    }
}
