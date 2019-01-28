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
package org.eclipse.smarthome.io.rest.internal;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.io.rest.DTOMapper;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DTOMapper} implementation.
 *
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@Component
public class DTOMapperImpl implements DTOMapper {

    private final Logger logger = LoggerFactory.getLogger(DTOMapperImpl.class);

    @Override
    public <T> Stream<T> limitToFields(Stream<T> itemStream, String fields) {
        if (fields == null || fields.trim().isEmpty()) {
            return itemStream;
        }
        List<String> fieldList = Stream.of(fields.split(",")).map(field -> field.trim()).collect(Collectors.toList());
        return itemStream.map(dto -> {
            for (Field field : dto.getClass().getFields()) {
                if (!fieldList.contains(field.getName())) {
                    try {
                        field.set(dto, null);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        logger.warn("Field '{}' could not be eliminated: {}", field.getName(), e.getMessage());
                    }
                }
            }
            return dto;
        });
    }

}
