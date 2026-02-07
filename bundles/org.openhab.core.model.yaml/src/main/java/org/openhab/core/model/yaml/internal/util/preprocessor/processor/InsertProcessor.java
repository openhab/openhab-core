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
import org.openhab.core.model.yaml.internal.util.preprocessor.core.RecursiveProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.InsertPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;

/**
 * Processor for resolving {@link InsertPlaceholder} in YAML models
 * into template content with local variable substitution.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class InsertProcessor implements PlaceholderProcessor<InsertPlaceholder> {

    private final PreprocessorContext context;
    private final RecursiveProcessor recursiveProcessor;
    private final SubstitutionProcessor substitutionProcessor;

    public InsertProcessor(PreprocessorContext context, RecursiveProcessor recursiveProcessor,
            SubstitutionProcessor substitutionProcessor) {
        this.context = context;
        this.recursiveProcessor = recursiveProcessor;
        this.substitutionProcessor = substitutionProcessor;
    }

    @Override
    public @Nullable Object process(InsertPlaceholder data) {
        return process(data, null);
    }

    /**
     * Resolves an {@link InsertPlaceholder} recursively
     * following any nested inserts, and returns the fully expanded content.
     */
    private @Nullable Object process(InsertPlaceholder placeholder, @Nullable Object defaultValue) {
        FragmentUtils.Parameters params = FragmentUtils.parseParameters(placeholder, "template");
        if (params == null) {
            context.getLogger().warn("{} Failed to process !insert: invalid parameters", placeholder.sourceLocation());
            return defaultValue;
        }

        String templateName = params.name();
        if (templateName == null || templateName.isBlank()) {
            context.getLogger().warn("{} Failed to process !insert: missing template name",
                    placeholder.sourceLocation());
            return defaultValue;
        }
        Map<String, @Nullable Object> templateVariables = FragmentUtils.combineInjectedVars(params.varsMap(),
                context.getVariables());
        Object templateObj = context.getTemplates().get(templateName);
        if (templateObj == null) {
            context.getLogger().warn("{} Failed to process !insert '{}': template not found",
                    placeholder.sourceLocation(), templateName);
            return defaultValue;
        }
        // The substitution placeholders in the template are resolved using the templateVariables context
        // unlike any other processing which uses the main variables map
        Object resolvedTemplate = recursiveProcessor.process(templateObj, SubstitutionPlaceholder.class, (p) -> {
            if (p instanceof SubstitutionPlaceholder substitutionPlaceholder) {
                return substitutionProcessor.process(substitutionPlaceholder, templateVariables);
            }
            return null;
        });
        return resolvedTemplate;
    }
}
