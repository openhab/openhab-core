/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.http.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.smarthome.io.http.Handler;
import org.eclipse.smarthome.io.http.HandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated kind of delegate handler which provides error handling.
 *
 * Each exception is set back on HandlerContext allowing HandlerChain to process it. When there is already error set in
 * current processing pipeline thrown exception is logged and ignored.
 *
 * @author Łukasz Dywicki - Initial contribution and API.
 */
public class CatchHandler implements Handler {

    private final Logger logger = LoggerFactory.getLogger(CatchHandler.class);
    private final Handler delegate;

    public CatchHandler(Handler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        try {
            delegate.handle(request, response, context);
        } catch (Exception e) {
            if (!context.hasError()) {
                context.error(e);
            } else {
                logger.error("Could not handle exception thrown by delegate handler {}", delegate, e);
            }
        }
    }

    @Override
    public void handleError(HttpServletRequest request, HttpServletResponse response, HandlerContext context) {
        delegate.handleError(request, response, context);
    }

}
