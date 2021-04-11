/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Expected execution of an {@link Rule}.
 *
 * @author Sönke Küper - Initial contribution
 */
@NonNullByDefault
public final class RuleExecution implements Comparable<RuleExecution> {

    private final Date date;
    private final Rule rule;

    /**
     * Creates an new {@link RuleExecution}.
     *
     * @param date The time when the rule will be executed.
     * @param rule The rule that will be executed.
     */
    public RuleExecution(Date date, Rule rule) {
        this.date = date;
        this.rule = rule;
    }

    /**
     * Returns the time when the rule will be executed.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Returns the rule that will be executed.
     */
    public Rule getRule() {
        return rule;
    }

    @Override
    public int compareTo(RuleExecution o) {
        return this.date.compareTo(o.getDate());
    }
}
