/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
 * Example Test for testing Gson deserialization.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class GsonTestClassAnnotated2_DTO {
    public @SerializedName("name") @Nullable String n;
    public @Nullable @SerializedName("email") String e;
    public int age;
    public boolean active;

    public GsonTestClassAnnotated2_DTO() {
    }

    public GsonTestClassAnnotated2_DTO(String name, int age, String email, boolean active) {
        this.n = name;
        this.e = email;
        this.age = age;
        this.active = active;
    }

    @Override
    public String toString() {
        return "GsonTestClassAnnotated2_DTO{" + "name='" + n + '\'' + ", age=" + age + ", email='" + e + '\''
                + ", active=" + active + '}';
    }
}
