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

/**
 * A representation of a sitemap Buttongrid widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Buttongrid extends LinkableWidget {

    /**
     * Get the button grid buttons. This method should return a modifiable list, allowing updates to the list of
     * buttons.
     *
     * @return buttons
     */
    List<ButtonDefinition> getButtons();

    /**
     * Replace the button grid buttons with a new list of buttons.
     *
     * @param buttons
     */
    void setButtons(List<ButtonDefinition> buttons);
}
