/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.ephemeris.EphemerisManager;
import org.openhab.core.model.script.actions.Ephemeris;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the ephemeris action.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@Component
@NonNullByDefault
public class EphemerisActionService implements ActionService {

    public static @Nullable EphemerisManager ephemerisManager;

    @Activate
    public EphemerisActionService(final @Reference EphemerisManager ephemerisManager) {
        EphemerisActionService.ephemerisManager = ephemerisManager;
    }

    @Override
    public Class<?> getActionClass() {
        return Ephemeris.class;
    }
}
