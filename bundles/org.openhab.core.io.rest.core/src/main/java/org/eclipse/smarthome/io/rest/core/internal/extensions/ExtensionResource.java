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
package org.eclipse.smarthome.io.rest.core.internal.extensions;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.extension.Extension;
import org.eclipse.smarthome.core.extension.ExtensionService;
import org.eclipse.smarthome.core.extension.ExtensionType;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for extensions and provides methods to install and uninstall them.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(ExtensionResource.PATH_EXTENSIONS)
@RolesAllowed({ Role.ADMIN })
@Api(value = ExtensionResource.PATH_EXTENSIONS)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ExtensionResource {
    public static final String PATH_EXTENSIONS = "extensions";

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private final @NonNullByDefault({}) Set<ExtensionService> extensionServices = new TreeSet<>();

    @Reference
    private @NonNullByDefault({}) EventPublisher eventPublisher;

    @Reference
    private @NonNullByDefault({}) LocaleService localeService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all extensions.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class) })
    public Stream<?> getExtensions(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language) {
        Locale locale = localeService.getLocale(language);
        return extensionServices.stream().map(s -> s.getExtensions(locale)).flatMap(l -> l.stream());
    }

    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all extension types.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class) })
    public Stream<?> getTypes(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language) {
        Locale locale = localeService.getLocale(language);

        final Collator coll = Collator.getInstance(locale);
        coll.setStrength(Collator.PRIMARY);
        Comparator<ExtensionType> comparator = new Comparator<ExtensionType>() {
            @Override
            public int compare(ExtensionType o1, ExtensionType o2) {
                return coll.compare(o1.getLabel(), o2.getLabel());
            }
        };
        return extensionServices.stream().flatMap(extensionService -> extensionService.getTypes(locale).stream())
                .distinct().sorted(comparator);
    }

    @GET
    @Path("/{extensionId: [a-zA-Z_0-9-]*}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get extension with given ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Not found") })
    public Extension getById(@HeaderParam("Accept-Language") @ApiParam(value = "language") String language,
            @PathParam("extensionId") @ApiParam(value = "extension ID", required = true) String extensionId) {
        Extension responseObject = getExtensionService(extensionId).getExtension(extensionId,
                localeService.getLocale(language));
        if (responseObject == null) {
            throw new NotFoundException();
        }
        return responseObject;
    }

    @POST
    @Path("/{extensionId: [a-zA-Z_0-9-:]*}/install")
    @ApiOperation(value = "Installs the extension with the given ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public void installExtension(
            final @PathParam("extensionId") @ApiParam(value = "extension ID", required = true) String extensionId) {
        getExtensionService(extensionId).install(extensionId);
    }

    @POST
    @Path("/url/{url}/install")
    @ApiOperation(value = "Installs the extension from the given URL.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "The given URL is malformed or not valid.") })
    public void installExtensionByURL(
            final @PathParam("url") @ApiParam(value = "extension install URL", required = true) String url) {
        final URI extensionURI;
        try {
            extensionURI = new URI(url);
        } catch (URISyntaxException e) {
            throw new BadRequestException("The given URL is malformed or not valid.", e);
        }
        String extensionId = getExtensionId(extensionURI);
        installExtension(extensionId);
    }

    @POST
    @Path("/{extensionId: [a-zA-Z_0-9-:]*}/uninstall")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public void uninstallExtension(
            final @PathParam("extensionId") @ApiParam(value = "extension ID", required = true) String extensionId) {
        getExtensionService(extensionId).uninstall(extensionId);
    }

    private ExtensionService getExtensionService(final String extensionId) {
        for (ExtensionService extensionService : extensionServices) {
            for (Extension extension : extensionService.getExtensions(Locale.getDefault())) {
                if (extensionId.equals(extension.getId())) {
                    return extensionService;
                }
            }
        }
        throw new NotFoundException("No extension service registered for " + extensionId);
    }

    private String getExtensionId(URI extensionURI) {
        for (ExtensionService extensionService : extensionServices) {
            String extensionId = extensionService.getExtensionId(extensionURI);
            if (StringUtils.isNotBlank(extensionId)) {
                return extensionId;
            }
        }

        throw new NotFoundException("No extension service registered for URI " + extensionURI);
    }
}
