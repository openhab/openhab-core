/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
 * Interface representing all sitemap entities that can be parents, should be extended by Sitemap and LinkableWidget.
 * This is a marker interface.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Parent {

    /**
     * Get the child {@link Widget}s. This method should return a modifiable list, allowing updates to the child
     * widgets.
     *
     * @return widgets
     */
    List<Widget> getWidgets();

    /**
     * Replace the child widgets with a new list of widgets.
     *
     * @param widgets
     */
    void setWidgets(List<Widget> widgets);
}
