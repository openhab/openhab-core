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
package org.openhab.core.io.http.auth.basic.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.core.io.http.Handler;
import org.openhab.core.io.http.HandlerContext;
import org.openhab.core.io.http.HandlerPriorities;
import org.osgi.service.component.annotations.Component;

/**
 * A handler which forces basic auth when authentication fails.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@Component
public class BasicChallengeHandler implements Handler {

    @Override
    public int getPriority() {
        return HandlerPriorities.AUTHENTICATION + 1;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        context.execute(request, response);
    }

    @Override
    public void handleError(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        response.setHeader("WWW-Authenticate", "Basic realm=\"Please enter user name and password to access system\"");
        response.setStatus(401);

        context.execute(request, response);
    }

}
