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
package org.eclipse.smarthome.config.core.status;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * The {@link ConfigStatusProvider} can be implemented and registered as an <i>OSGi</i> service to provide status
 * information for {@link Configuration}s of entities. The {@link ConfigStatusService} tracks each
 * {@link ConfigStatusProvider} and provides the corresponding {@link ConfigStatusInfo} by the operation
 * {@link ConfigStatusService#getConfigStatus(String, Locale)}.
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public interface ConfigStatusProvider {

    /**
     * Returns the configuration status in form of a collection of {@link ConfigStatusMessage}s for the
     * {@link Configuration} of the entity that is supported by this {@link ConfigStatusProvider}.
     *
     * @return the requested configuration status (not null)
     */
    Collection<ConfigStatusMessage> getConfigStatus();

    /**
     * Determines if the {@link ConfigStatusProvider} instance can provide the configuration status information for the
     * given entity.
     *
     * @param entityId the id of the entity whose configuration status information is to be provided
     * @return true, if the {@link ConfigStatusProvider} instance supports the given entity, otherwise false
     */
    boolean supportsEntity(String entityId);

    /**
     * Sets the given {@link ConfigStatusCallback} for the {@link ConfigStatusProvider}.
     *
     * @param configStatusCallback the configuration status callback to be set
     */
    void setConfigStatusCallback(@Nullable ConfigStatusCallback configStatusCallback);

}
