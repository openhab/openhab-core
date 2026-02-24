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
package org.openhab.core.persistence.dto;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.PersistenceItemInfo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@link PersistenceItemInfoDTO} is used for transferring {@link org.openhab.core.persistence.PersistenceItemInfo}
 * data.
 *
 * @author Florian Hotze - Initial contribution
 */
@Schema(name = "PersistenceItemInfo")
@NonNullByDefault
public class PersistenceItemInfoDTO {
    public @NonNullByDefault({}) String name;
    public @Nullable Integer count;
    public @Nullable Date earliest;
    public @Nullable Date latest;

    // do not remove - needed by GSON
    public PersistenceItemInfoDTO() {
    }

    public PersistenceItemInfoDTO(PersistenceItemInfo info) {
        this(info, info.getName());
    }

    public PersistenceItemInfoDTO(PersistenceItemInfo info, String alias) {
        this.name = alias;
        this.count = info.getCount();
        this.earliest = info.getEarliest();
        this.latest = info.getLatest();
    }
}
