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
package org.openhab.core.persistence.strategy;

import java.util.Objects;

/**
 * This class holds a strategy to persist items.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class PersistenceStrategy {
    public static class Globals {
        public static final PersistenceStrategy UPDATE = new PersistenceStrategy("everyUpdate");
        public static final PersistenceStrategy CHANGE = new PersistenceStrategy("everyChange");
        public static final PersistenceStrategy RESTORE = new PersistenceStrategy("restoreOnStartup");
    }

    private final String name;

    public PersistenceStrategy(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PersistenceStrategy)) {
            return false;
        }
        final PersistenceStrategy other = (PersistenceStrategy) obj;
        if (!Objects.equals(name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s]", getClass().getSimpleName(), name);
    }

}
