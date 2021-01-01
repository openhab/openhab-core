/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.transport.mqtt;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implement this to be notified of the success or error of a any method in {@link MqttBrokerConnection} that takes a
 * callback.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public interface MqttActionCallback {
    public void onSuccess(String topic);

    public void onFailure(String topic, Throwable error);
}
