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
package org.openhab.core.gson;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Example Test for testing Gson deserialization with correct annotation ordering.
 * Unlike {@link GsonTestClassAnnotated_DTO} and {@link GsonTestClassAnnotated2_DTO},
 * this class uses the correct annotation order where {@link SerializedName} precedes
 * any nullability annotations, which works correctly with ECJ 3.45.0 and later.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class GsonTestClassAnnotated3_DTO {
    public @SerializedName("name") @Nullable String n;
    public @SerializedName("email") @Nullable String e;
    public int age;
    public boolean active;

    public GsonTestClassAnnotated3_DTO() {
    }

    public GsonTestClassAnnotated3_DTO(String name, int age, String email, boolean active) {
        this.n = name;
        this.e = email;
        this.age = age;
        this.active = active;
    }

    @Override
    public String toString() {
        return "GsonTestClassAnnotated3_DTO{" + "name='" + n + '\'' + ", age=" + age + ", email='" + e + '\''
                + ", active=" + active + '}';
    }
}
