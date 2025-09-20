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
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class ImageImpl extends LinkableWidgetImpl implements Image {

    private @Nullable String url;
    private @Nullable Integer refresh;

    public ImageImpl() {
        super();
    }

    public ImageImpl(Parent parent) {
        super(parent);
    }

    @Override
    public @Nullable String getUrl() {
        return url;
    }

    @Override
    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    @Override
    public int getRefresh() {
        return refresh != null ? refresh : 0;
    }

    @Override
    public void setRefresh(@Nullable Integer refresh) {
        this.refresh = refresh;
    }
}
