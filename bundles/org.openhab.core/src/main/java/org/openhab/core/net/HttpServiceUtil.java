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
package org.openhab.core.net;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some utility functions related to the http service
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class HttpServiceUtil {

    private HttpServiceUtil() {
    }

    /**
     * Get the port that is used by the HTTP service.
     *
     * @param bc the bundle context used for lookup
     * @return the port if used, -1 if no port could be found
     */
    public static int getHttpServicePort(final BundleContext bc) {
        return getHttpServicePortProperty(bc, "org.osgi.service.http.port");
    }

    public static int getHttpServicePortSecure(final BundleContext bc) {
        return getHttpServicePortProperty(bc, "org.osgi.service.http.port.secure");
    }

    // Utility method that could be used for non-secure and secure port.
    private static int getHttpServicePortProperty(final BundleContext bc, final String propertyName) {
        Object value;
        int port = -1;

        // Try to find the port by using the service property (respect service ranking).
        final ServiceReference<?>[] refs;
        try {
            refs = bc.getAllServiceReferences("org.osgi.service.http.HttpService", null);
        } catch (final InvalidSyntaxException ex) {
            // This point of code should never be reached.
            final Logger logger = LoggerFactory.getLogger(HttpServiceUtil.class);
            logger.warn("This error should only be thrown if a filter could not be parsed. We don't use a filter...");
            return -1;
        }

        if (refs != null) {
            int candidate = Integer.MIN_VALUE;
            for (final ServiceReference<?> ref : refs) {
                value = ref.getProperty(propertyName);
                if (value == null) {
                    continue;
                }
                final int servicePort;
                try {
                    servicePort = Integer.parseInt(value.toString());
                } catch (final NumberFormatException ex) {
                    continue;
                }
                value = ref.getProperty(Constants.SERVICE_RANKING);
                final int serviceRanking;
                if (value == null || !(value instanceof Integer)) {
                    serviceRanking = 0;
                } else {
                    serviceRanking = (Integer) value;
                }
                if (serviceRanking >= candidate) {
                    candidate = serviceRanking;
                    port = servicePort;
                }
            }
        }
        if (port > 0) {
            return port;
        }

        // If the service does not provide the port, try to use the system property.
        value = bc.getProperty(propertyName);
        if (value != null) {
            if (value instanceof String) {
                try {
                    return Integer.parseInt(value.toString());
                } catch (final NumberFormatException ex) {
                    // If the property could not be parsed, the HTTP servlet itself has to care and warn about.
                }
            } else if (value instanceof Integer) {
                return (Integer) value;
            }
        }

        return -1;
    }

}
