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
package org.eclipse.smarthome.io.transport.mqtt.internal;

import java.util.concurrent.CompletableFuture;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.smarthome.io.transport.mqtt.MqttActionCallback;

/**
 * Create a IMqttActionListener object for being used as a callback for a publish attempt.
 *
 * @author David Graeff - Initial contribution
 */
public class MqttActionAdapterCallback implements IMqttActionListener {
    @Override
    public void onSuccess(IMqttToken token) {
        if (token.getUserContext() instanceof MqttActionCallback) {
            MqttActionCallback subscriber = (MqttActionCallback) token.getUserContext();
            subscriber.onSuccess(token.getTopics()[0]);
        } else if (token.getUserContext() instanceof CompletableFuture) {
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) token.getUserContext();
            future.complete(true);
        }
    }

    @Override
    public void onFailure(IMqttToken token, Throwable throwable) {
        if (token.getUserContext() instanceof MqttActionCallback) {
            MqttActionCallback subscriber = (MqttActionCallback) token.getUserContext();
            subscriber.onFailure(token.getTopics()[0], throwable);
        } else if (token.getUserContext() instanceof CompletableFuture) {
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) token.getUserContext();
            future.completeExceptionally(throwable);
        }
    }
}
