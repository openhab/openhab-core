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
package org.eclipse.smarthome.core.persistence.strategy;

/**
 * This class holds a cron expression based strategy to persist items.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public class SimpleCronStrategy extends SimpleStrategy {

    private final String cronExpression;

    public SimpleCronStrategy(final String name, final String cronExpression) {
        super(name);
        this.cronExpression = cronExpression;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    @Override
    public String toString() {
        return String.format("%s [%s, cronExpression=%s]", getClass().getSimpleName(), super.toString(),
                cronExpression);
    }

}
