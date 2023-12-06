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
package org.openhab.core.io.rest.internal;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.DTOMapper;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DTOMapper} implementation.
 *
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Component
@NonNullByDefault
public class DTOMapperImpl implements DTOMapper {

    private final Logger logger = LoggerFactory.getLogger(DTOMapperImpl.class);

    @Override
    public <@NonNull T> Stream<T> limitToFields(Stream<T> itemStream, @Nullable String fields) {
        if (fields == null || fields.trim().isEmpty()) {
            return itemStream;
        }
        List<String> fieldList = Stream.of(fields.split(",")).map(String::trim).toList();
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
