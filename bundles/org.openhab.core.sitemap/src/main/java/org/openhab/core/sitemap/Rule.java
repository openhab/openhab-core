/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.sitemap;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap widget icon, color or visibility rule.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Rule {

    /**
     * Get the rule conditions. This method should return a modifiable list, allowing updates to conditions.
     *
     * @return conditions
     */
    List<Condition> getConditions();

    /**
     * Replace the rule conditions with a new list of conditions.
     *
     * @param conditions
     */
    void setConditions(List<Condition> conditions);

    /**
     * Get the rule argument for icon or color rules. The rule argument is the resulting value if the rule is met.
     * Visibility rules don't have an argument, always work on the full widget.
     *
     * @return argument
     */
    @Nullable
    String getArgument();

    /**
     * Set the rule argument.
     *
     * @param argument
     */
    void setArgument(@Nullable String argument);
}
