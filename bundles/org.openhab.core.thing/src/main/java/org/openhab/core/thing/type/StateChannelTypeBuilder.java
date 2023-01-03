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
package org.openhab.core.thing.type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.StateDescriptionFragment;

/**
 * Interface for builders for {@link ChannelType}s of kind STATE
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public interface StateChannelTypeBuilder extends ChannelTypeBuilder<StateChannelTypeBuilder> {
    /**
     * Sets the {@link StateDescriptionFragment} for the {@link ChannelType}
     *
     * @param stateDescriptionFragment StateDescriptionFragment for the ChannelType
     * @return this Builder
     */
    StateChannelTypeBuilder withStateDescriptionFragment(@Nullable StateDescriptionFragment stateDescriptionFragment);

    /**
     * Sets the {@link AutoUpdatePolicy} for the {@link ChannelType}
     *
     * @param autoUpdatePolicy the AutoUpdatePolicy for the ChannelType
     * @return this builder
     */
    StateChannelTypeBuilder withAutoUpdatePolicy(@Nullable AutoUpdatePolicy autoUpdatePolicy);

    /**
     * Sets the list of {@link CommandOption}s for the {@link ChannelType}
     *
     * @param commandOptions the list of {@link CommandOption}s
     * @return this builder
     */
    StateChannelTypeBuilder withCommandDescription(@Nullable CommandDescription commandDescription);
}
