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
package org.eclipse.smarthome.io.rest.internal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.rest.internal.dto.RootDTO;
import org.eclipse.smarthome.io.rest.internal.dto.RootDTO.Links;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

/**
 * Returns all available REST endpoints
 *
 * @author David Graeff - Initial contribution
 */
@Path("/")
@Component(immediate = true)
@Produces(MediaType.APPLICATION_JSON)
@JaxrsResource
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@NonNullByDefault
public class RootResource {
    @Reference
    protected @NonNullByDefault({}) JaxrsServiceRuntime runtime;

    @GET
    public Object getRoot() {
        RootDTO bean = new RootDTO();
        for (ApplicationDTO a : runtime.getRuntimeDTO().applicationDTOs) {
            for (ResourceDTO resourceDTO : a.resourceDTOs) {
                bean.links.add(new Links(resourceDTO.name, resourceDTO.name));
            }
        }
        return bean;
    }

}
