/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.IllegalFormatException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan N. Klug - Refactored to OSGi service
 */
@Component(immediate = true)
@NonNullByDefault
public class TransformationHelper {
    private static final Map<String, TransformationService> SERVICES = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationHelper.class);

    public static final String FUNCTION_VALUE_DELIMITER = ":";

    /* RegEx to extract and parse a function String <code>'(.*?)\((.*)\):(.*)'</code> */
    protected static final Pattern EXTRACT_TRANSFORMFUNCTION_PATTERN = Pattern
            .compile("(.*?)\\((.*)\\)" + FUNCTION_VALUE_DELIMITER + "(.*)");

    private final BundleContext bundleContext;

    @Activate
    public TransformationHelper(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        SERVICES.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void setTransformationService(ServiceReference<TransformationService> ref) {
        String key = (String) ref.getProperty(TransformationService.SERVICE_PROPERTY_NAME);
        TransformationService service = bundleContext.getService(ref);
        if (service != null) {
            SERVICES.put(key, service);
            LOGGER.debug("Added transformation service {}", key);
        }
    }

    public void unsetTransformationService(ServiceReference<TransformationService> ref) {
        String key = (String) ref.getProperty(TransformationService.SERVICE_PROPERTY_NAME);
        if (SERVICES.remove(key) != null) {
            LOGGER.debug("Removed transformation service {}", key);
        }
    }

    /**
     * determines whether a pattern refers to a transformation service
     *
     * @param pattern the pattern to check
     * @return true, if the pattern contains a transformation
     */
    public static boolean isTransform(String pattern) {
        return EXTRACT_TRANSFORMFUNCTION_PATTERN.matcher(pattern).matches();
    }

    public static @Nullable TransformationService getTransformationService(String serviceName) {
        return SERVICES.get(serviceName);
    }

    /**
     * Return the transformation service that provides a given transformation type (e.g. REGEX, XSLT, etc.)
     *
     * @param context the bundle context which can be null
     * @param transformationType the desired transformation type
     * @return a service instance or null, if none could be found
     *
     * @deprecated use {@link #getTransformationService(String)} instead
     */
    @Deprecated
    public static @Nullable TransformationService getTransformationService(@Nullable BundleContext context,
            String transformationType) {
        return getTransformationService(transformationType);
    }

    /**
     * Transforms a state string using transformation functions within a given pattern.
     *
     * @param context a valid bundle context, required for accessing the services
     * @param transformationString the pattern that contains the transformation instructions
     * @param state the state to be formatted before being passed into the transformation function
     * @return the result of the transformation. If no transformation was done, <code>null</code> is returned
     * @throws TransformationException if transformation service is not available or the transformation failed
     *
     * @deprecated Use {@link #transform(String, String)} instead
     */
    @Deprecated
    public static @Nullable String transform(BundleContext context, String transformationString, String state)
            throws TransformationException {
        return transform(transformationString, state);
    }

    /**
     * Transforms a state string using transformation functions within a given pattern.
     *
     * @param transformationString the pattern that contains the transformation instructions
     * @param state the state to be formatted before being passed into the transformation function
     * @return the result of the transformation. If no transformation was done, <code>null</code> is returned
     * @throws TransformationException if transformation service is not available or the transformation failed
     */
    public static @Nullable String transform(String transformationString, String state) throws TransformationException {
        Matcher matcher = EXTRACT_TRANSFORMFUNCTION_PATTERN.matcher(transformationString);
        if (matcher.find()) {
            String type = matcher.group(1);
            String pattern = matcher.group(2);
            String value = matcher.group(3);
            TransformationService transformation = SERVICES.get(type);
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
        } catch (RuntimeException e) {
            throw new TransformationException("Transformation service threw an exception: " + e.getMessage(), e);
        }
    }
}
