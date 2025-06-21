/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Extension of standard {@link HttpContext} interface which allows creation of "sub contexts".
 * These sub contexts are nothing else but custom resource locators which provide new files to host, but should not
 * influence overall processing logic of
 * {@link #handleSecurity(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} and
 * {@link #getMimeType(String)}.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
public interface WrappingHttpContext extends HttpContext {

    /**
     * Creates new http context which hosts resources from given bundle.
     *
     * @param bundle Bundle with resources.
     * @return New context instance.
     */
    HttpContext wrap(@Nullable Bundle bundle);
}
