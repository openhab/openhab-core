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
package org.openhab.core.model.yaml.internal.util.preprocessor.placeholders;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor;

/**
 * The {@link IncludePlaceholder} represents an object constructed from an <code>!include</code> node
 * to be processed by the {@link YamlPreprocessor}.
 *
 * @param fileName The name of the file to be included
 * @param vars The variables to be passed to the included file
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public record IncludePlaceholder(Object fileName, Map<Object, Object> vars) {
}
