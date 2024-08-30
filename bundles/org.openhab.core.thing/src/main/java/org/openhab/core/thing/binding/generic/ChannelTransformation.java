/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.thing.binding.generic;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChannelTransformation} can be used to transform an input value using one or more transformations.
 * Individual transformations can be chained with <code>∩</code> and must follow the pattern
 * <code>serviceName:function</code> where <code>serviceName</code> refers to a {@link TransformationService} and
 * <code>function</code> has to be a valid transformation function for this service
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ChannelTransformation {
    private final Logger logger = LoggerFactory.getLogger(ChannelTransformation.class);
    private List<TransformationStep> transformationSteps;

    public ChannelTransformation(@Nullable String transformationString) {
        if (transformationString != null) {
            try {
                transformationSteps = splitTransformationString(transformationString).map(TransformationStep::new)
                        .toList();
                return;
            } catch (IllegalArgumentException e) {
                logger.warn("Transformation ignored, failed to parse {}: {}", transformationString, e.getMessage());
            }
        }
        transformationSteps = List.of();
    }

    public ChannelTransformation(@Nullable List<String> transformationStrings) {
        if (transformationStrings != null) {
            try {
                transformationSteps = transformationStrings.stream()
                        .flatMap(ChannelTransformation::splitTransformationString).map(TransformationStep::new)
                        .toList();
                return;
            } catch (IllegalArgumentException e) {
                logger.warn("Transformation ignored, failed to parse {}: {}", transformationStrings, e.getMessage());
            }
        }
        transformationSteps = List.of();
    }

    private static Stream<String> splitTransformationString(String transformationString) {
        return Arrays.stream(transformationString.split("∩")).filter(s -> !s.isBlank());
    }

    /**
     * Checks whether this object contains no transformation steps.
     * 
     * @return <code>true</code> if the transformation is empty, <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return transformationSteps.isEmpty();
    }

    /**
     * Checks whether this object contains at least one transformation step.
     * 
     * @return <code>true</code> if the transformation is present, <code>false</code> otherwise.
     */
    public boolean isPresent() {
        return !isEmpty();
    }

    /**
     * Applies all transformations to the given value.
     * 
     * @param value the value to transform.
     * @return the transformed value or an empty optional if the transformation failed.
     *         If the transformation is empty, the original value is returned.
     */
    public Optional<String> apply(String value) {
        Optional<String> valueOptional = Optional.of(value);

        // process all transformations
        for (TransformationStep transformationStep : transformationSteps) {
            valueOptional = valueOptional.flatMap(v -> {
                Optional<String> result = transformationStep.apply(v);
                if (logger.isTraceEnabled()) {
                    logger.trace("Transformed '{}' to '{}' using '{}'", v, result.orElse(null), transformationStep);
                }
                return result;
            });
        }

        return valueOptional;
    }

    /**
     * Checks whether the given string contains valid transformations.
     * 
     * Valid single and chained transformations will return true.
     * 
     * @param value the transformation string to check.
     * @return <code>true</code> if the string contains valid transformations, <code>false</code> otherwise.
     */
    public static boolean isValidTransformation(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return splitTransformationString(value).map(TransformationStep::new).count() > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static class TransformationStep {
        private static final List<Pattern> TRANSFORMATION_PATTERNS = List.of( //
                Pattern.compile("(?<service>[a-zA-Z0-9]+)\\s*\\((?<function>.*)\\)$"), //
                Pattern.compile("(?<service>[a-zA-Z0-9]+)\\s*:(?<function>.*)") //
        );

        private final Logger logger = LoggerFactory.getLogger(TransformationStep.class);
        private final String serviceName;
        private final String function;

        public TransformationStep(String pattern) throws IllegalArgumentException {
            pattern = pattern.trim();
            for (Pattern p : TRANSFORMATION_PATTERNS) {
                Matcher matcher = p.matcher(pattern);
                if (matcher.matches()) {
                    this.serviceName = matcher.group("service").trim().toUpperCase();
                    this.function = matcher.group("function").trim();
                    return;
                }
            }
            throw new IllegalArgumentException(
                    "The transformation pattern must be in the syntax of TYPE:PATTERN or TYPE(PATTERN)");
        }

        public Optional<String> apply(String value) {
            TransformationService service = TransformationHelper.getTransformationService(serviceName);
            if (service != null) {
                try {
                    return Optional.ofNullable(service.transform(function, value));
                } catch (TransformationException e) {
                    logger.debug("Applying {} failed: {}", this, e.getMessage());
                }
            } else {
                logger.warn("Failed to use {}, service not found", this);
            }
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "TransformationStep{serviceName='" + serviceName + "', function='" + function + "'}";
        }
    }
}
