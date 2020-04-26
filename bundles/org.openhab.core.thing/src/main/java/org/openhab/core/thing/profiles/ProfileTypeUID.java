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
package org.openhab.core.thing.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.UID;

/**
 * Identifier of a profile type.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class ProfileTypeUID extends UID {

    public static final String SYSTEM_SCOPE = "system";

    public ProfileTypeUID(String profileType) {
        super(profileType);
    }

    public ProfileTypeUID(String scope, String id) {
        super(scope, id);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 2;
    }

    public String getScope() {
        return getSegment(0);
    }

    public String getId() {
        return getSegment(1);
    }
}
