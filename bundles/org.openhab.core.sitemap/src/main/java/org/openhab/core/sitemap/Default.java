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
 * A representation of a sitemap Default widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Default extends NonLinkableWidget {

    /**
     * Get the configured height of the widget. If no height is configured, 0 should be returned.
     *
     * @return height
     */
    int getHeight();

    /**
     * Set the height of the widget, null means no height is configured.
     *
     * @param height
     */
    void setHeight(@Nullable Integer height);
}
