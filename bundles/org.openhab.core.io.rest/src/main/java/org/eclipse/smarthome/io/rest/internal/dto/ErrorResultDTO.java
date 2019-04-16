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
package org.eclipse.smarthome.io.rest.internal.dto;

import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an error message that is send on exceptions etc
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class ErrorResultDTO {
    public static class exceptionDTO {
        public final @JsonProperty("class") String clazz;
        public final String message;
        public final @JsonProperty("localized-message") String localizedMessage;
        public final @Nullable Object cause;

        exceptionDTO() {
            clazz = "";
            message = "";
            localizedMessage = "";
            cause = null;
        }

        public exceptionDTO(Exception ex) {
            clazz = ex.getClass().getName();
            cause = null != ex.getCause() ? ex.getCause().getClass().getName() : null;
            message = ex.getMessage();
            localizedMessage = ex.getLocalizedMessage();
        }
    }

    public static class errorDTO {
        public final String message;
        public final @Nullable @JsonProperty("http-code") Integer httpCode;
        public final ErrorResultDTO.@Nullable exceptionDTO exception;

        errorDTO() {
            message = "";
            httpCode = null;
            exception = null;
        }

        public errorDTO(String message, @Nullable Integer httpCode, ErrorResultDTO.@Nullable exceptionDTO exception) {
            this.message = message;
            this.httpCode = httpCode;
            this.exception = exception;
        }
    }

    public ErrorResultDTO.@Nullable errorDTO error;
    public @Nullable Object entity;

    public ErrorResultDTO() {
        error = null;
        entity = null;
    }

    public ErrorResultDTO(String message, Response.StatusType status, @Nullable Object entity, @Nullable Exception ex) {
        this.entity = entity;
        if (ex != null) {
            error = new errorDTO(message, status.getStatusCode(), new exceptionDTO(ex));
        } else {
            error = new errorDTO(message, status.getStatusCode(), null);
        }
    }
}