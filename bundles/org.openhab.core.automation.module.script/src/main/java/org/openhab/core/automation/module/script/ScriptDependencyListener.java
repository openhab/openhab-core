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
package org.openhab.core.automation.module.script;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface that allows listener to be notified of script dependencies (libraries)
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@NonNullByDefault
@FunctionalInterface
public interface ScriptDependencyListener extends Consumer<String> {
    void accept(String dependency);
}
