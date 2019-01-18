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
package org.eclipse.smarthome.core.persistence.strategy;

import java.util.Objects;

/**
 * This class holds a strategy to persist items.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public class SimpleStrategy {
    public static class Globals {
        public static final SimpleStrategy UPDATE = new SimpleStrategy("everyUpdate");
        public static final SimpleStrategy CHANGE = new SimpleStrategy("everyChange");
        public static final SimpleStrategy RESTORE = new SimpleStrategy("restoreOnStartup");
    }

    private final String name;

    public SimpleStrategy(final String name) {
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
        // if (getClass() != obj.getClass()) {
        // return false;
        // }
        if (!(obj instanceof SimpleStrategy)) {
            return false;
        }
        final SimpleStrategy other = (SimpleStrategy) obj;
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
