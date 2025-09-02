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
import org.openhab.core.sitemap.Mapview;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class MapviewImpl extends NonLinkableWidgetImpl implements Mapview {

    private @Nullable Integer height;

    public MapviewImpl() {
        super();
    }

    public MapviewImpl(Parent parent) {
        super(parent);
    }

    @Override
    public int getHeight() {
        return height != null ? height : 0;
    }

    @Override
    public void setHeight(@Nullable Integer height) {
        this.height = height;
    }
}
