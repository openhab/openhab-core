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
package org.eclipse.smarthome.io.transport.mqtt;

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
