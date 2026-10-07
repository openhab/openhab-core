/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Actions applicable to {@link ResourceType#ITEM} resources.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public enum ItemAction implements Action {
    READ,
    COMMAND,
    EDIT;

    @Override
    public ResourceType resourceType() {
        return ResourceType.ITEM;
    }
}
