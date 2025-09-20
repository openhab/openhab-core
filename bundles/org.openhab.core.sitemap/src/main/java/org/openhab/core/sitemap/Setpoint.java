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
package org.openhab.core.sitemap;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap Setpoint widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Setpoint extends NonLinkableWidget {

    /**
     * Get minimum setpoint value.
     *
     * @return minValue
     */
    @Nullable
    BigDecimal getMinValue();

    /**
     * Set minimum setpoint value.
     *
     * @param minValue
     */
    void setMinValue(@Nullable BigDecimal minValue);

    /**
     * Get maximum setpoint value.
     *
     * @return maxValue
     */
    @Nullable
    BigDecimal getMaxValue();

    /**
     * Set maximum setpoint value.
     *
     * @param maxValue
     */
    void setMaxValue(@Nullable BigDecimal maxValue);

    /**
     * Get setpoint step.
     *
     * @return step
     */
    @Nullable
    BigDecimal getStep();

    /**
     * Set setpoint step.
     *
     * @param step
     */
    void setStep(@Nullable BigDecimal step);
}
