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
package org.openhab.core.automation.parser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is an {@link Exception} implementation for automation objects that retain some additional information.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Keeps information about the type of the automation object for validation - module type, template or rule.
     */
    private final ObjectType type;

    /**
     * Keeps information about the UID of the automation object for validation - module type, template or rule.
     */
    private final @Nullable String uid;

    /**
     * Creates a new instance with the specified type, UID and message.
     *
     * @param type the {@link ObjectType} to use.
     * @param uid the UID to use, if any.
     * @param message The detail message.
     */
    public ValidationException(ObjectType type, @Nullable String uid, @Nullable String message) {
        super(message);
        this.type = type;
        this.uid = uid;
    }

    /**
     * Creates a new instance with the specified type, UID and cause.
     *
     * @param type the {@link ObjectType} to use.
     * @param uid the UID to use, if any.
     * @param cause the {@link Throwable} that caused this {@link Exception}.
     */
    public ValidationException(ObjectType type, @Nullable String uid, @Nullable Throwable cause) {
        super(cause);
        this.type = type;
        this.uid = uid;
    }

    /**
     * Creates a new instance with the specified type, UID, message and cause.
     *
     * @param type the {@link ObjectType} to use.
     * @param uid the UID to use, if any.
     * @param message The detail message.
     * @param cause the {@link Throwable} that caused this {@link Exception}.
     */
    public ValidationException(ObjectType type, @Nullable String uid, @Nullable String message,
            @Nullable Throwable cause) {
        super(message, cause);
        this.type = type;
        this.uid = uid;
    }

    /**
     * Creates a new instance with the specified type, UID, message and cause.
     *
     * @param type the {@link ObjectType} to use.
     * @param uid the UID to use, if any.
     * @param message The detail message.
     * @param cause the {@link Throwable} that caused this {@link Exception}.
     * @param enableSuppression whether or not suppression is enabled or disabled.
     * @param writableStackTrace whether or not the stack trace should be writable.
     */
    public ValidationException(ObjectType type, @Nullable String uid, @Nullable String message,
            @Nullable Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.type = type;
        this.uid = uid;
    }

    @Override
    public @Nullable String getMessage() {
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
        if (uid != null) {
            sb.append(' ').append(uid);
        }
        sb.append("] ").append(super.getMessage());
        return sb.toString();
    }

    public enum ObjectType {
        MODULE_TYPE,
        TEMPLATE,
        RULE;
    }
}
