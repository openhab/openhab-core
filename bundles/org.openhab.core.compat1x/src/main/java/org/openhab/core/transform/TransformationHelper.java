/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.transform;

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class TransformationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationHelper.class);

    /**
     * Queries the OSGi service registry for a service that provides a transformation service of
     * a given transformation type (e.g. REGEX, XSLT, etc.)
     *
     * @param transformationType the desired transformation type
     * @return a service instance or null, if none could be found
     */
    public static TransformationService getTransformationService(BundleContext context, String transformationType) {
        if (context != null) {
            String filter = "(smarthome.transform=" + transformationType + ")";
            try {
                Collection<ServiceReference<org.eclipse.smarthome.core.transform.TransformationService>> refs = context
                        .getServiceReferences(org.eclipse.smarthome.core.transform.TransformationService.class, filter);
                if (refs != null && refs.size() > 0) {
                    return new TransformationServiceDelegate(context.getService(refs.iterator().next()));
                } else {
                    LOGGER.warn("Cannot get service reference for transformation service of type {}",
                            transformationType);
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.warn("Cannot get service reference for transformation service of type {}", transformationType,
                        e);
            }
        }
        return null;
    }

    private static class TransformationServiceDelegate implements TransformationService {

        org.eclipse.smarthome.core.transform.TransformationService delegate;

        public TransformationServiceDelegate(org.eclipse.smarthome.core.transform.TransformationService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String transform(String function, String source) throws TransformationException {
            try {
                return delegate.transform(function, source);
            } catch (org.eclipse.smarthome.core.transform.TransformationException e) {
                throw new TransformationException(e.getMessage());
            }
        }

    }
}
