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
package org.openhab.core.automation;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This interface represents automation {@code Condition} modules which are working as a filter for {@link Rule}'s
 * executions. After being triggered, a Rule's execution will continue only if all its conditions are satisfied.
 * <p>
 * Conditions can be used to check the output from the trigger or other data available in the system. To receive an
 * output data from triggers the Conditions have {@link Input}s.
 * <p>
 * Conditions can be configured.
 * <p>
 * Conditions don't have {@link Output}s 'cause they don't provide information to the other modules of the Rule.
 * <p>
 * Building elements of conditions as {@link ConfigDescriptionParameter}s and {@link Input}s. They are defined by the
 * corresponding {@link ConditionType}.
 * <p>
 * Condition modules are placed in <b>conditions</b> section of the {@link Rule} definition.
 *
 * @see Module
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public interface Condition extends Module {

    /**
     * Gets the input references of the Condition. The references define how the {@link Input}s of this {@link Module}
     * are connected to {@link Output}s of other {@link Module}s.
     *
     * @return a map that contains the input references of this condition.
     */
    Map<String, String> getInputs();

}
