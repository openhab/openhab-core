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
package org.openhab.core.automation.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;

/**
 * This interface should be implemented by external modules which provide functionality for processing {@link Action}
 * modules. This functionality is called to execute the {@link Action}s of the {@link Rule} when it is needed.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 * @see ModuleHandler
 */
@NonNullByDefault
public interface ActionHandler extends ModuleHandler {

    /**
     * Called to execute an {@link Action} of the {@link Rule} when it is needed.
     *
     * @param context an unmodifiable map containing the outputs of the {@link Trigger} that triggered the {@link Rule},
     *            the outputs of all preceding {@link Action}s, and the inputs for this {@link Action}.
     * @return a map with the {@code outputs} which are the result of the {@link Action}'s execution (may be null).
     */
    @Nullable
    Map<String, Object> execute(Map<String, Object> context);

}
