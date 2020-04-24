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
package org.openhab.core.io.rest.internal;

import javax.ws.rs.core.Application;

import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationBase;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;

/**
 * The JAX-RS application for the openHAB JAX-RS resources.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component(service = Application.class, property = {
        // https://lists.apache.org/thread.html/
        // r1379789bd90c6b7e3971d5ffeedb2e0d1e1c9103fd2392cb95458596%40%3Cuser.aries.apache.org%3E
        "servlet.init.hide-service-list-page=true" //
})
@JaxrsName(RESTConstants.JAX_RS_NAME)
@JaxrsApplicationBase("rest")
public class RESTApplicationImpl extends Application {
}
