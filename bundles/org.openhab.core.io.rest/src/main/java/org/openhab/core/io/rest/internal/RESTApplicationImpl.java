/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.rest.internal;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationBase;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;

import jakarta.ws.rs.core.Application;

/**
 * The JAX-RS application for the openHAB JAX-RS resources.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component(service = Application.class)
@JakartarsName(RESTConstants.JAX_RS_NAME)
@JakartarsApplicationBase("rest")
@NonNullByDefault
public class RESTApplicationImpl extends Application {

    @Override
    @NonNullByDefault({})
    public Map<String, Object> getProperties() {
        // Disabling WADL takes over the role of the servlet.init.hide-service-list-page property that used to be set
        // for the Apache Aries JAX-RS Whiteboard: it keeps the resources from being listed publicly. It also silences
        // "JAXBContext implementation could not be found. WADL feature is disabled." on startup. The API is described
        // by OpenAPI instead.
        return Map.of("jersey.config.server.wadl.disableWadl", true);
    }
}
