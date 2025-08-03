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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import java.util.Map;

/**
 * The {@link IncludeObject} represents an object constructed from an <code>!include</code> node
 * to be processed by the {@link YamlPreprocessor}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
record IncludeObject(String fileName, Map<String, String> vars) {
}
