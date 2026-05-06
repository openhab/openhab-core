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

/**
 * Example Test for testing Gson deserialization.
 *
 * @author Holger Friedrich - Initial contribution
 */
public class GsonTestClassDTO {
    public String name;
    public int age;
    public String email;
    public boolean active;

    public GsonTestClassDTO() {
    }

    public GsonTestClassDTO(String name, int age, String email, boolean active) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.active = active;
    }

    @Override
    public String toString() {
        return "GsonTestClass{" + "name='" + name + '\'' + ", age=" + age + ", email='" + email + '\'' + ", active="
                + active + '}';
    }
}
