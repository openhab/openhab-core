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
package org.openhab.core.io.rest.sse.internal;

import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.media.sse.SseFeature;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for openHAB SSE bundle.
 *
 * @author Ivan Iliev - Initial contribution
 */
public class SseActivator implements BundleActivator {

    // See: https://github.com/openhab/openhab-core/issues/597
    private static class WorkAroundIssue597 {
        public static RuntimeDelegate getRuntimeDelegate(final long millisMax, final long millisSleep)
                throws InterruptedException {
            final Logger logger = LoggerFactory.getLogger(WorkAroundIssue597.class);

            logger.trace("get runtime delegate");
            final long begMillis = System.currentTimeMillis();
            do {
                try {
                    final RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();
                    logger.trace("succeeded");
                    return runtimeDelegate;
                } catch (final LinkageError ex) {
                    logger.trace("linkage error");
                    if (System.currentTimeMillis() - begMillis <= millisMax) {
                        Thread.sleep(millisSleep);
                    }
                }
            } while (System.currentTimeMillis() - begMillis <= millisMax);
            logger.trace("give up");
            return null;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(SseActivator.class);

    private static BundleContext context;

    private ServiceRegistration<?> sseFeatureRegistration;

    private ServiceRegistration<?> blockingAsyncFeatureRegistration;

    /**
     * Called whenever the OSGi framework starts our bundle
     */
    @Override
    public void start(BundleContext bc) throws Exception {
        context = bc;

        WorkAroundIssue597.getRuntimeDelegate(5000, 200);
        String featureName = SseFeature.class.getName();
        if (bc.getServiceReference(featureName) == null) {
            sseFeatureRegistration = bc.registerService(featureName, new SseFeature(), null);

            logger.debug("SSE API - SseFeature registered.");
        }

        logger.debug("SSE API has been started.");
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    @Override
    public void stop(BundleContext bc) throws Exception {
        context = null;

        if (sseFeatureRegistration != null) {
            sseFeatureRegistration.unregister();
            logger.debug("SseFeature unregistered.");
        }

        if (blockingAsyncFeatureRegistration != null) {
            blockingAsyncFeatureRegistration.unregister();
            logger.debug("BlockingAsyncFeature unregistered.");
        }

        logger.debug("SSE API has been stopped.");
    }

    /**
     * Returns the bundle context of this bundle
     *
     * @return the bundle context
     */
    public static BundleContext getContext() {
        return context;
    }
}
