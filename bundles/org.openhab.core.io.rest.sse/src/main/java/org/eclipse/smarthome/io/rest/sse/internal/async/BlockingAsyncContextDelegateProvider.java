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
package org.eclipse.smarthome.io.rest.sse.internal.async;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.smarthome.io.rest.sse.internal.util.SseUtil;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegateProvider;

/**
 * An {@link AsyncContextDelegateProvider} implementation that returns a
 * blocking {@link AsyncContextDelegate}, which blocks while the connection is
 * alive if the response content-type is {@link SseFeature #SERVER_SENT_EVENTS} or throws an
 * UnsupportedOperationException otherwise. The blocking continues
 * until the response can longer be written to.
 *
 * @author Ivan Iliev - Initial Contribution and API
 *
 */
public class BlockingAsyncContextDelegateProvider implements AsyncContextDelegateProvider {

    @Override
    public final AsyncContextDelegate createDelegate(final HttpServletRequest request,
            final HttpServletResponse response) {
        return new BlockingAsyncContextDelegate(request, response);
    }

    private static final class BlockingAsyncContextDelegate implements AsyncContextDelegate {
        private static final int PING_TIMEOUT = 15 * 1000;

        private final HttpServletResponse response;

        private volatile boolean isRunning;

        private BlockingAsyncContextDelegate(final HttpServletRequest request, final HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public void complete() {
            isRunning = false;
        }

        @Override
        public void suspend() throws IllegalStateException {
            if (SseUtil.shouldAsyncBlock()) {
                isRunning = true;

                synchronized (this) {
                    while (isRunning) {
                        try {
                            this.wait(PING_TIMEOUT);
                            ServletOutputStream outputStream = response.getOutputStream();

                            // write a new line to the OutputStream and flush to
                            // check connectivity. If the other peer closes the
                            // connection, the first flush() should generate a
                            // TCP reset that is detected on the second flush()
                            outputStream.write('\n');
                            response.flushBuffer();

                            outputStream.write('\n');
                            response.flushBuffer();
                        } catch (Exception exception) {
                            // If an exception has occurred during write and
                            // flush we consider the connection closed, attempt
                            // to close the outputstream and stop blocking.
                            try {
                                response.getOutputStream().close();
                            } catch (IOException e) {
                            }

                            isRunning = false;
                        }
                    }
                }
            } else {
                throw new UnsupportedOperationException("ASYNCHRONOUS PROCESSING IS NOT SUPPORTED!");
            }
        }
    }
}
