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
 * A representation of a sitemap Slider widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Slider extends NonLinkableWidget {

    /**
     * Return true if the UI should render switch capabilities for the slider.
     *
     * @return true if switch enabled
     */
    boolean isSwitchEnabled();

    /**
     * Set switch enabled.
     *
     * @param switchEnabled
     */
    void setSwitchEnabled(@Nullable Boolean switchEnabled);

    /**
     * Return true if the UI should only send updates to core on mouse release.
     *
     * @return true if release only
     */
    boolean isReleaseOnly();

    /**
     * Set release only.
     *
     * @param releaseOnly
     */
    void setReleaseOnly(@Nullable Boolean releaseOnly);

    /**
     * Get minimum slider value.
     *
     * @return minValue
     */
    @Nullable
    BigDecimal getMinValue();

    /**
     * Set minimum slider value.
     *
     * @param minValue
     */
    void setMinValue(@Nullable BigDecimal minValue);

    /**
     * Get maximum slider value.
     *
     * @return maxValue
     */
    @Nullable
    BigDecimal getMaxValue();

    /**
     * Set maximum slider value.
     *
     * @param maxValue
     */
    void setMaxValue(@Nullable BigDecimal maxValue);

    /**
     * Get slider step.
     *
     * @return step
     */
    @Nullable
    BigDecimal getStep();

    /**
     * Set slider step.
     *
     * @param step
     */
    void setStep(@Nullable BigDecimal step);
}
