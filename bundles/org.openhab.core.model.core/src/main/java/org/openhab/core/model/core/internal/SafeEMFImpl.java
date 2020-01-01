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
package org.openhab.core.model.core.internal;

import java.util.function.Supplier;

import org.openhab.core.model.core.SafeEMF;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of a safe EMF caller..
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component
public class SafeEMFImpl implements SafeEMF {

    @Override
    public synchronized <T> T call(Supplier<T> func) {
        return func.get();
    }

    @Override
    public synchronized void call(Runnable func) {
        func.run();
    }

}
