/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.rest.ui;

import java.util.Date;

import org.openhab.core.ui.components.RootUIComponent;

/**
 * A {@link RootUIComponent} enriched with runtime-computed information for REST API responses.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public class EnrichedRootUIComponent extends RootUIComponent {

    /**
     * Whether the component can be modified or deleted through the REST API.
     * Components loaded from YAML configuration files are not editable.
     */
    public boolean editable;

    public EnrichedRootUIComponent(RootUIComponent component, boolean editable) {
        super(component.getUID(), component.getType());
        setConfig(component.getConfig());
        setSlots(component.getSlots());
        if (component.getTags() != null) {
            addTags(component.getTags());
        }
        if (component.getTimestamp() != null) {
            Date ts = component.getTimestamp();
            if (ts != null) {
                setTimestamp(ts);
            }
        }
        setProps(component.getProps());
        this.editable = editable;
    }
}
