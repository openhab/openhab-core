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
package org.openhab.core.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface returned by all scheduled jobs. It can be used to wait for the value,
 * cancel the job or check how much time till the scheduled job will run.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface ScheduledCompletableFuture<T> extends ScheduledFuture<T> {
    /**
     * @return Returns the {@link CompletableFuture} associated with the scheduled job.
     */
    CompletableFuture<T> getPromise();
}
