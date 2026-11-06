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

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Factory for resolving {@link Action} instances from string names and {@link ResourceType}s.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public final class Actions {

    private Actions() {
    }

    /**
     * Parses an action name for the given resource type.
     *
     * @param type the resource type
     * @param name the action name (case-insensitive)
     * @return the typed action
     * @throws IllegalArgumentException if the name is not valid for the resource type
     */
    public static Action parse(ResourceType type, String name) {
        String upper = name.toUpperCase();
        try {
            return switch (type) {
                case ITEM -> ItemAction.valueOf(upper);
                case PAGE -> PageAction.valueOf(upper);
                case SITEMAP -> SitemapAction.valueOf(upper);
            };
        } catch (IllegalArgumentException e) {
            Action[] valid = switch (type) {
                case ITEM -> ItemAction.values();
                case PAGE -> PageAction.values();
                case SITEMAP -> SitemapAction.values();
            };
            throw new IllegalArgumentException("Unknown action '" + name + "' for resource type " + type
                    + ". Valid actions: " + Arrays.toString(valid));
        }
    }
}
