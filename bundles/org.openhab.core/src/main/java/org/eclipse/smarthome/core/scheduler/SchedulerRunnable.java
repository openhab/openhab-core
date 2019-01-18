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

/**
 * Runnable that can throw checked exceptions.
 *
 * @author Peter Kriens - initial contribution and API
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - moved to it's own class and renamed
 */
public interface SchedulerRunnable {

    /**
     * Scheduled job to run.
     * 
     * @throws Exception exception thrown
     */
    void run() throws Exception;
}
