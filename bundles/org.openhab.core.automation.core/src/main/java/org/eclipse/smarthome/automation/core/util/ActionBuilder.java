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
package org.eclipse.smarthome.automation.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.core.internal.ActionImpl;

/**
 * This class allows the easy construction of an {@link Action} instance using the builder pattern.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
@NonNullByDefault
public class ActionBuilder extends ModuleBuilder<ActionBuilder, Action> {

    private @Nullable Map<String, String> inputs;

    protected ActionBuilder() {
        super();
    }

    protected ActionBuilder(final Action action) {
        super(action);
        this.inputs = action.getInputs();
    }

    public static ActionBuilder create() {
        return new ActionBuilder();
    }

    public static ActionBuilder create(final Action action) {
        return new ActionBuilder(action);
    }

    public ActionBuilder withInputs(@Nullable Map<String, String> inputs) {
        this.inputs = inputs != null ? Collections.unmodifiableMap(new HashMap<>(inputs)) : null;
        return this;
    }

    @Override
    public Action build() {
        return new ActionImpl(getId(), getTypeUID(), configuration, label, description, inputs);
    }

}
