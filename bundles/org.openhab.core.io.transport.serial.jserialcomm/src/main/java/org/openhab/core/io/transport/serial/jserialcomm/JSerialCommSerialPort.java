/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.io.transport.serial.jserialcomm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortEventListener;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific OH serial transport SerialPort implementation using com.fazecast.jSerialComm.SerialPort.
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
public class JSerialCommSerialPort implements SerialPort {

    private final Logger logger = LoggerFactory.getLogger(JSerialCommSerialPort.class);

    private final com.fazecast.jSerialComm.SerialPort sp;

    private boolean notifyOnDataAvailable = false;
    private boolean notifyOnBreakInterrupt = false;
    private boolean notifyOnFramingError = false;
    private boolean notifyOnOverrunError = false;
    private boolean notifyOnParityError = false;
    private boolean notifyOnOutputEmpty = false;
    private boolean notifyOnCTS = false;
    private boolean notifyOnDSR = false;
    private boolean notifyOnRingIndicator = false;
    private boolean notifyOnCarrierDetect = false;

    private @Nullable SerialPortEventListener eventListener;

    /**
     * Constructor.
     *
     * @param sp the underlying serial port implementation
     */
    public JSerialCommSerialPort(final com.fazecast.jSerialComm.SerialPort sp) {
        this.sp = sp;
    }

    @Override
    public void close() {
        sp.closePort();
    }

    @Override
    public void setSerialPortParams(int baudrate, int dataBits, int stopBits, int parity)
            throws UnsupportedCommOperationException {
        if (!sp.setComPortParameters(baudrate, dataBits, stopBits, parity)) {
            throw new UnsupportedCommOperationException();
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

    private int combineListeningEvents() {
        // FIXME just use compact version below
        int combined = 0;
        if (notifyOnDataAvailable) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnDataAvailable({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_AVAILABLE, combined);
        }
        if (notifyOnBreakInterrupt) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_BREAK_INTERRUPT;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnBreakInterrupt({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_BREAK_INTERRUPT, combined);
        }
        if (notifyOnFramingError) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_FRAMING_ERROR;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnFramingError({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_FRAMING_ERROR, combined);
        }
        if (notifyOnOverrunError) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnOverrunError({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR, combined);
        }
        if (notifyOnParityError) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_PARITY_ERROR;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnParityError({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_PARITY_ERROR, combined);
        }
        if (notifyOnOutputEmpty) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_WRITTEN;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnOutputEmpty({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_WRITTEN, combined);
        }
        if (notifyOnCTS) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_CTS;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnCTS({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_CTS, combined);
        }
        if (notifyOnDSR) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DSR;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnDSR({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DSR, combined);
        }
        if (notifyOnRingIndicator) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_RING_INDICATOR;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnRingIndicator({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_RING_INDICATOR, combined);
        }
        if (notifyOnCarrierDetect) {
            combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_CARRIER_DETECT;
            logger.debug("--------TRANSPORT-jSerialComm--- subscribed notifyOnCarrierDetect({}) - combined now is: {}",
                    com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_CARRIER_DETECT, combined);
        }

        combined = combined | com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
        logger.debug(
                "--------TRANSPORT-jSerialComm--- subscribed LISTENING_EVENT_PORT_DISCONNECTED({}) - combined now is: {}",
                com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_PORT_DISCONNECTED, combined);

        logger.debug("--------TRANSPORT-jSerialComm--- FINAL combined is: {}", combined);

        return combined;

        /*
         * return com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_PORT_DISCONNECTED
         * | (notifyOnDataAvailable ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_AVAILABLE : 0)
         * | (notifyOnBreakInterrupt ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_BREAK_INTERRUPT : 0)
         * | (notifyOnFramingError ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_FRAMING_ERROR : 0)
         * | (notifyOnOverrunError ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR
         * : 0)
         * | (notifyOnParityError ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_PARITY_ERROR : 0)
         * | (notifyOnOutputEmpty ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_WRITTEN : 0)
         * | (notifyOnCTS ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_CTS : 0)
         * | (notifyOnDSR ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DSR : 0)
         * | (notifyOnRingIndicator ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_RING_INDICATOR : 0)
         * | (notifyOnCarrierDetect ? com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_CARRIER_DETECT : 0);
         */
    }

    @Override
    public void addEventListener(SerialPortEventListener listener) throws TooManyListenersException {
        boolean success = sp.addDataListener(new com.fazecast.jSerialComm.SerialPortDataListener() {
            @Override
            public void serialEvent(final com.fazecast.jSerialComm.@Nullable SerialPortEvent event) {
                if (event == null) {
                    return;
                }
                listener.serialEvent(new JSerialCommSerialPortEvent(event));
            }

            @Override
            public int getListeningEvents() {
                int subscribedEvents = combineListeningEvents();
                logger.debug("--------TRANSPORT-jSerialComm--- {} is subscribed to events: {}", this.toString(),
                        subscribedEvents);
                return subscribedEvents;
            }
        });
        if (!success) {
            logger.error("--------TRANSPORT-jSerialComm--- Could not add SerialPortDataListener to SerialPort {}",
                    sp.getSystemPortName());
            throw new TooManyListenersException(
                    ("Could not add SerialPortDataListener to SerialPort " + sp.getSystemPortName()));
        }
        eventListener = listener;
    }

    // private method to refresh listener (if exists) in order to refresh subscriptions to lib
    private synchronized void refreshListener() {
        SerialPortEventListener eL = eventListener;
        if (eL != null) {
            sp.removeDataListener();
            try {
                this.addEventListener(eL);
                logger.debug("--------TRANSPORT-jSerialComm--- LISTENER REFRESHED!");

            } catch (TooManyListenersException e) {
                logger.error("--------TRANSPORT-jSerialComm--- Could not add SerialPortDataListener to SerialPort {}",
                        sp.getSystemPortName());
            }
        }
    }

    @Override
    public void removeEventListener() {
        sp.removeDataListener();
    }

    @Override
    public void notifyOnDataAvailable(boolean enable) {
        this.notifyOnDataAvailable = enable;
        refreshListener();
    }

    @Override
    public void notifyOnBreakInterrupt(boolean enable) {
        this.notifyOnBreakInterrupt = enable;
        refreshListener();
    }

    @Override
    public void notifyOnFramingError(boolean enable) {
        this.notifyOnFramingError = enable;
        refreshListener();
    }

    @Override
    public void notifyOnOverrunError(boolean enable) {
        this.notifyOnOverrunError = enable;
        refreshListener();
    }

    @Override
    public void notifyOnParityError(boolean enable) {
        this.notifyOnParityError = enable;
        refreshListener();
    }

    @Override
    public void setRTS(boolean enable) {
        if (enable) {
            sp.setRTS();
        } else {
            sp.clearRTS();
        }
    }

    @Override
    public void enableReceiveTimeout(int timeout) throws UnsupportedCommOperationException {
        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("timeout must be non negative (is: %d)", timeout));
        }
        // FIXME !!!!! placeholder implementation
        boolean success = sp.setComPortTimeouts(com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING, timeout, 0);
        if (!success) {
            throw new UnsupportedCommOperationException();
        }
    }

    @Override
    public void disableReceiveTimeout() {
        // FIXME !!!!! placeholder implementation
        sp.setComPortTimeouts(com.fazecast.jSerialComm.SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
    }

    @Override
    public String getName() {
        String sysPortName = sp.getSystemPortName();
        if (sysPortName != null && sysPortName.startsWith("COM")) {
            return sysPortName;
        } else {
            return sp.getSystemPortPath();
        }
    }

    @Override
    public void setFlowControlMode(int flowcontrolRtsctsOut) throws UnsupportedCommOperationException {
        // FIXME check mapping
        if (!sp.setFlowControl(flowcontrolRtsctsOut)) {
            throw new UnsupportedCommOperationException();
        }
    }

    @Override
    public void enableReceiveThreshold(int i) throws UnsupportedCommOperationException {
        // FIXME !!!!! placeholder implementation
        /*
         * try {
         * sp.enableReceiveThreshold(i);
         * } catch (gnu.io.UnsupportedCommOperationException e) {
         * throw new UnsupportedCommOperationException(e);
         * }
         */
    }

    @Override
    public int getBaudRate() {
        return sp.getBaudRate();
    }

    @Override
    public int getDataBits() {
        return sp.getNumDataBits();
    }

    @Override
    public int getStopBits() {
        return sp.getNumStopBits();
    }

    @Override
    public int getParity() {
        return sp.getParity();
    }

    @Override
    public void notifyOnOutputEmpty(boolean enable) {
        this.notifyOnOutputEmpty = enable;
        refreshListener();
    }

    @Override
    public void notifyOnCTS(boolean enable) {
        this.notifyOnCTS = enable;
        refreshListener();
    }

    @Override
    public void notifyOnDSR(boolean enable) {
        this.notifyOnDSR = enable;
        refreshListener();
    }

    @Override
    public void notifyOnRingIndicator(boolean enable) {
        this.notifyOnRingIndicator = enable;
    }

    @Override
    public void notifyOnCarrierDetect(boolean enable) {
        this.notifyOnCarrierDetect = enable;
        refreshListener();
    }

    @Override
    public int getFlowControlMode() {
        // FIXME check mapping
        return sp.getFlowControlSettings();
    }

    @Override
    public boolean isRTS() {
        return sp.getRTS();
    }

    @Override
    public void setDTR(boolean state) {
        if (state) {
            sp.setDTR();
        } else {
            sp.clearDTR();
        }
    }

    @Override
    public boolean isDTR() {
        return sp.getDTR();
    }

    @Override
    public boolean isCTS() {
        return sp.getCTS();
    }

    @Override
    public boolean isDSR() {
        return sp.getDSR();
    }

    @Override
    public boolean isCD() {
        return sp.getDCD();
    }

    @Override
    public boolean isRI() {
        return sp.getRI();
    }

    @Override
    public void sendBreak(int duration) {
        // FIXME !!!!! placeholder implementation. Remove from OH serial transport since it's not used by any binding??
        sp.setBreak();
        // sp.sendBreak(duration);
    }
}
