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
package org.eclipse.smarthome.core.types;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Due to the duality of some types (which can be states and commands at the
 * same time), we need to be able to differentiate what the meaning of a
 * message on the bus is - does "item ON" mean that its state has changed to
 * ON or that it should turn itself ON? To decide this, we send the event
 * type as an additional information on the event bus for each message.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public enum EventType {

    COMMAND("command"),
    UPDATE("update");

    private String name;

    private EventType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
