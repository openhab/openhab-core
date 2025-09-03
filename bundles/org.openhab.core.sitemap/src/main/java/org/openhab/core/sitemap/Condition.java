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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap rule condition.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Condition {

    /**
     * Get the item for which the state will be used in the condition evaluation. If no item is set (null returned), the
     * item of
     * the widget will be used.
     *
     * @return item
     */
    @Nullable
    String getItem();

    /**
     * Set the item for which the state will be used in the condition evaluation.
     *
     * @param item
     */
    void setItem(@Nullable String item);

    /**
     * Get the condition comparator. Valid values are: "==", ">", "<", ">=", "<=", "!=". The item in the condition will
     * be compared against the value using this comparator. If no condition comparator is set, "==" is assumed.
     *
     * @return condition comparator
     */
    @Nullable
    String getCondition();

    /**
     * Set the condition comparator, see {@link #getCondition()}.
     *
     * @param condition
     */
    void setCondition(@Nullable String condition);

    /**
     * Get the condition comparison value.
     *
     * @return value
     */
    String getValue();

    /**
     * Set the condition comparison value.
     *
     * @param value
     */
    void setValue(String value);
}
