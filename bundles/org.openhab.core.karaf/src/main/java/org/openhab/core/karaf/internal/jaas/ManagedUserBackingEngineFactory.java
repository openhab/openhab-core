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
package org.openhab.core.karaf.internal.jaas;

import java.util.Map;

import javax.inject.Singleton;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.openhab.core.auth.UserRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A Karaf backing engine factory for the {@link UserRegistry}
 *
 * @author Yannick Schaus - initial contribution
 */
@Singleton
@Component(service = BackingEngineFactory.class)
public class ManagedUserBackingEngineFactory implements BackingEngineFactory {

    UserRegistry userRegistry;

    @Activate
    public ManagedUserBackingEngineFactory(@Reference UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public String getModuleClass() {
        return ManagedUserRealm.MODULE_CLASS;
    }

    @Override
    public BackingEngine build(Map<String, ?> options) {
        return new ManagedUserBackingEngine(userRegistry);
    }
}
