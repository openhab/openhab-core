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
package org.openhab.core.tools;

import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

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

    default @Nullable String getTargetVersion() {
        return null;
    }

    /**
     * Executes the upgrade process.
     *
     * @param userdataPath the OPENHAB_USERDATA directory for the upgrade,
     *            or a custom path given by the user as --userdata argument
     * @param confPath the OPENHAB_CONF directory for the upgrade,
     *            or a custom path given by the user as --conf argument
     * @return true if the upgrade was successful, false otherwise
     */
    boolean execute(@Nullable Path userdataPath, @Nullable Path confPath);
}
