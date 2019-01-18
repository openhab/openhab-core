/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.script.internal.engine.action;

import org.eclipse.smarthome.model.persistence.extensions.PersistenceExtensions;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;

/**
 * This class registers an OSGi service for the Persistence action.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component
public class PersistenceActionService implements ActionService {

    public PersistenceActionService() {
    }

    @Override
    public String getActionClassName() {
        return PersistenceExtensions.class.getCanonicalName();
    }

    @Override
    public Class<?> getActionClass() {
        return PersistenceExtensions.class;
    }

}
