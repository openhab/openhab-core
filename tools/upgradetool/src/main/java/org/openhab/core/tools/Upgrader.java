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
package org.openhab.core.tools;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link Upgrader} provides an interface for upgrading openHAB configuration files.
 * 
 * Implementing class MUST provide a no-argument constructor.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public interface Upgrader {
    String getName();

    String getDescription();

    /**
     * Executes the upgrade process.
     *
     * @param userdataDir the OPENHAB_USERDATA directory for the upgrade,
     *            or a custom path given by the user as --userdata argument
     * @param confDir the OPENHAB_CONF directory for the upgrade,
     *            or a custom path given by the user as --conf argument
     * @return true if the upgrade was successful, false otherwise
     */
    boolean execute(String userdataDir, String confDir);
}
