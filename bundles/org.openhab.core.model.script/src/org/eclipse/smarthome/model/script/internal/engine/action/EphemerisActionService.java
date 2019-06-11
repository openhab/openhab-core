/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.model.script.internal.engine.action;

import org.eclipse.smarthome.core.ephemeris.EphemerisManager;
import org.eclipse.smarthome.model.script.actions.Ephemeris;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the ephemeris action.
 *
 * @author Gaël L'hopital - Initial contribution and API
 */
@Component
public class EphemerisActionService implements ActionService {

    public static EphemerisManager ephemerisManager;

    @Override
    public Class<?> getActionClass() {
        return Ephemeris.class;
    }

    @Reference
    protected void setEphemerisManager(EphemerisManager ephemerisManager) {
        EphemerisActionService.ephemerisManager = ephemerisManager;
    }

    protected void unsetEphemerisManager(EphemerisManager ephemerisManager) {
        EphemerisActionService.ephemerisManager = null;
    }

}