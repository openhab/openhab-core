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
package org.eclipse.smarthome.core.thing.type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.types.StateDescription;

/**
 * Interface for builders for {@link ChannelType}s of kind STATE
 *
 * @author Stefan Triller - Initial Contribution
 *
 */
@NonNullByDefault
public interface StateChannelTypeBuilder extends ChannelTypeBuilder<StateChannelTypeBuilder> {

    /**
     * Sets the StateDescription for the ChannelType
     *
     * @param stateDescription StateDescription for the ChannelType
     * @return this Builder
     */
    StateChannelTypeBuilder withStateDescription(@Nullable StateDescription stateDescription);

    /**
     * Sets the auto update policy for the ChannelType
     *
     * @param autoUpdatePolicy the auto update policy
     * @return this builder
     */
    StateChannelTypeBuilder withAutoUpdatePolicy(@Nullable AutoUpdatePolicy autoUpdatePolicy);

}
