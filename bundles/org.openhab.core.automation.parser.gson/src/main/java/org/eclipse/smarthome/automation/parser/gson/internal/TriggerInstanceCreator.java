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
package org.eclipse.smarthome.automation.parser.gson.internal;

import java.lang.reflect.Type;

import org.eclipse.smarthome.automation.type.CompositeTriggerType;
import org.eclipse.smarthome.automation.type.TriggerType;

import com.google.gson.InstanceCreator;

/**
 * This class creates {@link TriggerType} instances.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class TriggerInstanceCreator implements InstanceCreator<CompositeTriggerType> {

    @Override
    public CompositeTriggerType createInstance(Type type) {
        return new CompositeTriggerType(null, null, null, null);
    }
}
