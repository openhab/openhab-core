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
 * A {@link Selector} that matches items by tag. Expression: {@code "tag:<tagName>"}
 * <p>
 * The simple {@link #matches(String)} always returns {@code false} — tag matching
 * requires entity access, resolved by the {@code ItemPermissionEvaluator} in Phase 1.
 *
 * @author Gabor Bicskei - Initial contribution
 *
 * @param tag the tag name
 */
@NonNullByDefault
public record ByTagSelector(String tag) implements Selector {

    @Override
    public boolean matches(String resourceId) {
        return false;
    }

    @Override
    public String expression() {
        return "tag:" + tag;
    }
}
