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
package org.openhab.core.persistence.strategy;

/**
 * This class holds a cron expression based strategy to persist items.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class PersistenceCronStrategy extends PersistenceStrategy {

    private final String cronExpression;

    public PersistenceCronStrategy(final String name, final String cronExpression) {
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
