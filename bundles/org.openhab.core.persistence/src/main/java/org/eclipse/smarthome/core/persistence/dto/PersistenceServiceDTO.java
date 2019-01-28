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
package org.eclipse.smarthome.core.persistence.dto;

/**
 * This is a java bean that is used to serialize services to JSON.
 *
 * @author Chris Jackson - Initial Contribution
 *
 */
public class PersistenceServiceDTO {
    public PersistenceServiceDTO() {
    }

    /**
     * Service Id
     */
    public String id;

    /**
     * Service label
     */
    public String label;

    /**
     * Persistence service class
     */
    public String type;
}
