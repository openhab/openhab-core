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
package org.openhab.core.io.rest.log.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openhab.core.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 *
 * @author Sebastian Janzen - Initial contribution
 */
@Component
@Path("/log")
@Api(value = LogHandler.PATH_LOG)
@Produces(MediaType.APPLICATION_JSON)
public class LogHandler implements RESTResource {

    private final Logger logger = LoggerFactory.getLogger(LogHandler.class);
    public static final String PATH_LOG = "log";

    private static final String TEMPLATE_INTERNAL_ERROR = "{\"error\":\"%s\",\"severity\":\"%s\"}";

    /**
     * Rolling array to store the last LOG_BUFFER_LIMIT messages. Those can be fetched e.g. by a
     * diagnostic UI to display errors of other clients, where e.g. the logs are not easily accessible.
     */
    private final ConcurrentLinkedDeque<LogMessage> logBuffer = new ConcurrentLinkedDeque<>();

    /**
     * Container for a log message
     */
    public class LogMessage {
        public long timestamp;
        public String severity;
        public URL url;
        public String message;
    }

    @GET
    @Path("/levels")
    @ApiOperation(value = "Get log severities, which are logged by the current logger settings.", code = 200, notes = "This depends on the current log settings at the backend.")
    public Response getLogLevels() {
        return Response.ok(createLogLevelsMap()).build();
    }

    @GET
    @ApiOperation(value = "Returns the last logged frontend messages. The amount is limited to the "
            + LogConstants.LOG_BUFFER_LIMIT + " last entries.")
    @ApiParam(name = "limit", allowableValues = "range[1, " + LogConstants.LOG_BUFFER_LIMIT + "]")
    public Response getLastLogs(@DefaultValue(LogConstants.LOG_BUFFER_LIMIT + "") @QueryParam("limit") Integer limit) {
        if (logBuffer.isEmpty()) {
            return Response.ok("[]").build();
        }

        int effectiveLimit;
        if (limit == null || limit <= 0 || limit > LogConstants.LOG_BUFFER_LIMIT) {
            effectiveLimit = logBuffer.size();
        } else {
            effectiveLimit = limit;
        }

        if (effectiveLimit >= logBuffer.size()) {
            return Response.ok(logBuffer.toArray()).build();
        } else {
            final List<LogMessage> result = new ArrayList<>();
            Iterator<LogMessage> iter = logBuffer.descendingIterator();
            do {
                result.add(iter.next());
            } while (iter.hasNext() && result.size() < effectiveLimit);
            Collections.reverse(result);
            return Response.ok(result).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Log a frontend log message to the backend.")
    @ApiParam(name = "logMessage", value = "Severity is required and can be one of error, warn, info or debug, depending on activated severities which you can GET at /logLevels.", example = "{\"severity\": \"error\", \"url\": \"http://example.org\", \"message\": \"Error message\"}")
    @ApiResponses({ @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = LogConstants.LOG_SEVERITY_IS_NOT_SUPPORTED) })
    public Response log(final LogMessage logMessage) {
        if (logMessage == null) {
            logger.debug("Received null log message model!");
            return Response.status(500)
                    .entity(String.format(TEMPLATE_INTERNAL_ERROR, LogConstants.LOG_HANDLE_ERROR, "ERROR")).build();
        }
        logMessage.timestamp = Calendar.getInstance().getTimeInMillis();

        if (!doLog(logMessage)) {
            return Response.status(403).entity(String.format(TEMPLATE_INTERNAL_ERROR,
                    LogConstants.LOG_SEVERITY_IS_NOT_SUPPORTED, logMessage.severity)).build();
        }

        logBuffer.add(logMessage);
        if (logBuffer.size() > LogConstants.LOG_BUFFER_LIMIT) {
            logBuffer.pollLast(); // Remove last element of Deque
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    /**
     * Executes the logging call.
     *
     * @param logMessage
     * @return Falls if severity is not supported, true if successfully logged.
     */
    private boolean doLog(LogMessage logMessage) {
        switch (logMessage.severity.toLowerCase()) {
            case "error":
                logger.error(LogConstants.FRONTEND_LOG_PATTERN, logMessage.url, logMessage.message);
                break;
            case "warn":
                logger.warn(LogConstants.FRONTEND_LOG_PATTERN, logMessage.url, logMessage.message);
                break;
            case "info":
                logger.info(LogConstants.FRONTEND_LOG_PATTERN, logMessage.url, logMessage.message);
                break;
            case "debug":
                logger.debug(LogConstants.FRONTEND_LOG_PATTERN, logMessage.url, logMessage.message);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Return map of currently logged messages. They can change at runtime.
     */
    private Map<String, Boolean> createLogLevelsMap() {
        Map<String, Boolean> result = new HashMap<>();
        result.put("error", logger.isErrorEnabled());
        result.put("warn", logger.isWarnEnabled());
        result.put("info", logger.isInfoEnabled());
        result.put("debug", logger.isDebugEnabled());
        return result;
    }

}
