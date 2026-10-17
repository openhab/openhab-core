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
        // Ask Jersey not to publish a WADL of the resources, the API is described by OpenAPI instead. This states the
        // intent, but does not take effect at the moment: the Eclipse Jakarta REST Whiteboard builds its Jersey
        // configuration without the properties of this application, so WadlFeature does not see the property. It
        // disables itself anyway, because Jersey finds no JAXB implementation, and logs "JAXBContext implementation
        // could not be found. WADL feature is disabled." while doing so. Should a JAXB implementation ever become
        // visible to Jersey, WADL would be published unless the whiteboard passes this property on by then.
        return Map.of("jersey.config.server.wadl.disableWadl", true);
    }
}
