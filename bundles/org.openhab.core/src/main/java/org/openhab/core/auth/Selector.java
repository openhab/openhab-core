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
package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A selector determines which resources a {@link Permission} applies to.
 * <p>
 * Simple selectors ({@link AllSelector}, {@link ByIdSelector}) can resolve
 * purely by resource ID. Entity-aware selectors ({@link ByGroupSelector},
 * {@link ByTagSelector}, {@link ByLocationSelector}) require access to the
 * full entity and are resolved by the {@link PermissionEvaluator} — their
 * {@link #matches(String)} method returns {@code false} by design.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public interface Selector {

    /**
     * Tests whether this selector matches the given resource identifier.
     * Entity-aware selectors (group, tag, location) return {@code false} here —
     * they require the full entity, resolved in the {@link PermissionEvaluator}.
     *
     * @param resourceId the resource identifier
     * @return {@code true} if this selector covers the resource by ID alone
     */
    boolean matches(String resourceId);

    /**
     * Returns the string expression form of this selector for serialization and display.
     * <p>
     * Format: {@code "*"} | {@code "id:value"} | {@code "group:value"} |
     * {@code "tag:value"} | {@code "location:value"}
     *
     * @return the expression string
     */
    String expression();
}
