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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.sitemap.ButtonDefinition;
import org.openhab.core.sitemap.Buttongrid;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class ButtongridImpl extends LinkableWidgetImpl implements Buttongrid {

    private List<ButtonDefinition> buttons = new CopyOnWriteArrayList<>();

    public ButtongridImpl() {
        super();
    }

    public ButtongridImpl(Parent parent) {
        super(parent);
    }

    @Override
    public List<ButtonDefinition> getButtons() {
        return buttons;
    }

    @Override
    public void setButtons(List<ButtonDefinition> buttons) {
        this.buttons = new CopyOnWriteArrayList<>(buttons);
    }
}
