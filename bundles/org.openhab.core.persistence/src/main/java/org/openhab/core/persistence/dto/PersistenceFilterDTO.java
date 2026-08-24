/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.persistence.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@link org.openhab.core.persistence.dto.PersistenceFilterDTO} is used for transferring persistence filter
 * configurations
 *
 * @author Jan N. Klug - Initial contribution
 */
@Schema(name = "PersistenceFilter")
public class PersistenceFilterDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;

    // threshold and time
    public BigDecimal value;

    // threshold
    public Boolean relative;

    // threshold, include/exclude
    public String unit;

    // include/exclude
    public BigDecimal lower;
    public BigDecimal upper;

    // equals/not equals
    public List<String> values;

    // equals/not equals, include/exclude
    public Boolean inverted;
}
