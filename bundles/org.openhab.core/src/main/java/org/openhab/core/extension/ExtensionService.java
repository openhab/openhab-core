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
package org.openhab.core.extension;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Classes implementing this interface can be registered as an OSGi service in order to provide functionality for
 * managing extensions, such as listing, installing and uninstalling them.
 * The REST API offers an uri that exposes this functionality.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface ExtensionService {

    /**
     * Retrieves all extensions
     *
     * It is expected that this method is rather cheap to call and will return quickly, i.e. some caching should be
     * implemented if required.
     *
     * @param locale the locale to use for the result
     * @return the localized extensions
     */
    List<Extension> getExtensions(Locale locale);

    /**
     * Retrieves the extension for the given id.
     *
     * @param id the id of the extension
     * @param locale the locale to use for the result
     * @return the localized extension or null, if no extension exists with this id
     */
    Extension getExtension(String id, Locale locale);

    /**
     * Retrieves all possible types of extensions.
     *
     * @param locale the locale to use for the result
     * @return the localized types
     */
    List<ExtensionType> getTypes(Locale locale);

    /**
     * Installs the given extension.
     *
     * This can be a long running process. The framework makes sure that this is called within a separate thread and
     * ExtensionEvents will be sent upon its completion.
     *
     * @param id the id of the extension to install
     */
    void install(String id);

    /**
     * Uninstalls the given extension.
     *
     * This can be a long running process. The framework makes sure that this is called within a separate thread and
     * ExtensionEvents will be sent upon its completion.
     *
     * @param id the id of the extension to uninstall
     */
    void uninstall(String id);

    /**
     * Parses the given URI and extracts an extension Id.
     *
     * This must not be a long running process but return immediately.
     *
     * @param extensionURI the URI from which to parse the extension Id.
     * @return the extension Id if the URI can be parsed, otherwise <code>null</code>.
     */
    String getExtensionId(URI extensionURI);

}
