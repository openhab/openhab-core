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
package org.eclipse.smarthome.core.thing.internal.type;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.type.AutoUpdatePolicy;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.StateChannelTypeBuilder;
import org.eclipse.smarthome.core.types.StateDescription;

/**
 * StateChannelTypeBuilder to create {@link ChannelType}s of kind STATE
 *
 * @author Stefan Triller - Initial contribution
 *
 */
@NonNullByDefault
public class StateChannelTypeBuilderImpl extends AbstractChannelTypeBuilder<StateChannelTypeBuilder>
        implements StateChannelTypeBuilder {

    private final String itemType;
    private @Nullable StateDescription stateDescription;
    private @Nullable AutoUpdatePolicy autoUpdatePolicy;

    public StateChannelTypeBuilderImpl(ChannelTypeUID channelTypeUID, String label, String itemType) {
        super(channelTypeUID, label);

        if (StringUtils.isEmpty(itemType)) {
            throw new IllegalArgumentException("Supported itemType for a ChannelType must not be empty.");
        }

        this.itemType = itemType;
    }

    @Override
    public StateChannelTypeBuilder withStateDescription(@Nullable StateDescription stateDescription) {
        this.stateDescription = stateDescription;
        return this;
    }

    @Override
    public StateChannelTypeBuilder withAutoUpdatePolicy(@Nullable AutoUpdatePolicy autoUpdatePolicy) {
        this.autoUpdatePolicy = autoUpdatePolicy;
        return this;
    }

    @Override
    public ChannelType build() {
        return new ChannelType(channelTypeUID, advanced, itemType, ChannelKind.STATE, label, description, category,
                tags.isEmpty() ? null : tags, stateDescription, null, configDescriptionURI, autoUpdatePolicy);
    }

}
