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
package org.eclipse.smarthome.automation.handler;

import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.Trigger;

/**
 * This interface provides common functionality for processing {@link Condition} modules.
 *
 * @see ModuleHandler
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Initial Contribution
 * @author Vasil Ilchev - Initial Contribution
 */
public interface ConditionHandler extends ModuleHandler {

    /**
     * Checks if the Condition is satisfied in the given {@code context}.
     *
     * @param context an unmodifiable map containing the outputs of the {@link Trigger} that triggered the {@link Rule}
     *                and the inputs of the {@link Condition}.
     * @return {@code true} if {@link Condition} is satisfied, {@code false} otherwise.
     */
    public boolean isSatisfied(Map<String, Object> context);

}
