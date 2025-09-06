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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.sitemap.Mapping;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Selection;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SelectionImpl extends NonLinkableWidgetImpl implements Selection {

    private List<Mapping> mappings = new CopyOnWriteArrayList<>();

    public SelectionImpl() {
        super();
    }

    public SelectionImpl(Parent parent) {
        super(parent);
    }

    @Override
    public List<Mapping> getMappings() {
        return mappings;
    }

    @Override
    public void setMappings(List<Mapping> mappings) {
        this.mappings = new CopyOnWriteArrayList<>(mappings);
    }
}
