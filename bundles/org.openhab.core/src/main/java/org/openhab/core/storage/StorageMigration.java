/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.storage;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The StorageMigration can be used to convert old legacy classes to new classes.
 *
 * @author Simon Lamon - Initial contribution
 */
@NonNullByDefault
public abstract class StorageMigration {
    private @Nullable String oldEntityClassName;
    private Class<?> oldEntityClass;
    private @Nullable String newEntityClassName;
    private Class<?> newEntityClass;

    public StorageMigration(Class<?> oldEntityClass, Class<?> newEntityClass) {
        super();
        this.oldEntityClass = oldEntityClass;
        this.newEntityClass = newEntityClass;
    }

    public StorageMigration(String oldEntityClassName, Class<?> oldEntityClass, String newEntityClassName,
            Class<?> newEntityClass) {
        super();
        this.oldEntityClassName = oldEntityClassName;
        this.oldEntityClass = oldEntityClass;
        this.newEntityClassName = newEntityClassName;
        this.newEntityClass = newEntityClass;
    }

    public @Nullable String getOldEntityClassName() {
        return oldEntityClassName;
    }

    public @Nullable String getNewEntityClassName() {
        return newEntityClassName;
    }

    public Class<?> getOldEntityClass() {
        return oldEntityClass;
    }

    public Class<?> getNewEntityClass() {
        return newEntityClass;
    }

    public abstract Object migrate(Object in);
}
