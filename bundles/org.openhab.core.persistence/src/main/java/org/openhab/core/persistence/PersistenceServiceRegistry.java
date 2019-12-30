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
package org.openhab.core.persistence;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is the interface for a central service that provides access to {@link PersistenceService}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface PersistenceServiceRegistry {

    /**
     * Get the default persistence service.
     *
     * @return {@link PersistenceService} default service
     */
    @Nullable
    PersistenceService getDefault();

    /**
     * Get the persistence service with the given id.
     *
     * @param serviceId the service id
     * @return {@link PersistenceService} the service with the id or null, if not present
     */
    @Nullable
    PersistenceService get(@Nullable String serviceId);

    /**
     * Get the id of the default persistence service.
     *
     * @return the id of the default persistence service or null, if no default service is defined
     */
    @Nullable
    String getDefaultId();

    /**
     * Returns all available persistence services.
     *
     * @return all available persistence services
     */
    Set<PersistenceService> getAll();

}
