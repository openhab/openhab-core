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
package org.eclipse.smarthome.model.internal.valueconverter;

import org.eclipse.smarthome.model.core.valueconverter.ValueTypeToStringConverter;
import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;

import com.google.inject.Inject;

/**
 * Registers {@link IValueConverter}s for the items language.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class ItemValueConverters extends DefaultTerminalConverters {

    @Inject
    private ValueTypeToStringConverter valueTypeToStringConverter;

    @ValueConverter(rule = "ValueType")
    public IValueConverter<Object> ValueType() {
        return valueTypeToStringConverter;
    }

}
