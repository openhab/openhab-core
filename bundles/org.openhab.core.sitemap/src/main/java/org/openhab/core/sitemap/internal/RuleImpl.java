/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.sitemap.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Rule;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class RuleImpl implements Rule {

    private List<Condition> conditions = new CopyOnWriteArrayList<>();
    private @Nullable String argument;

    public RuleImpl() {
    }

    public RuleImpl(Rule rule) {
        this.conditions = rule.getConditions();
        this.argument = rule.getArgument();
    }

    @Override
    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public void setConditions(List<Condition> conditions) {
        this.conditions = new CopyOnWriteArrayList<>(conditions);
    }

    @Override
    public @Nullable String getArgument() {
        return argument;
    }

    @Override
    public void setArgument(@Nullable String argument) {
        this.argument = argument;
    }
}
