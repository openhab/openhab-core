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
package org.openhab.core.thing;

/**
 * {@link ThingManager} interface defines methods for managing a {@link Thing}.
 *
 * @author Yordan Zhelev - Initial contribution
 */
public interface ThingManager {

    /**
     * This method gets the <b>enabled</b> status for a {@link Thing}.
     * The only {@link ThingStatus} which is NOT enabled is {@link ThingStatus} with
     * {@link ThingStatusDetail#DISABLED}.
     *
     * @param thingUID UID of the {@link Thing}.
     * @return {@code false} when the {@link Thing} has {@link ThingStatus} with {@link ThingStatusDetail#DISABLED}.
     *         Returns {@code true} in all other cases.
     */
    public boolean isEnabled(ThingUID thingUID);

    /**
     * This method is used for changing <b>enabled</b> state of the {@link Thing}
     * The only {@link ThingStatus} which is NOT enabled is {@link ThingStatus} with
     * {@link ThingStatusDetail#DISABLED}.
     *
     * @param thingUID UID of the {@link Thing}.
     * @param isEnabled a new <b>enabled / disabled</b> state of the {@link Thing}.
     */
    public void setEnabled(ThingUID thingUID, boolean isEnabled);

}
