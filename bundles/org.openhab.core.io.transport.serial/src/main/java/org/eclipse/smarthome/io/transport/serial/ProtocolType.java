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
package org.eclipse.smarthome.io.transport.serial;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Holds the {@link PathType}, which specifies whether its a local or remote path and the scheme.
 *
 * @author Matthias Steigenberger - Initial Contribution
 *
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

}
