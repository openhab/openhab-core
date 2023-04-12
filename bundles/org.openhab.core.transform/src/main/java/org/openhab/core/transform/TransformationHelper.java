/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class TransformationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationHelper.class);

    public static final String FUNCTION_VALUE_DELIMITER = ":";

    /* RegEx to extract and parse a function String <code>'(.*?)\((.*)\):(.*)'</code> */
    protected static final Pattern EXTRACT_TRANSFORMFUNCTION_PATTERN = Pattern
            .compile("(.*?)\\((.*)\\)" + FUNCTION_VALUE_DELIMITER + "(.*)");

    /**
     * determines whether a pattern refers to a transformation service
     *
     * @param pattern the pattern to check
     * @return true, if the pattern contains a transformation
     */
    public static boolean isTransform(String pattern) {
        return EXTRACT_TRANSFORMFUNCTION_PATTERN.matcher(pattern).matches();
    }

    /**
     * Queries the OSGi service registry for a service that provides a transformation service of
     * a given transformation type (e.g. REGEX, XSLT, etc.)
     *
     * @param context the bundle context which can be null
     * @param transformationType the desired transformation type
     * @return a service instance or null, if none could be found
     */
    public static @Nullable TransformationService getTransformationService(@Nullable BundleContext context,
            String transformationType) {
        if (context != null) {
            String filter = "(" + TransformationService.SERVICE_PROPERTY_NAME + "=" + transformationType + ")";
            try {
                Collection<ServiceReference<TransformationService>> refs = context
                        .getServiceReferences(TransformationService.class, filter);
                if (refs != null && !refs.isEmpty()) {
                    return context.getService(refs.iterator().next());
                } else {
                    LOGGER.debug("Cannot get service reference for transformation service of type {}",
                            transformationType);
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.debug("Cannot get service reference for transformation service of type {}", transformationType,
                        e);
            }
        }
        return null;
    }

    /**
     * Transforms a state string using transformation functions within a given pattern.
     *
     * @param context a valid bundle context, required for accessing the services
     * @param stateDescPattern the pattern that contains the transformation instructions
     * @param state the state to be formatted before being passed into the transformation function
     * @return the result of the transformation. If no transformation was done, <code>null</code> is returned
     * @throws TransformationException if transformation service is not available or the transformation failed
     */
    public static @Nullable String transform(BundleContext context, String stateDescPattern, String state)
            throws TransformationException {
        Matcher matcher = EXTRACT_TRANSFORMFUNCTION_PATTERN.matcher(stateDescPattern);
        if (matcher.find()) {
            String type = matcher.group(1);
            String pattern = matcher.group(2);
            String value = matcher.group(3);
            TransformationService transformation = TransformationHelper.getTransformationService(context, type);
            if (transformation != null) {
                return transform(transformation, pattern, value, state);
            } else {
                throw new TransformationException("Couldn't transform value because transformation service of type '"
                        + type + "' is not available.");
            }
        } else {
            return state;
        }
    }

    /**
     * Transforms a state string using a transformation service
     *
     * @param service the {@link TransformationService} to be used
     * @param function the function containing the transformation instruction
     * @param format the format the state should be converted to before transformation
     * @param state the state to be formatted before being passed into the transformation function
     * @return the result of the transformation. If no transformation was done, <code>null</code> is returned
     * @throws TransformationException if transformation service fails or the state cannot be formatted according to the
     *             format
     */
    public static @Nullable String transform(TransformationService service, String function, String format,
            String state) throws TransformationException {
        try {
            String value = String.format(format, state);
            return service.transform(function, value);
        } catch (IllegalFormatException e) {
            throw new TransformationException("Cannot format state '" + state + "' to format '" + format + "'", e);
        }
    }
}
