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
 * A representation of a sitemap Video widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Video extends NonLinkableWidget {

    /**
     * Get the url of the video.
     *
     * @return url
     */
    String getUrl();

    /**
     * Set the url of the video.
     *
     * @param url
     */
    void setUrl(String url);

    /**
     * Get the configured video encoding.
     *
     * @return encoding, null if no encoding is configured
     */
    @Nullable
    String getEncoding();

    /**
     * Set the video encoding.
     *
     * @param encoding
     */
    void setEncoding(@Nullable String encoding);
}
