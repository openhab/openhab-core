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
package org.openhab.core.io.transport.serial;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Holds the {@link PathType}, which specifies whether its a local or remote path and the scheme.
 *
 * @author Matthias Steigenberger - Initial contribution
 */
@NonNullByDefault
public class ProtocolType {

    /**
     * Remote (NET) or Local path.
     *
     */
    public enum PathType {
        NET,
        LOCAL;

        public static PathType fromURI(URI uri) {
            return uri.getSchemeSpecificPart().startsWith("//") ? NET : LOCAL;
        }
    }

    private final PathType pathType;
    private final String scheme;

    public ProtocolType(PathType pathType, String scheme) {
        this.pathType = pathType;
        this.scheme = scheme;
    }

    public PathType getPathType() {
        return pathType;
    }

    public String getScheme() {
        return scheme;
    }

    @Override
    public String toString() {
        return "ProtocolType [pathType=" + pathType + ", scheme=" + scheme + "]";
    }
}
