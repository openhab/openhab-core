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
package org.openhab.core.automation.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.internal.ConditionImpl;

/**
 * This class allows the easy construction of a {@link Condition} instance using the builder pattern.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class ConditionBuilder extends ModuleBuilder<ConditionBuilder, Condition> {

    private @Nullable Map<String, String> inputs;

    protected ConditionBuilder() {
        super();
    }

    protected ConditionBuilder(final Condition condition) {
        super(condition);
        this.inputs = condition.getInputs();
    }

    public static ConditionBuilder create() {
        return new ConditionBuilder();
    }

    public static ConditionBuilder create(final Condition condition) {
        return new ConditionBuilder(condition);
    }

    public ConditionBuilder withInputs(@Nullable Map<String, String> inputs) {
        this.inputs = inputs != null ? Collections.unmodifiableMap(new HashMap<>(inputs)) : null;
        return this;
    }

    @Override
    public Condition build() {
        return new ConditionImpl(getId(), getTypeUID(), configuration, label, description, inputs);
    }
}
