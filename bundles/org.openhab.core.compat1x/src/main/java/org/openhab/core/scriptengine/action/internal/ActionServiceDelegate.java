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
package org.openhab.core.scriptengine.action.internal;

import org.eclipse.smarthome.model.script.engine.action.ActionService;

/**
 * This class serves as a mapping from the "old" org.openhab namespace to the new org.eclipse.smarthome
 * namespace for the action service. It wraps an instance with the old interface
 * into a class with the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class ActionServiceDelegate implements ActionService {

    private org.openhab.core.scriptengine.action.ActionService service;

    public ActionServiceDelegate(org.openhab.core.scriptengine.action.ActionService service) {
        this.service = service;
    }

    @Override
    public String getActionClassName() {
        return service.getActionClassName();
    }

    @Override
    public Class<?> getActionClass() {
        return service.getActionClass();
    }

}
