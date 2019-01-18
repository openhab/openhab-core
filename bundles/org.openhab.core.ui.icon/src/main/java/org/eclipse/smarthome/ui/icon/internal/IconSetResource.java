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
package org.eclipse.smarthome.ui.icon.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.eclipse.smarthome.ui.icon.IconProvider;
import org.eclipse.smarthome.ui.icon.IconSet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is a REST resource that provides information about available icon sets.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Path("iconsets")
@Component
public class IconSetResource implements RESTResource {

    private List<IconProvider> iconProviders = new ArrayList<>(5);

    private LocaleService localeService;

    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    protected void addIconProvider(IconProvider iconProvider) {
        this.iconProviders.add(iconProvider);
    }

    protected void removeIconProvider(IconProvider iconProvider) {
        this.iconProviders.remove(iconProvider);
    }

    @Reference
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }
    
    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@HeaderParam("Accept-Language") String language) {
        Locale locale = localeService.getLocale(language);

        List<IconSet> iconSets = new ArrayList<>(iconProviders.size());
        for (IconProvider iconProvider : iconProviders) {
            iconSets.addAll(iconProvider.getIconSets(locale));
        }
        return Response.ok(iconSets).build();
    }

}
