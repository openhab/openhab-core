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
package org.openhab.core.model.thing.valueconverter;

import org.openhab.core.model.core.valueconverter.ValueTypeToStringConverter;
import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;

import com.google.inject.Inject;

public class ThingValueConverters extends DefaultTerminalConverters {

    @Inject
    private ValueTypeToStringConverter valueTypeToStringConverter;

    @Inject
    private UIDtoStringConverter uidToStringConverter;

    @ValueConverter(rule = "ValueType")
    public IValueConverter<Object> ValueType() {
        return valueTypeToStringConverter;
    }

    @ValueConverter(rule = "UID")
    public IValueConverter<String> UID() {
        return uidToStringConverter;
    }

}
