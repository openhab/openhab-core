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
package org.openhab.core.io.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.openhab.core.library.types.DateTimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * Static helper methods to build up JSON-like Response objects and error handling.
 *
 * @author Joerg Plewe - Initial contribution
 * @author Henning Treu - Provide streaming capabilities
 */
public class JSONResponse {

    private final Logger logger = LoggerFactory.getLogger(JSONResponse.class);

    private static final JSONResponse INSTANCE = new JSONResponse();
    private final Gson gson = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS).create();

    static final String JSON_KEY_ERROR_MESSAGE = "message";
    static final String JSON_KEY_ERROR = "error";
    static final String JSON_KEY_HTTPCODE = "http-code";
    static final String JSON_KEY_ENTITY = "entity";

    /**
     * avoid instantiation apart from {@link #createResponse}.
     */
    private JSONResponse() {
    }

    /**
     * in case of error (404 and such)
     *
     * @param status
     * @param errormessage
     * @return Response containing a status and the errormessage in JSON format
     */
    public static Response createErrorResponse(Response.StatusType status, String errormessage) {
        return createResponse(status, null, errormessage);
    }

    /**
     * Depending on the status, create a Response object containing either the entity alone or an error JSON
     * which might hold the entity as well.
     *
     * @param status the status for the response.
     * @param entity the entity which is transformed into a JSON stream.
     * @param errormessage an optional error message (may be null), ignored if the status family is successful
     * @return Response configure for error or success
     */
    public static Response createResponse(Response.StatusType status, Object entity, String errormessage) {
        if (status.getFamily() != Response.Status.Family.SUCCESSFUL) {
            return INSTANCE.createErrorResponse(status, entity, errormessage);
        }

        return INSTANCE.createResponse(status, entity);
    }

    /**
     * basic configuration of a ResponseBuilder
     *
     * @param status
     * @return ResponseBuilder configured for "Content-Type" MediaType.APPLICATION_JSON
     */
    private ResponseBuilder responseBuilder(Response.StatusType status) {
        return Response.status(status).header("Content-Type", MediaType.APPLICATION_JSON);
    }

    /**
     * setup JSON depending on the content
     *
     * @param message a message (may be null)
     * @param status
     * @param entity
     * @param ex
     * @return
     */
    private JsonElement createErrorJson(String message, Response.StatusType status, Object entity, Exception ex) {
        JsonObject resultJson = new JsonObject();
        JsonObject errorJson = new JsonObject();
        resultJson.add(JSON_KEY_ERROR, errorJson);

        errorJson.addProperty(JSON_KEY_ERROR_MESSAGE, message);

        // in case we have a http status code, report it
        if (status != null) {
            errorJson.addProperty(JSON_KEY_HTTPCODE, status.getStatusCode());
        }

        // in case there is an entity...
        if (entity != null) {
            // return the existing object
            resultJson.add(JSON_KEY_ENTITY, gson.toJsonTree(entity));
        }

        // is there an exception?
        if (ex != null) {
            // JSONify the Exception
            JsonObject exceptionJson = new JsonObject();
            exceptionJson.addProperty("class", ex.getClass().getName());
            exceptionJson.addProperty("message", ex.getMessage());
            exceptionJson.addProperty("localized-message", ex.getLocalizedMessage());
            exceptionJson.addProperty("cause", null != ex.getCause() ? ex.getCause().getClass().getName() : null);
            errorJson.add("exception", exceptionJson);
        }

        return resultJson;
    }

    private Response createErrorResponse(Response.StatusType status, Object entity, String errormessage) {
        ResponseBuilder rp = responseBuilder(status);
        JsonElement errorJson = createErrorJson(errormessage, status, entity, null);
        rp.entity(errorJson);
        return rp.build();
    }

    private Response createResponse(Response.StatusType status, final Object entity) {
        ResponseBuilder rp = responseBuilder(status);

        if (entity == null) {
            return rp.build();
        }

        // The PipedOutputStream will only be closed by the writing thread
        // since closing it during this method call would be too early.
        // The receiver of the response will read from the pipe after this method returns.
        PipedOutputStream out = new PipedOutputStream();

        try {
            // we will not actively close the PipedInputStream since it is read by the receiving end
            // and will be GC'ed once the response is consumed.
            PipedJSONInputStream in = new PipedJSONInputStream(out);
            rp.entity(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        Thread writerThread = new Thread(() -> {
            try (JsonWriter jsonWriter = new JsonWriter(
                    new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))) {
                gson.toJson(entity, entity.getClass(), jsonWriter);
                jsonWriter.flush();
            } catch (IOException | JsonIOException e) {
                logger.debug("Error streaming JSON through PipedInputStream / PipedOutputStream.", e);
            }
        });

        writerThread.setDaemon(true); // daemonize thread to permit the JVM shutdown even if we stream JSON.
        writerThread.start();

        return rp.build();
    }

    /**
     * An piped input stream that is marked to produce JSON string.
     *
     * @author Markus Rathgeb - Initial contribution
     */
    private static class PipedJSONInputStream extends PipedInputStream implements JSONInputStream {

        public PipedJSONInputStream(PipedOutputStream src) throws IOException {
            super(src);
        }
    }

    /**
     * trap exceptions
     *
     * @author Joerg Plewe
     */
    public static class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {

        private final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

        @Override
        public Response toResponse(Exception e) {
            logger.debug("Exception during REST handling.", e);

            Response.StatusType status = Response.Status.INTERNAL_SERVER_ERROR;

            // in case the Exception is a WebApplicationException, it already carries a Status
            if (e instanceof WebApplicationException) {
                status = ((WebApplicationException) e).getResponse().getStatusInfo();
            }

            JsonElement ret = INSTANCE.createErrorJson(e.getMessage(), status, null, e);
            return INSTANCE.responseBuilder(status).entity(INSTANCE.gson.toJson(ret)).build();
        }
    }
}
