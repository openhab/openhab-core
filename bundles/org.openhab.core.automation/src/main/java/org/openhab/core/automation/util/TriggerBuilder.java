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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.TriggerImpl;

/**
 * This class allows the easy construction of a {@link Trigger} instance using the builder pattern.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class TriggerBuilder extends ModuleBuilder<TriggerBuilder, Trigger> {

    protected TriggerBuilder() {
        super();
    }

    protected TriggerBuilder(final Trigger condition) {
        super(condition);
    }

    public static TriggerBuilder create() {
        return new TriggerBuilder();
    }

    public static TriggerBuilder create(final Trigger trigger) {
        return new TriggerBuilder(trigger);
    }

    @Override
    public Trigger build() {
        return new TriggerImpl(getId(), getTypeUID(), configuration, label, description);
    }

}
