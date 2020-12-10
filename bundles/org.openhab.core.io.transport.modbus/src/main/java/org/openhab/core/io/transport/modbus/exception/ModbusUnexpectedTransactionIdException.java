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
package org.openhab.core.io.transport.modbus.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Exception representing situation where transaction id of the response does not match request
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class ModbusUnexpectedTransactionIdException extends ModbusTransportException {

    private static final long serialVersionUID = -2453232634024813933L;
    private int requestId;
    private int responseId;

    public ModbusUnexpectedTransactionIdException(int requestId, int responseId) {
        this.requestId = requestId;
        this.responseId = responseId;
    }

    @Override
    public @Nullable String getMessage() {
        return String.format("Transaction id of request (%d) does not equal response (%d). Slave response is invalid.",
                requestId, responseId);
    }

    @Override
    public String toString() {
        return String.format(
                "ModbusUnexpectedTransactionIdException(requestTransactionId=%d, responseTransactionId=%d)", requestId,
                responseId);
    }

    public int getRequestId() {
        return requestId;
    }

    public int getResponseId() {
        return responseId;
    }
}
