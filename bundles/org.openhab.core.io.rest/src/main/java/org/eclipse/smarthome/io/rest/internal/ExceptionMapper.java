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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.io.rest.internal.dto.ErrorResultDTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * trap exceptions
 *
 * @author Joerg Plewe - Initial contribution
 */
@Component(immediate = true)
@Provider
@JaxrsExtension
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {
    public final Gson gson = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS).create();

    private final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

    /**
     * create JSON Response
     */
    @Override
    public Response toResponse(Exception e) {
        logger.debug("exception during REST Handling", e);

        Response.StatusType status = Response.Status.INTERNAL_SERVER_ERROR;

        // in case the Exception is a WebApplicationException, it already carries a Status
        if (e instanceof WebApplicationException) {
            status = ((WebApplicationException) e).getResponse().getStatusInfo();
        }

        ErrorResultDTO ret = new ErrorResultDTO(e.getMessage(), status, null, e);
        return Response.status(status).header("Content-Type", MediaType.APPLICATION_JSON).entity(gson.toJson(ret))
                .build();
    }
}