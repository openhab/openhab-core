/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model.sitemap;

import java.util.Set;


import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.sitemap.sitemap.Sitemap;

@NonNullByDefault
public interface SitemapProvider {

    /**
     * This method provides access to sitemap model files, loads them and returns the object model tree.
     *
     * @param sitemapName the name of the sitemap to load
     * @return the object model tree, null if it is not found
     */
    @Nullable
    Sitemap getSitemap(String sitemapName);

    /**
     * Returns the names of all available sitemaps
     *
     * @return names of provided sitemaps
     */
    Set<String> getSitemapNames();

    /**
     * Add a listener which will be informed subsequently once a model has changed
     *
     * @param listener
     */
    void addModelChangeListener(ModelRepositoryChangeListener listener);

    /**
     * Remove a model change listener again
     *
     * @param listener
     */
    void removeModelChangeListener(ModelRepositoryChangeListener listener);

}
