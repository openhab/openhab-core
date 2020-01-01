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
package org.openhab.core.io.http.auth.internal;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.core.auth.Authentication;
import org.openhab.core.io.http.Handler;
import org.openhab.core.io.http.HandlerContext;
import org.openhab.core.io.http.HandlerPriorities;
import org.osgi.service.component.annotations.Component;

/**
 * Handler located after authentication which redirect client to page from which he started authentication process.
 *
 * @author Łukasz Dywicki - Initial contribution.
 */
@Component
public class RedirectHandler implements Handler {

    @Override
    public int getPriority() {
        return HandlerPriorities.AUTHENTICATION + 10;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        Optional<Authentication> authhentication = Optional
                .ofNullable(request.getAttribute(Authentication.class.getName()))
                .filter(Authentication.class::isInstance).map(Authentication.class::cast);

        Optional<String> redirect = Optional
                .ofNullable(request.getParameter(AuthenticationHandler.REDIRECT_PARAM_NAME));

        if (authhentication.isPresent() && redirect.isPresent()) {
            response.setHeader("Location", redirect.get());
        }

        context.execute(request, response);
    }

    @Override
    public void handleError(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        context.execute(request, response);
    }

}
