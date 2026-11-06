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
 * Factory that parses selector expression strings into typed {@link Selector} instances.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public final class SelectorParser {

    private SelectorParser() {
    }

    /**
     * Parses a selector expression string into a typed {@link Selector}.
     * <p>
     * Note: Selector expressions are <b>case-sensitive</b> — item names, group names, tag names, and
     * location names must match exactly. This contrasts with resource types and actions, which are
     * case-insensitive.
     *
     * @param expression the expression (e.g., {@code "*"}, {@code "id:LivingRoom_Light"}, {@code "group:gLights"})
     * @return the parsed selector
     * @throws IllegalArgumentException if the expression is not recognized
     */
    public static Selector parse(String expression) {
        if ("*".equals(expression)) {
            return AllSelector.INSTANCE;
        }
        int colon = expression.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Invalid selector expression: " + expression);
        }
        String prefix = expression.substring(0, colon);
        String value = expression.substring(colon + 1);
        return switch (prefix) {
            case "id" -> new ByIdSelector(value);
            case "group" -> new ByGroupSelector(value);
            case "tag" -> new ByTagSelector(value);
            case "location" -> new ByLocationSelector(value);
            default -> throw new IllegalArgumentException(
                    "Unknown selector prefix: " + prefix + ". Valid prefixes: id, group, tag, location");
        };
    }
}
