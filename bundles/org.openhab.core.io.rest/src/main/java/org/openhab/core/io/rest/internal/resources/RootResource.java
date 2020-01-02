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
package org.openhab.core.io.rest.internal.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.internal.Constants;
import org.openhab.core.io.rest.internal.resources.beans.RootBean;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class acts as an entry point / root resource for the REST API.
 *
 * <p>
 * In good HATEOAS manner, it provides links to other offered resources.
 *
 * <p>
 * The result is returned as JSON
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Path("/")
@Component(service = RootResource.class, configurationPid = "org.openhab.restroot")
public class RootResource {

    private final transient Logger logger = LoggerFactory.getLogger(RootResource.class);

    private final List<RESTResource> restResources = new ArrayList<>();

    private ConfigurationAdmin configurationAdmin;

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot(@Context HttpHeaders headers) {
        return Response.ok(getRootBean()).build();
    }

    private RootBean getRootBean() {
        RootBean bean = new RootBean();

        for (RESTResource resource : restResources) {
            // we will include all RESTResources that are currently satisfied
            if (resource.isSatisfied()) {
                String path = resource.getClass().getAnnotation(Path.class).value();
                bean.links
                        .add(new RootBean.Links(path, uriInfo.getBaseUriBuilder().path(path).build().toASCIIString()));
            }
        }

        return bean;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRESTResource(RESTResource resource) {
        restResources.add(resource);
    }

    public void removeRESTResource(RESTResource resource) {
        restResources.remove(resource);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Activate
    public void activate() {
        Configuration configuration;
        try {
            configuration = configurationAdmin.getConfiguration(Constants.JAXRS_CONNECTOR_CONFIG, null);

            if (configuration != null) {
                Dictionary properties = configuration.getProperties();

                if (properties == null) {
                    properties = new Properties();
                }

                String rootAlias = (String) properties.get(Constants.JAXRS_CONNECTOR_ROOT_PROPERTY);
                if (!RESTConstants.REST_URI.equals(rootAlias)) {
                    properties.put(Constants.JAXRS_CONNECTOR_ROOT_PROPERTY, RESTConstants.REST_URI);

                    configuration.update(properties);
                }
            }
        } catch (IOException e) {
            logger.error("Could not set REST configuration properties!", e);
        }
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }

}
