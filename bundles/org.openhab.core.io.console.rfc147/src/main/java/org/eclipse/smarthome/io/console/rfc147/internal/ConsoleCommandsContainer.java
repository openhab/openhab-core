/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.console.rfc147.internal;

import java.util.Collection;

import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;

/**
 *
 * @author Markus Rathgeb - Initial contribution and API
 *
 */
public interface ConsoleCommandsContainer {

    public Collection<ConsoleCommandExtension> getConsoleCommandExtensions();
}
