/**
 * Copyright (c) 2015-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private static Logger logger = LoggerFactory.getLogger(TransformationHelper.class);

    /**
     * Queries the OSGi service registry for a service that provides a transformation service of
     * a given transformation type (e.g. REGEX, XSLT, etc.)
     *
     * @param transformationType the desired transformation type
     * @return a service instance or null, if none could be found
     */
    static public TransformationService getTransformationService(BundleContext context, String transformationType) {
        if (context != null) {
            String filter = "(smarthome.transform=" + transformationType + ")";
            try {
                Collection<ServiceReference<org.eclipse.smarthome.core.transform.TransformationService>> refs = context
                        .getServiceReferences(org.eclipse.smarthome.core.transform.TransformationService.class, filter);
                if (refs != null && refs.size() > 0) {
                    return new TransformationServiceDelegate(context.getService(refs.iterator().next()));
                } else {
                    logger.warn("Cannot get service reference for transformation service of type {}",
                            transformationType);
                }
            } catch (InvalidSyntaxException e) {
                logger.warn("Cannot get service reference for transformation service of type {}", transformationType,
                        e);
            }
        }
        return null;
    }

    static private class TransformationServiceDelegate implements TransformationService {

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
