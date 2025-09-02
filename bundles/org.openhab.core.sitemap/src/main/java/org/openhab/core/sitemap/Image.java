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
 * A representation of a sitemap Image widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Image extends LinkableWidget {

    /**
     * Get the url of the image.
     *
     * @return url
     */
    @Nullable
    String getUrl();

    /**
     * Set the url of the video.
     *
     * @param url
     */
    void setUrl(@Nullable String url);

    /**
     * Get the image refresh interval in s. If no interval is set, 0 should be returned.
     *
     * @return refresh
     */
    int getRefresh();

    /**
     * Set the image refresh interval in s.
     *
     * @param refresh
     */
    void setRefresh(@Nullable Integer refresh);
}
