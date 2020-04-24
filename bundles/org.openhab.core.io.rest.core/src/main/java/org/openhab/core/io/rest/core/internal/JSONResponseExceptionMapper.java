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
package org.openhab.core.io.rest.core.internal;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;

/**
 * Trap exceptions.
 *
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
public class JSONResponseExceptionMapper implements ExceptionMapper<Exception> {

    private final ExceptionMapper<Exception> delegate = new JSONResponse.ExceptionMapper();

    @Override
    public Response toResponse(Exception e) {
        return delegate.toResponse(e);
    }
}
