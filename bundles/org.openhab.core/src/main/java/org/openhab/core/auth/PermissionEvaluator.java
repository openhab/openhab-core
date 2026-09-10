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
 * Evaluates whether a specific {@link Permission} grants access to a resource.
 * One evaluator is registered per {@link ResourceType}. The
 * {@link AuthorizationService} delegates to the appropriate evaluator when
 * checking permissions.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public interface PermissionEvaluator {

    /**
     * Returns the resource type this evaluator handles.
     *
     * @return the resource type
     */
    ResourceType getResourceType();

    /**
     * Checks if the given permission grants access to the specified resource and action.
     *
     * @param permission the permission to evaluate
     * @param resourceId the resource identifier to check against the permission's selector
     * @param action the action to check against the permission's action
     * @return {@code true} if the permission grants the requested access
     */
    boolean evaluate(Permission permission, String resourceId, Action action);
}
