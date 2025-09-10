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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap Input widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Input extends NonLinkableWidget {

    /**
     * Get the input hint. This can be used by a UI to tailor the representation. See {@link #setInputHint(String)}.
     *
     * @return input hint
     */
    @Nullable
    String getInputHint();

    /**
     * Set the input hint, allowed values are: "text", "number", "date", "time", "datetime". This can be used by a UI to
     * tailor the representation.
     *
     * @param inputHint
     */
    void setInputHint(@Nullable String inputHint);
}
