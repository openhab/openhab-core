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
package org.openhab.core.model.yaml.internal.util.preprocessor.placeholder;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor;

/**
 * The {@link SubstitutionPlaceholder} represents a deferred string interpolation constructed from a <code>!sub</code>
 * node to be processed by the {@link YamlPreprocessor}.
 *
 * <p>
 * It preserves the raw scalar value and the delimiter pattern that was active (from !sub or parent !sub).
 *
 * @param value The raw string value containing variable interpolation patterns
 * @param pattern The effective pattern for identifying variable interpolation expressions
 * @param sourceLocation Description of the source location for logging purposes
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public record SubstitutionPlaceholder(@NonNull String value, @NonNull Pattern pattern,
        @NonNull String sourceLocation) implements Placeholder {
}
