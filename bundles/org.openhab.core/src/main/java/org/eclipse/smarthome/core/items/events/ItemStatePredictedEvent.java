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
package org.eclipse.smarthome.core.items.events;

import org.eclipse.smarthome.core.events.AbstractEvent;
import org.eclipse.smarthome.core.types.State;

/**
 * This event announces potential item state outcomes when a command was received.
 * <p>
 * Thereby it denotes that the item state is most likely going to change to the given predicted value.
 * <p>
 * If {@code isConfirmation == true}, then it basically only confirms the previous item state because a received command
 * will not be successfully executed and therefore presumably will not result in a state change (e.g. because no handler
 * currently is capable of delivering such an event to its device).
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class ItemStatePredictedEvent extends AbstractEvent {

    /**
     * The item state changed event type.
     */
    public static final String TYPE = ItemStatePredictedEvent.class.getSimpleName();

    protected final String itemName;
    protected final State predictedState;
    protected final boolean isConfirmation;

    public ItemStatePredictedEvent(String topic, String payload, String itemName, State predictedState,
            boolean isConfirmation) {
        super(topic, payload, null);
        this.itemName = itemName;
        this.predictedState = predictedState;
        this.isConfirmation = isConfirmation;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getItemName() {
        return itemName;
    }

    public State getPredictedState() {
        return predictedState;
    }

    public boolean isConfirmation() {
        return isConfirmation;
    }

    @Override
    public String toString() {
        return String.format("%s predicted to become %s", itemName, predictedState);
    }

}
