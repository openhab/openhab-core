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
package org.openhab.core.thing.binding.generic;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
public class ChannelTransformation {
    private final Logger logger = LoggerFactory.getLogger(ChannelTransformation.class);
    private List<TransformationStep> transformationSteps;

    public ChannelTransformation(@Nullable String transformationString) {
        if (transformationString != null) {
            try {
                transformationSteps = Arrays.stream(transformationString.split("∩")).filter(s -> !s.isBlank())
                        .map(TransformationStep::new).toList();
                return;
            } catch (IllegalArgumentException e) {
                logger.warn("Transformation ignored, failed to parse {}: {}", transformationString, e.getMessage());
            }
        }
        transformationSteps = List.of();
    }

    public Optional<String> apply(String value) {
        Optional<String> valueOptional = Optional.of(value);

        // process all transformations
        for (TransformationStep transformationStep : transformationSteps) {
            valueOptional = valueOptional.flatMap(transformationStep::apply);
        }

        logger.trace("Transformed '{}' to '{}' using '{}'", value, valueOptional, transformationSteps);
        return valueOptional;
    }

    private static class TransformationStep {
        private final Logger logger = LoggerFactory.getLogger(TransformationStep.class);
        private final String serviceName;
        private final String function;

        public TransformationStep(String pattern) throws IllegalArgumentException {
            int index = pattern.indexOf(":");
            if (index == -1) {
                throw new IllegalArgumentException(
                        "The transformation pattern must consist of the type and the pattern separated by a colon");
            }
            this.serviceName = pattern.substring(0, index).toUpperCase().trim();
            this.function = pattern.substring(index + 1).trim();
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
