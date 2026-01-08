/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.util.preprocessor.tags;

import java.util.regex.Pattern;

import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor;

/**
 * The {@link SubstitutionPlaceholder} represents a deferred string interpolation constructed from a <code>!sub</code>
 * node to be processed by the {@link YamlPreprocessor}.
 *
 * <p>
 * It preserves the raw scalar value and the delimiter pattern that was active (from !sub or parent !sub).
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public record SubstitutionPlaceholder(String value, Pattern pattern, boolean isPlainScalar) {
}
