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
package org.openhab.core.model.persistence.runtime.internal;

import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.persistence.PersistenceStandaloneSetup;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class PersistenceRuntimeActivator implements ModelParser {

    private final Logger logger = LoggerFactory.getLogger(PersistenceRuntimeActivator.class);

    public void activate() throws Exception {
        PersistenceStandaloneSetup.doSetup();
        logger.debug("Registered 'persistence' configuration parser");
    }

    public void deactivate() throws Exception {
        PersistenceStandaloneSetup.unregister();
    }

    @Override
    public String getExtension() {
        return "persist";
    }

}
