/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.json.gson;

/**
 * Bean used for tests of the GsonBindingService.
 *
 * @author Flavio Costa - Initial implementation
 */
public class GsonBindingServiceTestBean {

    /**
     * Enum used to test custom serialization.
     */
    public static enum Active {
        YES,
        NO
    };

    /**
     * Field for test of custom serialization of enums.
     */
    public Active status;

    /**
     * Field for test of serialization of private fields.
     */
    private String text;

    /**
     * Returns the text in this bean.
     *
     * @return Value in the private field.
     */
    public String getText() {
        return text;
    }
}
