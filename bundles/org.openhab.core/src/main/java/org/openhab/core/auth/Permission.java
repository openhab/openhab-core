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
 * A permission grants access to perform a specific {@link Action} on resources of a given
 * {@link ResourceType} matching the {@link Selector} expression.
 *
 * @author Gabor Bicskei - Initial contribution
 *
 * @param resourceType the type of resource this permission applies to
 * @param selector a selector determining which resources are covered
 * @param action the action that is permitted
 */
@NonNullByDefault
public record Permission(ResourceType resourceType, Selector selector, Action action) {

    /**
     * Validates that the action's resource type matches the permission's resource type.
     */
    public Permission {
        if (action.resourceType() != resourceType) {
            throw new IllegalArgumentException(
                    "Action " + action.name() + " is not valid for resource type " + resourceType);
        }
    }
}
