/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.core.xml;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.xml.util.GenericUnmarshaller;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link FilterCriteriaConverter} creates a {@link FilterCriteria} instance
 * from a {@code criteria} XML node.
 *
 * @author Alex Tugarev - Initial contribution
 */
@NonNullByDefault
public class FilterCriteriaConverter extends GenericUnmarshaller<FilterCriteria> {

    public FilterCriteriaConverter() {
        super(FilterCriteria.class);
    }

    @Override
    public @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String name = reader.getAttribute("name");
        String criteria = reader.getValue();
        return new FilterCriteria(name, criteria);
    }
}
