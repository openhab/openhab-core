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
package org.eclipse.smarthome.core.thing.type;

/**
 * A binding's recommendation to the framework whether a state update should be automatically sent to an item if a
 * command was received.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public enum AutoUpdatePolicy {
    /**
     * No automatic state update should be sent by the framework. The handler will make sure it sends a state update and
     * it can do it better than just converting the command to a state.
     */
    VETO,

    /**
     * The binding does not care and the framework may do what it deems to be right. The state update which the
     * framework will send out normally will correspond the command state anyway. This is the default if no other policy
     * is set.
     */
    DEFAULT,

    /**
     * An automatic state update should be sent by the framework because no updates will be sent by the binding.
     * This usually is the case when devices don't expose their current state to the handler.
     */
    RECOMMEND,
}
