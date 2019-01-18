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
package org.eclipse.smarthome.automation.internal.provider.file;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;

/**
 * This class is implementation of {@link ModuleTypeProvider}. It extends functionality of {@link AbstractFileProvider}
 * for importing the {@link ModuleType}s from local files.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public abstract class ModuleTypeFileProvider extends AbstractFileProvider<ModuleType> implements ModuleTypeProvider {

    public ModuleTypeFileProvider() {
        super("moduletypes");
    }

    @Override
    protected String getUID(ModuleType providedObject) {
        return providedObject.getUID();
    }

    @Override
    public Collection<ModuleType> getAll() {
        return getModuleTypes(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> T getModuleType(String UID, Locale locale) {
        return (T) providedObjectsHolder.get(UID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(Locale locale) {
        Collection<ModuleType> values = providedObjectsHolder.values();
        if (values.isEmpty()) {
            return Collections.<T>emptyList();
        }
        return (Collection<T>) new LinkedList<ModuleType>(values);
    }
}
