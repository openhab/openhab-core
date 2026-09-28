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
package org.openhab.core.io.http;

import org.osgi.framework.Bundle;
import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * Create {@link ServletContextHelper} instances when registering servlets, resources or filters.
 *
 * @author Henning Treu - Initial contribution
 */
public interface HttpContextFactoryService {

    /**
     * Creates a {@link ServletContextHelper} according to the OSGi whiteboard specification.
     *
     * @param bundle the bundle which will be used by this {@link ServletContextHelper} to resolve resources.
     * @return the {@link ServletContextHelper} for the given bundle.
     */
    ServletContextHelper createDefaultHttpContext(Bundle bundle);
}
