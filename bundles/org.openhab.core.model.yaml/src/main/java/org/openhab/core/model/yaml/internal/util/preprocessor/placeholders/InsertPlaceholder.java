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
 * The {@link InsertPlaceholder} represents an object constructed from an <code>!insert</code> node
 * to be processed by the {@link YamlPreprocessor}.
 *
 * @param template The template object to be inserted
 * @param vars The variables to be passed to the inserted template
 * @param contextDescription A description of the context where this placeholder was created (for logging/debugging)
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public record InsertPlaceholder(Object template, Map<Object, Object> vars, String contextDescription) {
}
