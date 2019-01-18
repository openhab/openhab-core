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
package org.eclipse.smarthome.core.persistence;

/**
 * A persistence manager service which could be used to start event handling or supply configuration for persistence
 * services.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public interface PersistenceManager {
    /**
     * Add a configuration for a persistence service.
     *
     * @param dbId the database id used by the persistence service
     * @param config the configuration of the persistence service
     */
    void addConfig(String dbId, PersistenceServiceConfiguration config);

    /**
     * Remove a configuration for a persistence service.
     *
     * @param dbId the database id used by the persistence service
     */
    void removeConfig(String dbId);

}
