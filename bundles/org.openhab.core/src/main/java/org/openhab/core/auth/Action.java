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
 * Marker interface for typed permission actions. Each {@link ResourceType} has its
 * own enum implementing this interface (e.g., {@link ItemAction}, {@link PageAction}).
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public interface Action {

    /**
     * The action name as used in serialization and display.
     *
     * @return the action name
     */
    String name();

    /**
     * The resource type this action belongs to.
     *
     * @return the resource type
     */
    ResourceType resourceType();
}
