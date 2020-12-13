/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.osgi.service.component.annotations.Component;

/**
 * This class registers an OSGi service for the Persistence action.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
public class PersistenceActionService implements ActionService {

    public PersistenceActionService() {
    }

    @Override
    public Class<?> getActionClass() {
        return PersistenceExtensions.class;
    }

}
