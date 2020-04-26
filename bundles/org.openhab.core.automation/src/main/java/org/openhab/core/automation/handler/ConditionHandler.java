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
package org.openhab.core.automation.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;

/**
 * This interface provides common functionality for processing {@link Condition} modules.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 * @see ModuleHandler
 */
@NonNullByDefault
public interface ConditionHandler extends ModuleHandler {

    /**
     * Checks if the Condition is satisfied in the given {@code context}.
     *
     * @param context an unmodifiable map containing the outputs of the {@link Trigger} that triggered the {@link Rule}
     *            and the inputs of the {@link Condition}.
     * @return {@code true} if {@link Condition} is satisfied, {@code false} otherwise.
     */
    public boolean isSatisfied(Map<String, Object> context);
}
