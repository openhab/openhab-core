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
package org.openhab.core.binding.xml.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;

/**
 * The {@link BindingInfoXmlResult} is an intermediate XML conversion result object which
 * contains a mandatory {@link BindingInfo} and an optional {@link ConfigDescription} object.
 * <p>
 * If a {@link ConfigDescription} object exists, it must be added to the according {@link ConfigDescriptionProvider}.
 *
 * @author Michael Grammling - Initial contribution
 */
@NonNullByDefault
public class BindingInfoXmlResult {

    private BindingInfo bindingInfo;
    private @Nullable ConfigDescription configDescription;

    public BindingInfoXmlResult(BindingInfo bindingInfo, @Nullable ConfigDescription configDescription)
            throws IllegalArgumentException {
        if (bindingInfo == null) {
            throw new IllegalArgumentException("The BindingInfo must not be null!");
        }

        this.bindingInfo = bindingInfo;
        this.configDescription = configDescription;
    }

    public BindingInfo getBindingInfo() {
        return bindingInfo;
    }

    public @Nullable ConfigDescription getConfigDescription() {
        return configDescription;
    }

    @Override
    public String toString() {
        return "BindingInfoXmlResult [bindingInfo=" + bindingInfo + ", configDescription=" + configDescription + "]";
    }
}
