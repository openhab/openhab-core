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
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This interface represents automation {@code Action} modules which are the expected result of {@link Rule}s execution.
 * They describe the actual work that should be performed by the Rule as a response to a trigger.
 * <p>
 * Each Action can provide information to the next Actions in the list through its {@link Output}s. The actions have
 * {@link Input}s to process input data from other Actions or {@link Trigger}s.
 * <p>
 * Actions can be configured.
 * <p>
 * The building elements of the Actions are {@link ConfigDescriptionParameter}s, {@link Input}s and {@link Output}s.
 * They are defined by the corresponding {@link ActionType}.
 * <p>
 * Action modules are placed in the <b>actions</b> section of the {@link Rule} definition.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public interface Action extends Module {

    /**
     * Gets the input references of the Action. The references define how the {@link Input}s of this {@link Module} are
     * connected to {@link Output}s of other {@link Module}s.
     *
     * @return a map with the input references of this action.
     */
    Map<String, String> getInputs();

}
