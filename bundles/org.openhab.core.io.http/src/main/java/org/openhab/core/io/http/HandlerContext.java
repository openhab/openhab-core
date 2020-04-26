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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler context represents a present state of all handlers placed in execution chain.
 *
 * There are two basic operations located in this type - first allows to continue execution and let next handler be
 * called. Second is intended to break the chain and force handlers to process error and generate error response.
 *
 * When Handler decide to not call context by delegating further handler to get called via
 * {@link #execute(HttpServletRequest, HttpServletResponse)} nor {@link #error(Exception)} then chain is stopped.
 * By this simple way handlers can decide to hold processing and generate own response.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
public interface HandlerContext {

    String ERROR_ATTRIBUTE = "handler.error";

    /**
     * Delegate execution to next handler in the chain, if available.
     *
     * When current handler is last in processing queue then nothing happens, execution chain returns to its caller.
     *
     * @param request Request.
     * @param response Response.
     */
    void execute(HttpServletRequest request, HttpServletResponse response);

    /**
     * Signal that an error occurred during handling of request.
     *
     * Call to this method will break normal execution chain and force handling of error.
     */
    void error(Exception error);

    /**
     * Checks if has any errors occurred while handling request.
     *
     * @return True if an exception occurred while handling request.
     */
    boolean hasError();
}
