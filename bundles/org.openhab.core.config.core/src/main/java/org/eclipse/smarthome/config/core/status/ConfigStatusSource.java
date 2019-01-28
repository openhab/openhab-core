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

import org.eclipse.jdt.annotation.NonNull;

/**
 * The {@link ConfigStatusSource} represents a source which would like to propagate its new configuration status. It is
 * used as input for {@link ConfigStatusCallback#configUpdated(ConfigStatusSource)}.
 *
 * @author Thomas Höfer - Initial contribution
 */
public abstract class ConfigStatusSource {

    /** The id of the entity whose new configuration status is to be propagated. */
    public final String entityId;

    /**
     * Creates a new config status source object.
     *
     * @param entityId the id of the entity whose new configuration status is to be propagated
     */
    public ConfigStatusSource(@NonNull String entityId) {
        super();
        this.entityId = entityId;
    }

    /**
     * @return the topic over which the new configuration status is to be propagated
     */
    public abstract String getTopic();

}
