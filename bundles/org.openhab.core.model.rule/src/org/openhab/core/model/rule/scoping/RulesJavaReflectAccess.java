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
package org.openhab.core.model.rule.scoping;

import org.eclipse.xtext.common.types.access.impl.ClassFinder;
import org.eclipse.xtext.common.types.util.JavaReflectAccess;

import com.google.inject.Inject;

/**
 * This is a customized version of {@link JavaReflectAccess}.
 *
 * It allows for removing and updating classes in the cache used by the {@link RulesClassFinder} when add-ons are
 * installed or updated.
 *
 * @author Wouter Born - Initial contribution
 */
public class RulesJavaReflectAccess extends JavaReflectAccess {

    private ClassLoader classLoader = getClass().getClassLoader();

    private ClassFinder classFinder;

    @Override
    @Inject(optional = true)
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader != this.classLoader) {
            this.classLoader = classLoader;
            classFinder = null;
        }
    }

    @Override
    public ClassFinder getClassFinder() {
        if (classFinder == null) {
            classFinder = new RulesClassFinder(classLoader);
        }
        return classFinder;
    }
}
