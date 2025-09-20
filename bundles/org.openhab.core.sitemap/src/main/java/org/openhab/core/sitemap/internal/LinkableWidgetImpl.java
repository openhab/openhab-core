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
import org.openhab.core.sitemap.LinkableWidget;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Widget;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class LinkableWidgetImpl extends WidgetImpl implements LinkableWidget {

    private List<Widget> widgets = new CopyOnWriteArrayList<>();

    public LinkableWidgetImpl() {
        super();
    }

    public LinkableWidgetImpl(Parent parent) {
        super(parent);
    }

    @Override
    public List<Widget> getWidgets() {
        return widgets;
    }

    @Override
    public void setWidgets(List<Widget> widgets) {
        this.widgets = new CopyOnWriteArrayList<>(widgets);
    }
}
