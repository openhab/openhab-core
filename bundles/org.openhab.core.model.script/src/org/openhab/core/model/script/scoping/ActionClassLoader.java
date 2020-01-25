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
package org.openhab.core.model.script.scoping;

import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.thing.binding.ThingActions;

/**
 * This is a special class loader that tries to resolve classes from available {@link ActionService}s,
 * if the class cannot be resolved from the normal classpath.
 *
 * @author Kai Kreuzer - Initial contribution
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
