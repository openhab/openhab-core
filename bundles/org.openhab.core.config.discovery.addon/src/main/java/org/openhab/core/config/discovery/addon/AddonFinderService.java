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
package org.openhab.core.config.discovery.addon;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Classes implementing this interface can be registered as an OSGi service in order to provide functionality for
 * managing add-on suggestion finders, such as installing and uninstalling them.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface AddonFinderService {

    /**
     * Installs the given add-on suggestion finder.
     *
     * This can be a long running process. The framework makes sure that this is called within a separate thread and
     * add-on events will be sent upon its completion.
     *
     * @param id the id of the add-on suggestion finder to install
     */
    void install(String id);

    /**
     * Uninstalls the given add-on suggestion finder.
     *
     * This can be a long running process. The framework makes sure that this is called within a separate thread and
     * add-on events will be sent upon its completion.
     *
     * @param id the id of the add-on suggestion finder to uninstall
     */
    void uninstall(String id);
}
