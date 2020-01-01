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
package org.openhab.core.io.http.internal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.core.io.http.Handler;
import org.openhab.core.io.http.WrappingHttpContext;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;

/**
 * Default http context implementation which groups all Smart Home related http elements into one logical application.
 *
 * Additionally to standard http context, this one provides its own implementation of
 * {@link #handleSecurity(HttpServletRequest, HttpServletResponse)} method which is based on injected list of generic
 * handlers.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@Component(service = { HttpContext.class, WrappingHttpContext.class }, property = {
        "httpContext.id:String=oh-dfl-http-ctx" })
public class SmartHomeHttpContext implements WrappingHttpContext {

    /**
     * Sorted list of handlers, where handler with priority 0 is first.
     */
    private final List<Handler> handlers = new CopyOnWriteArrayList<>();

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Deque<Handler> queue = new ArrayDeque<>(handlers);
        DefaultHandlerContext handlerContext = new DefaultHandlerContext(queue);
        handlerContext.execute(request, response);

        if (handlerContext.hasError()) {
            return false;
        }

        return true;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

    @Override
    public HttpContext wrap(Bundle bundle) {
        return new BundleHttpContext(this, bundle);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addHandler(Handler handler) {
        this.handlers.add(handler);
        Collections.sort(handlers, Comparator.comparingInt(Handler::getPriority));
    }

    public void removeHandler(Handler handler) {
        this.handlers.remove(handler);
    }

}
