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
package org.eclipse.smarthome.model.script.scoping;

import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.model.script.ScriptServiceUtil;
import org.eclipse.smarthome.model.script.engine.action.ActionService;

/**
 * This is a special class loader that tries to resolve classes from available {@link ActionService}s,
 * if the class cannot be resolved from the normal classpath.
 *
 * @author Kai Kreuzer
 *
 */
final public class ActionClassLoader extends ClassLoader {

    public ActionClassLoader(ClassLoader cl) {
        super(cl);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            Class<?> clazz = getParent().loadClass(name);
            return clazz;
        } catch (ClassNotFoundException e) {
            for (ActionService actionService : ScriptServiceUtil.getActionServices()) {
                if (actionService.getActionClassName().equals(name)) {
                    return actionService.getActionClass();
                }
            }
            for (ThingActions actions : ScriptServiceUtil.getThingActions()) {
                if (actions.getClass().getName().equals(name)) {
                    return actions.getClass();
                }
            }
        }
        throw new ClassNotFoundException();
    }
}
