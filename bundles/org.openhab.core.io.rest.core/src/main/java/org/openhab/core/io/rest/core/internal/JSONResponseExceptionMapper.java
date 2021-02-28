/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
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
public class JSONResponseExceptionMapper implements ExceptionMapper<@NonNull Exception> {

    private final Logger logger = LoggerFactory.getLogger(JSONResponseExceptionMapper.class);
    private final ExceptionMapper<Exception> delegate = new JSONResponse.ExceptionMapper();

    @Override
    public Response toResponse(Exception e) {
        logger.error("Unexpected exception occurred while processing REST request.", e);
        return delegate.toResponse(e);
    }
}
