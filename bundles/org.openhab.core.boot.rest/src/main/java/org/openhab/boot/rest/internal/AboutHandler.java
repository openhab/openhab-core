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
package org.openhab.boot.rest.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.openhab.boot.rest.internal.dto.About;
import org.openhab.boot.rest.internal.dto.About.Distribution;
import org.openhab.core.OpenHAB;
import org.osgi.service.component.annotations.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Provides the /about rest endpoint
 * 
 * @author David Graeff - Initial contribution
 */
@Component
@Path("/about")
@Api(value = "about")
@Produces(MediaType.APPLICATION_JSON)
@NonNullByDefault
public class AboutHandler implements RESTResource {
    @GET
    @ApiOperation(value = "Get name and version information about the running openHAB installation and underlying distribution")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of about detail", response = About.class),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response about() throws IOException, ParseException {
        InputStream stream = getClass().getResourceAsStream("/META-INF/MANIFEST.MF");
        Manifest manifest = new Manifest(stream);
        Attributes attributes = manifest.getMainAttributes();
        String builtDateStr = attributes.getValue("Built-Date");
        Date buildDate = Date.from(Instant.now());
        buildDate = new SimpleDateFormat().parse(builtDateStr);
        return Response.ok(new About("openHAB", OpenHAB.getVersion(), buildDate, distribution())).build();
    }

    static public @Nullable Distribution distribution() {
        Properties prop = new Properties();

        java.nio.file.Path versionFilePath = Paths.get(ConfigConstants.getUserDataFolder(), "etc",
                "distribution.properties");
        try (FileInputStream fis = new FileInputStream(versionFilePath.toFile())) {
            prop.load(fis);
            String name = prop.getProperty("name", "");
            String version = prop.getProperty("version", "");
            String abouturl = prop.getProperty("abouturl", "");
            return new Distribution(name, version, abouturl);
        } catch (Exception ignore) {
            // ignore if the file is not there or not readable
            return null;
        }
    }
}
