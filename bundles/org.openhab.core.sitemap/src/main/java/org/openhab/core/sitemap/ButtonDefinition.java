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
 * A representation of a sitemap {@link Buttongrid} button definition. All buttons will act on the same item defined in
 * the button grid.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface ButtonDefinition {

    /**
     * Get button row in grid.
     *
     * @return row
     */
    int getRow();

    /**
     * Set button row in grid.
     *
     * @param row
     */
    void setRow(int row);

    /**
     * Get button column in grid.
     *
     * @return column
     */
    int getColumn();

    /**
     * Set button column in grid.
     *
     * @param column
     */
    void setColumn(int column);

    /**
     * Get button command.
     *
     * @return cmd
     */
    String getCmd();

    /**
     * Set button command.
     *
     * @param cmd
     */
    void setCmd(String cmd);

    /**
     * Get button label.
     *
     * @return label
     */
    String getLabel();

    /**
     * Set button label.
     *
     * @param label
     */
    void setLabel(String label);

    /**
     * Get button icon.
     *
     * @return icon
     */
    @Nullable
    String getIcon();

    /**
     * Set button icon.
     *
     * @param icon
     */
    void setIcon(@Nullable String icon);
}
