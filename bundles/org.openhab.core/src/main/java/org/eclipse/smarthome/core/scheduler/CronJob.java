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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Runnable that can be passed data and can throw a checked exception.
 *
 * @author Peter Kriens - initial contribution and API
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
