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
 * A representation of a sitemap widget Mapping.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Mapping {

    /**
     * Get the click command mapped to the widget item state.
     *
     * @return cmd
     */
    String getCmd();

    /**
     * Set the click command mapped to the widget item state.
     *
     * @param cmd
     */
    void setCmd(String cmd);

    /**
     *
     * Get the release command mapped to the widget item state.
     *
     * @return releaseCmd
     */
    @Nullable
    String getReleaseCmd();

    /**
     * Set the release command mapped to the widget item state.
     *
     * @param releaseCmd
     */
    void setReleaseCmd(@Nullable String releaseCmd);

    /**
     * Get the label mapped to the widget item state.
     *
     * @return label
     */
    String getLabel();

    /**
     * Set the label mapped to the widget item state.
     *
     * @param label
     */
    void setLabel(String label);

    /**
     * Get the icon mapped to the widget item state.
     *
     * @return icon
     */
    @Nullable
    String getIcon();

    /**
     * Set the label mapped to the widget item state.
     *
     * @param icon
     */
    void setIcon(@Nullable String icon);
}
