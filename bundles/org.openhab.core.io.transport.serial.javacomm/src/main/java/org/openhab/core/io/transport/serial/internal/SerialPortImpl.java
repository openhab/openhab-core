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
package org.openhab.core.io.transport.serial.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import javax.comm.SerialPortEvent;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortEventListener;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;

/**
 * Specific serial port implementation.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - added further methods
 */
@NonNullByDefault
public class SerialPortImpl implements SerialPort {

    private final javax.comm.SerialPort sp;

    /**
     * Constructor.
     *
     * @param sp the underlying serial port implementation
     */
    public SerialPortImpl(final javax.comm.SerialPort sp) {
        this.sp = sp;
    }

    @Override
    public void close() {
        sp.close();
    }

    @Override
    public void setSerialPortParams(int baudrate, int dataBits, int stopBits, int parity)
            throws UnsupportedCommOperationException {
        try {
            sp.setSerialPortParams(baudrate, dataBits, stopBits, parity);
        } catch (javax.comm.UnsupportedCommOperationException ex) {
            throw new UnsupportedCommOperationException(ex);
        }
    }

    @Override
    public @Nullable InputStream getInputStream() throws IOException {
        return sp.getInputStream();
    }

    @Override
    public @Nullable OutputStream getOutputStream() throws IOException {
        return sp.getOutputStream();
    }

    @Override
    public void addEventListener(SerialPortEventListener listener) throws TooManyListenersException {
        sp.addEventListener(new javax.comm.SerialPortEventListener() {
            @Override
            public void serialEvent(final @Nullable SerialPortEvent event) {
                if (event == null) {
                    return;
                }
                listener.serialEvent(new SerialPortEventImpl(event));
            }
        });
    }

    @Override
    public void removeEventListener() {
        sp.removeEventListener();
    }

    @Override
    public void notifyOnDataAvailable(boolean enable) {
        sp.notifyOnDataAvailable(enable);
    }

    @Override
    public void notifyOnBreakInterrupt(boolean enable) {
        sp.notifyOnBreakInterrupt(enable);
    }

    @Override
    public void notifyOnFramingError(boolean enable) {
        sp.notifyOnFramingError(enable);
    }

    @Override
    public void notifyOnOverrunError(boolean enable) {
        sp.notifyOnOverrunError(enable);
    }

    @Override
    public void notifyOnParityError(boolean enable) {
        sp.notifyOnParityError(enable);
    }

    @Override
    public void setRTS(boolean enable) {
        sp.setRTS(enable);
    }

    @Override
    public void enableReceiveTimeout(int timeout) throws UnsupportedCommOperationException {
        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("timeout must be non negative (is: %d)", timeout));
        }
        try {
            sp.enableReceiveTimeout(timeout);
        } catch (javax.comm.UnsupportedCommOperationException ex) {
            throw new UnsupportedCommOperationException(ex);
        }
    }

    @Override
    public void disableReceiveTimeout() {
        sp.disableReceiveTimeout();
    }

    @Override
    public String getName() {
        return sp.getName();
    }

    @Override
    public void setFlowControlMode(int flowcontrolRtsctsOut) throws UnsupportedCommOperationException {
        try {
            sp.setFlowControlMode(flowcontrolRtsctsOut);
        } catch (javax.comm.UnsupportedCommOperationException e) {
            throw new UnsupportedCommOperationException(e);
        }
    }

    @Override
    public void enableReceiveThreshold(int i) throws UnsupportedCommOperationException {
        try {
            sp.enableReceiveThreshold(i);
        } catch (javax.comm.UnsupportedCommOperationException e) {
            throw new UnsupportedCommOperationException(e);
        }
    }

}
