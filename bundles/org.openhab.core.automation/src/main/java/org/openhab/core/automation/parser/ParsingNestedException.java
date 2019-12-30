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
package org.openhab.core.automation.parser;

/**
 * This class extends the {@link Exception} class functionality with keeping additional information about reasons for
 * exception during the parsing process.
 *
 * @author Ana Dimova - Initial contribution
 */
@SuppressWarnings("serial")
public class ParsingNestedException extends Exception {

    public static final int MODULE_TYPE = 1;
    public static final int TEMPLATE = 2;
    public static final int RULE = 3;

    /**
     * Keeps information about the UID of the automation object for parsing - module type, template or rule.
     */
    private final String id;

    /**
     * Keeps information about the type of the automation object for parsing - module type, template or rule.
     */
    private final int type;

    /**
     * Creates an exception based on exception thrown the parsing plus information about the type of the automation
     * object, its UID and additional message with additional information about the parsing process.
     *
     * @param type is the type of the automation object for parsing.
     * @param id is the UID of the automation object for parsing.
     * @param msg is the additional message with additional information about the parsing process.
     * @param t is the exception thrown during the parsing.
     */
    public ParsingNestedException(int type, String id, String msg, Throwable t) {
        super(msg, t);
        this.id = id;
        this.type = type;
    }

    /**
     * Creates an exception based on exception thrown during the parsing plus information about the type of the
     * automation object and its UID.
     *
     * @param type is the type of the automation object for parsing.
     * @param id is the UID of the automation object for parsing.
     * @param t is the exception thrown during the parsing.
     */
    public ParsingNestedException(int type, String id, Throwable t) {
        super(t);
        this.id = id;
        this.type = type;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case MODULE_TYPE:
                sb.append("[Module Type");
                break;
            case TEMPLATE:
                sb.append("[Template");
                break;
            case RULE:
                sb.append("[Rule");
                break;
            default:
                break;
        }
        if (id != null) {
            sb.append(" " + id);
        }
        sb.append("] " + super.getMessage());
        return sb.toString();
    }
}
