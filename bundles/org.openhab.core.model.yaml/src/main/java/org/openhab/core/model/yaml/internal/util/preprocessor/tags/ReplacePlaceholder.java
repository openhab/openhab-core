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

import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor;

/**
 * The {@link ReplacePlaceholder} represents an object constructed from a <code>!replace</code> node
 * to be processed by the {@link YamlPreprocessor}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public record ReplacePlaceholder(Object object) {
}
