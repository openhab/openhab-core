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
package org.openhab.core.io.console.rfc147.internal;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;

/**
 * Container interface for accessing registered console command extensions.
 * This interface provides access to all console commands that are currently registered
 * in the OSGi RFC 147 console support.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface ConsoleCommandsContainer {

    /**
     * Gets all registered console command extensions.
     *
     * @return a collection of all console command extensions currently registered
     */
    Collection<ConsoleCommandExtension> getConsoleCommandExtensions();
}
