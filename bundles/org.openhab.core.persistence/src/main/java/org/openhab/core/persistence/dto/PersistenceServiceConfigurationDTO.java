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
package org.openhab.core.persistence.dto;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link PersistenceServiceConfigurationDTO} is used for transferring persistence service configurations
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceServiceConfigurationDTO {
    public String serviceId = "";
    public Collection<PersistenceItemConfigurationDTO> configs = List.of();
    public Collection<String> defaults = List.of();
    public Collection<PersistenceCronStrategyDTO> cronStrategies = List.of();
    public Collection<PersistenceFilterDTO> thresholdFilters = List.of();
    public Collection<PersistenceFilterDTO> timeFilters = List.of();

    public boolean editable = false;
}
