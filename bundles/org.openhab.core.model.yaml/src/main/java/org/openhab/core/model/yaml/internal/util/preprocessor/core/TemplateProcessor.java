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
package org.openhab.core.model.yaml.internal.util.preprocessor.core;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorConfig;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorContext;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;

/**
 * The {@link TemplateProcessor} is responsible for extracting templates from the YAML model and storing them in the
 * preprocessor context for later use.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class TemplateProcessor {
    private final PreprocessorContext context;
    private final RecursiveProcessor recursiveProcessor;
    private final MergeKeyProcessor mergeKeyProcessor;
    private final SourceLocator locator;

    public TemplateProcessor(PreprocessorContext context, RecursiveProcessor recursiveProcessor,
            MergeKeyProcessor mergeKeyProcessor, SourceLocator locator) {
        this.context = context;
        this.recursiveProcessor = recursiveProcessor;
        this.mergeKeyProcessor = mergeKeyProcessor;
        this.locator = locator;
    }

    /**
     * Extracts templates from the given map and stores them into the templates map.
     *
     * @param templatesSection the section of the YAML model containing templates
     */
    public void extractTemplates(@Nullable Object templatesSection) {
        if (templatesSection instanceof java.util.Map<?, ?> templatesMap) {
            templatesMap.keySet().removeIf(Objects::isNull);
            mergeKeyProcessor.resolveMergeKeys(templatesMap);
            templatesMap.forEach((key, value) -> {
                Object resolvedKey = recursiveProcessor.process(key, SubstitutionPlaceholder.class);
                if (resolvedKey != null) {
                    context.getTemplates().put(resolvedKey, value);
                }
            });
        } else if (templatesSection != null) {
            var position = locator.findPosition(PreprocessorConfig.TEMPLATES_KEY);
            context.getLogger().warn("{}:{} 'templates' is not a map", context.getRelativePath(), position);
        }
    }
}
