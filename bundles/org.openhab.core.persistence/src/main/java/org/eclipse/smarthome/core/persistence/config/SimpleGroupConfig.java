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
package org.eclipse.smarthome.core.persistence.config;

/**
 * This class represents the configuration that is used for group items.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public class SimpleGroupConfig extends SimpleConfig {

    private final String group;

    public SimpleGroupConfig(final String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return String.format("%s [group=%s]", getClass().getSimpleName(), group);
    }

}
