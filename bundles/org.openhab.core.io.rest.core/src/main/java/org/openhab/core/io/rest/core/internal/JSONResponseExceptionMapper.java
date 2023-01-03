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
package org.openhab.core.io.rest.core.internal;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trap exceptions.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@NonNullByDefault
public class JSONResponseExceptionMapper implements ExceptionMapper<Exception> {

    private final Logger logger = LoggerFactory.getLogger(JSONResponseExceptionMapper.class);
    private final ExceptionMapper<Exception> delegate = new JSONResponse.ExceptionMapper();

    @Override
    public @Nullable Response toResponse(Exception e) {
        if (e instanceof IOException) {
            // we catch this exception to avoid confusion errors in the log file, since this is not any error situation
            // see https://github.com/openhab/openhab-distro/issues/1188
            logger.debug("Failed writing HTTP response, since other side closed the connection", e);
            // Returning null results in a Response.Status.NO_CONTENT response.
            return null;
        } else {
            logger.error("Unexpected exception occurred while processing REST request.", e);
            return delegate.toResponse(e);
        }
    }
}
