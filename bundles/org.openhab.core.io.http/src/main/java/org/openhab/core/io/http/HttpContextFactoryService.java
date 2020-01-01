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
package org.openhab.core.io.http;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Create {@link HttpContext} instances when registering servlets, resources or filters using the
 * {@link HttpService#registerServlet} and corresponding methods.
 *
 * @author Henning Treu - Initial contribution
 */
public interface HttpContextFactoryService {

    /**
     * Creates an {@link HttpContext} according to the OSGi specification of
     * {@link HttpService#createDefaultHttpContext()}.
     *
     * @param bundle the bundle which will be used by this {@link HttpContext} to resolve resources.
     * @return the {@link HttpContext} for the given bundle.
     */
    HttpContext createDefaultHttpContext(Bundle bundle);

}
