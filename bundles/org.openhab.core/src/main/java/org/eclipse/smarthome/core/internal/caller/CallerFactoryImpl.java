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
package org.eclipse.smarthome.core.internal.caller;

import java.util.concurrent.Executors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.caller.Caller;
import org.eclipse.smarthome.core.caller.CallerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Factory for a caller
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
@Component(service = { CallerFactory.class })
public class CallerFactoryImpl implements CallerFactory {

    @Override
    public Caller create(String id, int fixedThreadPoolSize) {
        return new CallerImpl(id,
                Executors.newFixedThreadPool(fixedThreadPoolSize, CallerImpl.newExecutorThreadFactory(id)), true);
    }

}
