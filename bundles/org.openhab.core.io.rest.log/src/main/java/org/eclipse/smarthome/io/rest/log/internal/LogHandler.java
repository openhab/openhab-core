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
package org.eclipse.smarthome.io.rest.log.internal;

import java.net.URL;
import java.util.Calendar;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.service.component.annotations.Component;
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
 *
 * @author Sebastian Janzen - Initial contribution
 */
@Path("/log")
@Api(value = LogHandler.PATH_LOG)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class LogHandler {

    private final Logger logger = LoggerFactory.getLogger(LogHandler.class);
    public static final String PATH_LOG = "log";

    private static final String TEMPLATE_INTERNAL_ERROR = "{\"error\":\"%s\",\"severity\":\"%s\"}";

    /**
     * Rolling array to store the last LOG_BUFFER_LIMIT messages. Those can be fetched e.g. by a
     * diagnostic UI to display errors of other clients, where e.g. the logs are not easily accessible.
     */
    private final Deque<LogMessage> LOG_BUFFER = new ConcurrentLinkedDeque<>();

    /**
     * Container for a log message
     */
    @NonNullByDefault({})
    public class LogMessage {
        public long timestamp;
        public String severity;
        public URL url;
        public String message;
    }

    @GET
    @Path("/levels")
    @ApiOperation(value = "Get log severities, which are logged by the current logger settings.", code = 200, notes = "This depends on the current log settings at the backend.")
    public Map<String, Boolean> getLogLevels() {
        return createLogLevelsMap();
    }

    @GET
    @ApiOperation(value = "Returns the last logged frontend messages. The amount is limited to the "
            + LogConstants.LOG_BUFFER_LIMIT + " last entries.")
    @ApiParam(name = "limit", allowableValues = "range[1, " + LogConstants.LOG_BUFFER_LIMIT + "]")
    public Stream<LogMessage> getLastLogs(
            @DefaultValue(LogConstants.LOG_BUFFER_LIMIT + "") @QueryParam("limit") Integer limit) {
        if (LOG_BUFFER.size() <= 0) {
            return Stream.empty();
        }

        int effectiveLimit;
        if (limit <= 0 || limit > LogConstants.LOG_BUFFER_LIMIT) {
            effectiveLimit = LOG_BUFFER.size();
        } else {
            effectiveLimit = limit;
        }

        return LOG_BUFFER.stream().limit(effectiveLimit);

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Log a frontend log message to the backend.")
    @ApiParam(name = "logMessage", value = "Severity is required and can be one of error, warn, info or debug, depending on activated severities which you can GET at /logLevels.", example = "{\"severity\": \"error\", \"url\": \"http://example.org\", \"message\": \"Error message\"}")
    @ApiResponses({ @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = LogConstants.LOG_SEVERITY_IS_NOT_SUPPORTED) })
    public void log(final LogMessage logMessage) {
        logMessage.timestamp = Calendar.getInstance().getTimeInMillis();

        if (!doLog(logMessage)) {
            throw new NotSupportedException(String.format(TEMPLATE_INTERNAL_ERROR,
                    LogConstants.LOG_SEVERITY_IS_NOT_SUPPORTED, logMessage.severity));
        }

        LOG_BUFFER.add(logMessage);
        if (LOG_BUFFER.size() > LogConstants.LOG_BUFFER_LIMIT) {
            LOG_BUFFER.pollLast(); // Remove last element of Deque
        }
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
