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
package org.openhab.core.items.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.State;

/**
 * This event announces potential item state outcomes when a command was received.
 * <p>
 * Thereby it denotes that the item state is most likely going to change to the given predicted value.
 * <p>
 * If {@code isConfirmation == true}, then it basically only confirms the previous item state because a received command
 * will not be successfully executed and therefore presumably will not result in a state change (e.g. because no handler
 * currently is capable of delivering such an event to its device).
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class ItemStatePredictedEvent extends ItemEvent {

    /**
     * The item state predicted event type.
     */
    public static final String TYPE = ItemStatePredictedEvent.class.getSimpleName();

    protected final State predictedState;
    protected final boolean isConfirmation;

    /**
     * Constructs a new item state predicted event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param predictedState the predicted item state
     * @param isConfirmation the confirmation of previous item state
     */
    public ItemStatePredictedEvent(String topic, String payload, String itemName, State predictedState,
            boolean isConfirmation) {
        super(topic, payload, itemName, null);
        this.predictedState = predictedState;
        this.isConfirmation = isConfirmation;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the predicted item state.
     *
     * @return the predicted item state
     */
    public State getPredictedState() {
        return predictedState;
    }

    /**
     * Gets the confirmation of previous item state.
     *
     * @return true, if previous item state is confirmed
     */
    public boolean isConfirmation() {
        return isConfirmation;
    }

    @Override
    public String toString() {
        return String.format("Item '%s' predicted to become %s", itemName, predictedState);
    }
}
