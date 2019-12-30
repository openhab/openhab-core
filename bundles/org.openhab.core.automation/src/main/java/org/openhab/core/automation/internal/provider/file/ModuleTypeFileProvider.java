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
package org.openhab.core.automation.internal.provider.file;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;

/**
 * This class is implementation of {@link ModuleTypeProvider}. It extends functionality of {@link AbstractFileProvider}
 * for importing the {@link ModuleType}s from local files.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
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
    public <T extends ModuleType> @Nullable T getModuleType(String UID, @Nullable Locale locale) {
        return (T) providedObjectsHolder.get(UID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(@Nullable Locale locale) {
        Collection<ModuleType> values = providedObjectsHolder.values();
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return (Collection<T>) new LinkedList<>(values);
    }
}
