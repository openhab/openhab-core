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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Input;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class InputImpl extends NonLinkableWidgetImpl implements Input {

    private @Nullable String inputHint;

    public InputImpl() {
        super();
    }

    public InputImpl(Parent parent) {
        super(parent);
    }

    @Override
    public @Nullable String getInputHint() {
        return inputHint;
    }

    @Override
    public void setInputHint(@Nullable String inputHint) {
        this.inputHint = inputHint;
    }
}
