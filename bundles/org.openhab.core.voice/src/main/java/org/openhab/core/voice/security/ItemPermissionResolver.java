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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.voice.text.HumanLanguageInterpreter;

/**
 * Defines a utility service to resolve the permission for an item for Human Language Interpreters.
 * <p>
 * The permission rules are as follows:
 * <ul>
 * <li>Explicit configuration: An Item can explicitly set the permission via the {@code permission} property in the
 * {@code voiceSystem} metadata namespace.</li>
 * <li>Inheritance: If no explicit configuration is found on the Item itself, it inherits permissions from its parent
 * groups.</li>
 * <li>Merging and Priority: Parent permissions are merged. Within the same layer, the following prioritization applies:
 * {@link ItemPermission#NO_ACCESS} > {@link ItemPermission#READ_ONLY} > {@link ItemPermission#READ_WRITE}.
 * The closest parent layer has priority over further away layers.</li>
 * <li>System Default: If no explicit configuration is found on the Item or any of its ancestors, the system-wide
 * default is used.</li>
 * </ul>
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface ItemPermissionResolver {
    String VOICE_SYSTEM_NAMESPACE = "voiceSystem";
    String PERMISSION_PROPERTY = "permission";
    String SYSTEM_DEFAULT_SOURCE = "system:default";

    /**
     * Adds a listener that is notified whenever item access may have changed.
     * 
     * @param listener the listener to add
     */
    void addItemAccessChangeListener(Runnable listener);

    /**
     * Removes a previously registered item access change listener.
     * 
     * @param listener the listener to remove
     */
    void removeItemAccessChangeListener(Runnable listener);

    /**
     * Returns the {@link ItemPermission} for an item.
     *
     * @param item the item to check
     * @return the item permission
     */
    ItemPermission getPermission(Item item);

    /**
     * Returns whether an item is accessible for {@link HumanLanguageInterpreter}s.
     *
     * @param item the item to check
     * @return true if the item is accessible, false otherwise
     */
    default boolean isAccessible(Item item) {
        return getPermission(item) != ItemPermission.NO_ACCESS;
    }

    /**
     * Gets the {@link ItemPermissionDetails} for an item.
     *
     * @param item the item to check
     * @return the item permission details (the item permission and its source)
     */
    ItemPermissionDetails getItemPermissionDetails(Item item);

    record ItemPermissionDetails(ItemPermission permission, @Nullable String source) {
    }
}
