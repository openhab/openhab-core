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
package org.openhab.core.semantics;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a representation of an item semantics configuration problem.
 *
 * @param item item with problem
 * @param semanticType item semantic type
 * @param reason description for the item semantics configuration problem
 * @param explanation longer explanation of problem
 *
 * @author Mark Herwege - Persistence health API endpoint
 */
@NonNullByDefault
public record ItemSemanticsProblem(String item, @Nullable String semanticType, String reason,
        @Nullable String explanation) {
}
