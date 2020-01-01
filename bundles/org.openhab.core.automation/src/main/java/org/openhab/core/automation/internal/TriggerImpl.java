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
package org.openhab.core.automation.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Trigger;
import org.openhab.core.config.core.Configuration;

/**
 * This class is implementation of {@link Trigger} modules used in the {@link RuleEngine}s.
 *
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public class TriggerImpl extends ModuleImpl implements Trigger {

    public TriggerImpl(String id, String typeUID, @Nullable Configuration configuration, @Nullable String label,
            @Nullable String description) {
        super(id, typeUID, configuration, label, description);
    }

}
