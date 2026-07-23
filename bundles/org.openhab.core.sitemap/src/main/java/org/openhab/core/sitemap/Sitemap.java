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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * A representation of a '<em><b>Sitemap</b></em>'.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Sitemap extends Identifiable<String>, Parent {

    /**
     * Returns the sitemap name.
     *
     * @return sitemap name.
     */
    String getName();

    /**
     * Sets the sitemap name.
     *
     * @param name the new sitemap name.
     */
    void setName(String name);

    /**
     * Returns the sitemap label.
     *
     * @return sitemap label.
     */
    @Nullable
    String getLabel();

    /**
     * Sets the sitemap label.
     *
     * @param label the new sitemap label.
     */
    void setLabel(@Nullable String label);

    /**
     * Returns the sitemap icon.
     *
     * @return sitemap icon.
     */
    @Nullable
    String getIcon();

    /**
     * Sets the sitemap icon.
     *
     * @param icon the new sitemap icon.
     */
    void setIcon(@Nullable String icon);
}
