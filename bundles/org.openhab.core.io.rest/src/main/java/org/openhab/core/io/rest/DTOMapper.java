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
package org.openhab.core.io.rest;

import java.util.stream.Stream;

/**
 * Utilities for mapping/transforming DTOs.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public interface DTOMapper {

    <T> Stream<T> limitToFields(Stream<T> itemStream, String fields);

}
