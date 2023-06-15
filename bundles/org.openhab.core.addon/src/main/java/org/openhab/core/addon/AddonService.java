/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.addon;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Classes implementing this interface can be registered as an OSGi service in order to provide functionality for
 * managing add-ons, such as listing, installing and uninstalling them.
 * The REST API offers a uri that exposes this functionality.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Add id, name and refreshSource
 */
@NonNullByDefault
public interface AddonService {

    /**
     * Returns the ID of the service.
     *
     * @return the service identifier
     */
    String getId();

    /**
     * Returns the name of the service.
     *
     * @return the service name
     */
    String getName();

    /**
     * Refreshes the source used for providing the add-ons.
     *
     * This can be called before getAddons to ensure the add-on information is up-to-date; otherwise they might be
     * retrieved from a cache.
     */
    void refreshSource();

    /**
     * Retrieves all add-ons.
     *
     * It is expected that this method is rather cheap to call and will return quickly, i.e. some caching should be
     * implemented if required.
     *
     * @param locale the locale to use for the result
     * @return the localized add-ons
     */
    List<Addon> getAddons(@Nullable Locale locale);

    /**
     * Retrieves the add-on for the given id.
     *
     * @param id the id of the add-on
     * @param locale the locale to use for the result
     * @return the localized add-on or <code>null</code>, if no add-on exists with this id
     */
    @Nullable
    Addon getAddon(String id, @Nullable Locale locale);

    /**
     * Retrieves all possible types of add-ons.
     *
     * @param locale the locale to use for the result
     * @return the localized types
     */
    List<AddonType> getTypes(@Nullable Locale locale);

    /**
     * Installs the given add-on.
     *
     * This can be a long running process. The framework makes sure that this is called within a separate thread and
     * add-on events will be sent upon its completion.
     *
     * @param id the id of the add-on to install
     */
    void install(String id);

    /**
     * Uninstalls the given add-on.
     *
     * This can be a long running process. The framework makes sure that this is called within a separate thread and
     * add-on events will be sent upon its completion.
     *
     * @param id the id of the add-on to uninstall
     */
    void uninstall(String id);

    /**
     * Parses the given URI and extracts an add-on Id.
     *
     * This must not be a long running process but return immediately.
     *
     * @param addonURI the URI from which to parse the add-on Id.
     * @return the add-on Id if the URI can be parsed, otherwise <code>null</code>
     */
    @Nullable
    String getAddonId(URI addonURI);
}
