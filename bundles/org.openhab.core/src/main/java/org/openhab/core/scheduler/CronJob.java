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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Runnable that can be passed data and can throw a checked exception.
 *
 * @author Peter Kriens - Initial contribution
 */
@NonNullByDefault
public interface CronJob {
    /**
     * The service property that specifies the cron schedule. The type is String+.
     */
    String CRON = "cron";

    /**
     * Run a cron job.
     *
     * @param data The data for the job
     * @throws Exception Exception thrown
     */
    public void run(Map<String, Object> data) throws Exception;
}
