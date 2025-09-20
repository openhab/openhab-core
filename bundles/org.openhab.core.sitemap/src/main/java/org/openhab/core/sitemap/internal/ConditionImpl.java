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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Condition;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class ConditionImpl implements Condition {

    private @Nullable String item;
    private @Nullable String condition;
    private String value = "";

    public ConditionImpl() {
    }

    public ConditionImpl(Condition condition) {
        this.item = condition.getItem();
        this.condition = condition.getCondition();
        this.value = condition.getValue();
    }

    @Override
    public @Nullable String getItem() {
        return item;
    }

    @Override
    public void setItem(@Nullable String item) {
        this.item = item;
    }

    @Override
    public @Nullable String getCondition() {
        return condition;
    }

    @Override
    public void setCondition(@Nullable String condition) {
        this.condition = condition;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }
}
