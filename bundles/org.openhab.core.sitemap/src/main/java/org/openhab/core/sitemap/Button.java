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
 * A representation of a sitemap {@link Button} widget. Button widgets should have a parent {@link Buttongrid} widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Button extends NonLinkableWidget {

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
     * True if the button is stateless, by default a button is stateful.
     *
     * @return stateless
     */
    boolean isStateless();

    /**
     * Set stateless parameter for button.
     *
     * @param stateless
     */
    void setStateless(@Nullable Boolean stateless);

    /**
     * Get button command, will be executed when the button is clicked.
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
     * Get button release command, will be executed when the button is released.
     *
     * @return releaseCmd
     */
    @Nullable
    String getReleaseCmd();

    /**
     * Set the button release command.
     *
     * @param releaseCmd
     */
    void setReleaseCmd(@Nullable String releaseCmd);
}
