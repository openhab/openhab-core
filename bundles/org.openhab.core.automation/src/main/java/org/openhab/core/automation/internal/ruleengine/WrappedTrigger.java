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
package org.openhab.core.automation.internal.ruleengine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.TriggerHandler;

/**
 * This class holds the information that is necessary for the rule engine.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class WrappedTrigger extends WrappedModule<Trigger, TriggerHandler> {

    public WrappedTrigger(final Trigger trigger) {
        super(trigger);
    }
}
