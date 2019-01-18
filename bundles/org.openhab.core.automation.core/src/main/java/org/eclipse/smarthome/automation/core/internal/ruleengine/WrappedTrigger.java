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
package org.eclipse.smarthome.automation.core.internal.ruleengine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.TriggerHandler;

/**
 * This class holds the information that is necessary for the rule engine.
 *
 * @author Markus Rathgeb - Initial Contribution and API
 */
@NonNullByDefault
public class WrappedTrigger extends WrappedModule<Trigger, TriggerHandler> {

    public WrappedTrigger(final Trigger trigger) {
        super(trigger);
    }
}
