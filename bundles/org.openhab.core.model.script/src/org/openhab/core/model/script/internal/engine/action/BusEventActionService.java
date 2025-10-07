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
package org.openhab.core.model.script.internal.engine.action;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.action.BusEvent;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the BusEvent action.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class BusEventActionService implements ActionService {

    private static @Nullable BusEvent busEvent;

    @Activate
    public BusEventActionService(final @Reference BusEvent busEvent) {
        BusEventActionService.busEvent = busEvent;
    }

    @Override
    public Class<?> getActionClass() {
        return BusEvent.class;
    }

    public static BusEvent getBusEvent() {
        return Objects.requireNonNull(busEvent);
    }
}
