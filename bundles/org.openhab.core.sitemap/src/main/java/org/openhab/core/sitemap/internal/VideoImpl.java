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
package org.openhab.core.sitemap.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Video;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class VideoImpl extends NonLinkableWidgetImpl implements Video {

    private String url = "";
    private @Nullable String encoding;

    public VideoImpl() {
        super();
    }

    public VideoImpl(Parent parent) {
        super(parent);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public @Nullable String getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(@Nullable String encoding) {
        this.encoding = encoding;
    }
}
