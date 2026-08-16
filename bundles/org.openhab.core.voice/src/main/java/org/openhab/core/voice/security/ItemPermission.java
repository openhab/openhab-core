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
package org.openhab.core.voice.security;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Defines the permissions for an item for Human Language Interpreters.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public enum ItemPermission {
    // Attention: Enum ordering MUST NOT be changed as it defines the priority of the permissions

    /**
     * No access to the item.
     */
    NO_ACCESS,

    /**
     * Read-only access to the item.
     */
    READ_ONLY,

    /**
     * Read-write access to the item.
     */
    READ_WRITE
}
