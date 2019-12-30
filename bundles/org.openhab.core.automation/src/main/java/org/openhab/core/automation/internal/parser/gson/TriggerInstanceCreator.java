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
package org.openhab.core.automation.internal.parser.gson;

import java.lang.reflect.Type;

import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.TriggerType;

import com.google.gson.InstanceCreator;

/**
 * This class creates {@link TriggerType} instances.
 *
 * @author Ana Dimova - Initial contribution
 */
public class TriggerInstanceCreator implements InstanceCreator<CompositeTriggerType> {

    @Override
    public CompositeTriggerType createInstance(Type type) {
        return new CompositeTriggerType(null, null, null, null);
    }
}
