/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Runnable that can throw checked exceptions.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - adapted to CompletableFutures
 * @author Hilbrand Bouwkamp - moved to it's own class and renamed
 */
@NonNullByDefault
public interface SchedulerRunnable {

    /**
     * Scheduled job to run.
     *
     * @throws Exception exception thrown
     */
    void run() throws Exception;
}
