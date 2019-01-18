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
package org.eclipse.smarthome.config.xml;

import org.eclipse.smarthome.config.core.FilterCriteria;
import org.eclipse.smarthome.config.xml.util.GenericUnmarshaller;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link FilterCriteriaConverter} creates a {@link FilterCriteria} instance
 * from a {@code criteria} XML node.
 *
 * @author Alex Tugarev - Initial Contribution
 */
public class FilterCriteriaConverter extends GenericUnmarshaller<FilterCriteria> {

    public FilterCriteriaConverter() {
        super(FilterCriteria.class);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String name = reader.getAttribute("name");
        String criteria = reader.getValue();
        return new FilterCriteria(name, criteria);
    }

}
