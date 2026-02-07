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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.PlaceholderProcessor;

/**
 * A signal returned by {@link PlaceholderProcessor}s to indicate that the entry
 * currently being processed should be removed from its parent container (Map or List).
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public enum RemovalSignal {
    /**
     * Remove the entry from the parent container.
     */
    REMOVE;
}
