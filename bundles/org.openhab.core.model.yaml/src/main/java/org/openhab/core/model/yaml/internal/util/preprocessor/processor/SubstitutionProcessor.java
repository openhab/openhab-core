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
package org.openhab.core.model.yaml.internal.util.preprocessor.processor;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorContext;
import org.openhab.core.model.yaml.internal.util.preprocessor.StringInterpolator;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;

/**
 * Processor for handling variable substitutions by resolving {@code SubstitutionPlaceholder} in YAML models
 * into interpolated strings.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class SubstitutionProcessor implements PlaceholderProcessor<SubstitutionPlaceholder> {

    private final PreprocessorContext preprocessorContext;

    public SubstitutionProcessor(PreprocessorContext context) {
        this.preprocessorContext = context;
    }

    /**
     * Recursively resolve all SubstitutionPlaceholders in a value (map, list, or scalar).
     * using the context's variables as the substitution context.
     *
     * @param value The value to process
     * @return The processed value with substitutions applied
     */
    @Override
    public @Nullable Object process(SubstitutionPlaceholder placeholder) {
        return process(placeholder, preprocessorContext.getVariables());
    }

    /**
     * Recursively resolve all SubstitutionPlaceholders in a value (map, list, or scalar)
     * using the provided context for substitutions.
     *
     * This overload is needed by {@link InsertProcessor} to apply substitutions
     * with a custom variable context when processing templates.
     *
     * @param value The value to process
     * @param context The variable context for substitutions
     * @return The processed value with substitutions applied
     */
    public @Nullable Object process(SubstitutionPlaceholder placeholder, Map<String, @Nullable Object> context) {
        try {
            return StringInterpolator.interpolate(placeholder.value(), placeholder.pattern(), context,
                    preprocessorContext.getLogger().getLogSession(), placeholder.sourceLocation());
        } catch (IllegalArgumentException e) {
            preprocessorContext.getLogger().warn("{} {}", placeholder.sourceLocation(), e.getMessage());
            return null;
        }
    }
}
