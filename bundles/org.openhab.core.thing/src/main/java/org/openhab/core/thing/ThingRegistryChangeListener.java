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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * {@link ThingRegistryChangeListener} can be implemented to listen for things
 * beeing added or removed. The listener must be added and removed via
 * {@link ThingRegistry#addRegistryChangeListener(ThingRegistryChangeListener)} and
 * {@link ThingRegistry#removeRegistryChangeListener(ThingRegistryChangeListener)}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Added dynamic configuration update
 *
 * @see ThingRegistry
 */
@NonNullByDefault
public interface ThingRegistryChangeListener extends RegistryChangeListener<Thing> {

}
