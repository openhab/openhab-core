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
package org.eclipse.smarthome.core.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface returned by all scheduled jobs. It can be used to wait for the value,
 * cancel the job or check how much time till the scheduled job will run.
 *
 * @author Hilbrand Bouwkamp - initial contribution
 */
@NonNullByDefault
public interface ScheduledCompletableFuture<T> extends ScheduledFuture<T> {
    /**
     * @return Returns the {@link CompletableFuture} associated with the scheduled job.
     */
    CompletableFuture<T> getPromise();
}