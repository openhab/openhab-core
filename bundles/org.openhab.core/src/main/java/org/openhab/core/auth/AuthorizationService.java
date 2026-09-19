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

import java.util.Collection;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Service for checking whether an authenticated user has permission to perform
 * an action on a resource. When RBAC enforcement is disabled, all checks return
 * {@code true} (allow-all).
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public interface AuthorizationService {

    /**
     * Returns whether RBAC enforcement is enabled.
     * When disabled, all permission checks return {@code true} (allow-all).
     *
     * @return {@code true} if RBAC enforcement is active
     */
    boolean isEnabled();

    /**
     * Checks whether the authenticated user has permission to perform the given
     * action on the specified resource.
     *
     * @param auth the authentication context (username + roles)
     * @param type the resource type
     * @param resourceId the specific resource identifier
     * @param action the action being performed
     * @return {@code true} if access is granted
     */
    boolean hasPermission(Authentication auth, ResourceType type, String resourceId, Action action);

    /**
     * Filters a collection, returning only resources the user is authorized to access.
     * When RBAC is disabled, returns the input collection unchanged (no copy).
     *
     * @param <T> the element type
     * @param auth the authentication context
     * @param resources the full collection of resources
     * @param type the resource type
     * @param action the action being performed
     * @param idExtractor extracts the resource ID from each element
     * @return a collection containing only authorized resources (may be the input itself if unfiltered)
     */
    <T> Collection<T> filterAuthorized(Authentication auth, Collection<T> resources, ResourceType type, Action action,
            Function<T, String> idExtractor);
}
