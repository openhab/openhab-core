/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.binding.events;

import org.eclipse.smarthome.core.events.AbstractEvent;

/**
 * Event for binding level user notifications
 *
 * @author Chris Jackson - Initial Contribution
 *
 */
public class BindingEvent extends AbstractEvent {
    public final static String TYPE = BindingEvent.class.getSimpleName();

    public BindingEvent(String topic, String binding, String payload) {
        super(topic, payload, binding);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
