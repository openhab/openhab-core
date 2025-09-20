/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.sitemap.internal;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Setpoint;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SetpointImpl extends NonLinkableWidgetImpl implements Setpoint {

    private @Nullable BigDecimal minValue;
    private @Nullable BigDecimal maxValue;
    private @Nullable BigDecimal step;

    public SetpointImpl() {
        super();
    }

    public SetpointImpl(Parent parent) {
        super(parent);
    }

    @Override
    public @Nullable BigDecimal getMinValue() {
        return minValue;
    }

    @Override
    public void setMinValue(@Nullable BigDecimal minValue) {
        this.minValue = minValue;
    }

    @Override
    public @Nullable BigDecimal getMaxValue() {
        return maxValue;
    }

    @Override
    public void setMaxValue(@Nullable BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public @Nullable BigDecimal getStep() {
        return step;
    }

    @Override
    public void setStep(@Nullable BigDecimal step) {
        this.step = step;
    }
}
