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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This interface represents automation {@code Trigger} modules which define what phenomenon will start the execution
 * of the {@link Rule} and trigger it when an exact phenomenon occurs. Each of them can independently trigger the rule.
 * <p>
 * The triggers do not receive information from other modules of the Rule so they don't have {@link Input}s.
 * <p>
 * The triggers can be configured.
 * <p>
 * The triggers have {@link Output}s to provide information about the occurred phenomenon to the {@link Condition}s and
 * {@link Action}s of the Rule.
 * <p>
 * Building elements of conditions as {@link ConfigDescriptionParameter}s and {@link Input}s are defined by
 * {@link TriggerType}.
 * <p>
 * Trigger modules are placed in <b>triggers</b> section of the {@link Rule} definition.
 *
 * @see Module
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public interface Trigger extends Module {

}
