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
package org.eclipse.smarthome.core.thing.xml.internal;

/**
 * Utility class containing helper methods to be used in XML generation.
 *
 * @author Ivan Iliev - Initial contribution
 *
 */
public class XmlHelper {

    public static final String SYSTEM_NAMESPACE_PREFIX = "system.";
    private static final String SYSTEM_NAMESPACE = "system";

    /**
     * Returns a UID in the format of {1}:{2}, where {1} is {@link #SYSTEM_NAMESPACE} and {2} is the
     * given typeId stripped of the prefix {@link #SYSTEM_NAMESPACE_PREFIX} if it exists.
     *
     * @param typeId
     * @return system uid (e.g. "system:test")
     */
    public static String getSystemUID(String typeId) {
        String type;
        if (typeId.startsWith(SYSTEM_NAMESPACE_PREFIX)) {
            type = typeId.substring(SYSTEM_NAMESPACE_PREFIX.length());
        } else {
            type = typeId;
        }
        return String.format("%s:%s", SYSTEM_NAMESPACE, type);
    }
}
