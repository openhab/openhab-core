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

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.caller.Caller;
import org.eclipse.smarthome.core.caller.CallerFactory;
import org.eclipse.smarthome.core.caller.ExecutionConstraints;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * An OSGi component providing the {@link Caller} interface.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
@Component(name = "openhab.callerservice")
public class CallerService implements Caller {

    public @interface Config {
        int threads() default 4;
    }

    private final Caller caller;

    @Activate
    public CallerService(final @Reference CallerFactory callerFactory, Config config) {
        this.caller = callerFactory.create("service", config.threads());
    }

    @Override
    @Deactivate
    public void close() {
        caller.close();
    }

    @Override
    public <R> CompletionStage<R> exec(Supplier<R> func, ExecutionConstraints constraints) {
        return caller.exec(func, constraints);
    }

    @Override
    public <R> CompletionStage<R> execSync(Supplier<R> func, ExecutionConstraints constraints) {
        return caller.execSync(func, constraints);
    }

    @Override
    public <R> CompletionStage<R> execAsync(Supplier<R> func, ExecutionConstraints constraints) {
        return caller.execAsync(func, constraints);
    }

}
