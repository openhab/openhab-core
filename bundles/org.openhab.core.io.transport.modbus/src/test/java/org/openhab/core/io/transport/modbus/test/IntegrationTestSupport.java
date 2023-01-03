/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.io.transport.modbus.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.function.LongSupplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.openhab.core.io.transport.modbus.internal.ModbusManagerImpl;
import org.openhab.core.test.java.JavaTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.SerialPort;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.io.ModbusTransport;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.net.ModbusSerialListener;
import net.wimpi.modbus.net.ModbusTCPListener;
import net.wimpi.modbus.net.ModbusUDPListener;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.net.SerialConnectionFactory;
import net.wimpi.modbus.net.TCPSlaveConnection;
import net.wimpi.modbus.net.TCPSlaveConnection.ModbusTCPTransportFactory;
import net.wimpi.modbus.net.TCPSlaveConnectionFactory;
import net.wimpi.modbus.net.UDPSlaveTerminal;
import net.wimpi.modbus.net.UDPSlaveTerminal.ModbusUDPTransportFactoryImpl;
import net.wimpi.modbus.net.UDPSlaveTerminalFactory;
import net.wimpi.modbus.net.UDPTerminal;
import net.wimpi.modbus.procimg.SimpleProcessImage;
import net.wimpi.modbus.util.AtomicCounter;
import net.wimpi.modbus.util.SerialParameters;

/**
 * @author Sami Salonen - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class IntegrationTestSupport extends JavaTest {

    private final Logger logger = LoggerFactory.getLogger(IntegrationTestSupport.class);

    public enum ServerType {
        TCP,
        UDP,
        SERIAL
    }

    /**
     * Servers to test
     * Serial is system dependent
     */
    public static final ServerType[] TEST_SERVERS = new ServerType[] { ServerType.TCP
            // ServerType.UDP,
            // ServerType.SERIAL
    };

    // One can perhaps test SERIAL with https://github.com/freemed/tty0tty
    // and using those virtual ports? Not the same thing as real serial device of course
    private static final String SERIAL_SERVER_PORT = "/dev/pts/7";
    private static final String SERIAL_CLIENT_PORT = "/dev/pts/8";

    private static final SerialParameters SERIAL_PARAMETERS_CLIENT = new SerialParameters(SERIAL_CLIENT_PORT, 115200,
            SerialPort.FLOWCONTROL_NONE, SerialPort.FLOWCONTROL_NONE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE, Modbus.SERIAL_ENCODING_ASCII, false, 1000);

    private static final SerialParameters SERIAL_PARAMETERS_SERVER = new SerialParameters(SERIAL_SERVER_PORT,
            SERIAL_PARAMETERS_CLIENT.getBaudRate(), SERIAL_PARAMETERS_CLIENT.getFlowControlIn(),
            SERIAL_PARAMETERS_CLIENT.getFlowControlOut(), SERIAL_PARAMETERS_CLIENT.getDatabits(),
            SERIAL_PARAMETERS_CLIENT.getStopbits(), SERIAL_PARAMETERS_CLIENT.getParity(),
            SERIAL_PARAMETERS_CLIENT.getEncoding(), SERIAL_PARAMETERS_CLIENT.isEcho(), 1000);

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        System.setProperty("gnu.io.rxtx.SerialPorts", SERIAL_SERVER_PORT + File.pathSeparator + SERIAL_CLIENT_PORT);
    }

    /**
     * Max time to wait for connections/requests from client
     */
    protected static final int MAX_WAIT_REQUESTS_MILLIS = 1000;

    /**
     * The server runs in single thread, only one connection is accepted at a time.
     * This makes the tests as strict as possible -- connection must be closed.
     */
    private static final int SERVER_THREADS = 1;
    protected static final int SLAVE_UNIT_ID = 1;

    private static AtomicCounter udpServerIndex = new AtomicCounter(0);

    protected @Spy TCPSlaveConnectionFactory tcpConnectionFactory = new TCPSlaveConnectionFactoryImpl();
    protected @Spy UDPSlaveTerminalFactory udpTerminalFactory = new UDPSlaveTerminalFactoryImpl();
    protected @Spy SerialConnectionFactory serialConnectionFactory = new SerialConnectionFactoryImpl();

    protected @NonNullByDefault({}) ResultCaptor<ModbusRequest> modbustRequestCaptor;

    protected @NonNullByDefault({}) ModbusTCPListener tcpListener;
    protected @NonNullByDefault({}) ModbusUDPListener udpListener;
    protected @NonNullByDefault({}) ModbusSerialListener serialListener;
    protected @NonNullByDefault({}) SimpleProcessImage spi;
    protected int tcpModbusPort = -1;
    protected int udpModbusPort = -1;
    protected ServerType serverType = ServerType.TCP;
    protected long artificialServerWait = 0;

    protected @NonNullByDefault({}) NonOSGIModbusManager modbusManager;

    private Thread serialServerThread = new Thread("ModbusTransportTestsSerialServer") {
        @Override
        public void run() {
            serialListener = new ModbusSerialListener(SERIAL_PARAMETERS_SERVER);
        }
    };

    protected static InetAddress localAddress() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        modbustRequestCaptor = new ResultCaptor<>(new LongSupplier() {

            @Override
            public long getAsLong() {
                return artificialServerWait;
            }
        });
        modbusManager = new NonOSGIModbusManager();
        startServer();
    }

    @AfterEach
    public void tearDown() {
        stopServer();
        modbusManager.close();
    }

    protected void waitForRequests(int expectedRequestCount) {
        waitForAssert(
                () -> assertThat(modbustRequestCaptor.getAllReturnValues().size(), is(equalTo(expectedRequestCount))),
                MAX_WAIT_REQUESTS_MILLIS, 10);
    }

    protected void waitForConnectionsReceived(int expectedConnections) {
        waitForAssert(() -> {
            if (ServerType.TCP.equals(serverType)) {
                verify(tcpConnectionFactory, times(expectedConnections)).create(any(Socket.class));
            } else if (ServerType.UDP.equals(serverType)) {
                logger.debug("No-op, UDP server type");
            } else if (ServerType.SERIAL.equals(serverType)) {
                logger.debug("No-op, SERIAL server type");
            } else {
                throw new UnsupportedOperationException();
            }
        }, MAX_WAIT_REQUESTS_MILLIS, 10);
    }

    private void startServer() {
        spi = new SimpleProcessImage();
        ModbusCoupler.getReference().setProcessImage(spi);
        ModbusCoupler.getReference().setMaster(false);
        ModbusCoupler.getReference().setUnitID(SLAVE_UNIT_ID);

        if (ServerType.TCP.equals(serverType)) {
            startTCPServer();
        } else if (ServerType.UDP.equals(serverType)) {
            startUDPServer();
        } else if (ServerType.SERIAL.equals(serverType)) {
            startSerialServer();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void stopServer() {
        if (ServerType.TCP.equals(serverType)) {
            tcpListener.stop();
            logger.debug("Stopped TCP listener, tcpModbusPort={}", tcpModbusPort);
        } else if (ServerType.UDP.equals(serverType)) {
            udpListener.stop();
            logger.debug("Stopped UDP listener, udpModbusPort={}", udpModbusPort);
        } else if (ServerType.SERIAL.equals(serverType)) {
            try {
                serialServerThread.join(100);
            } catch (InterruptedException e) {
                logger.debug("Serial server thread .join() interrupted! Will interrupt it now.");
            }
            serialServerThread.interrupt();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void startUDPServer() {
        udpListener = new ModbusUDPListener(localAddress(), udpTerminalFactory);
        for (int portCandidate = 10000 + udpServerIndex.increment(); portCandidate < 20000; portCandidate++) {
            try {
                DatagramSocket socket = new DatagramSocket(portCandidate);
                socket.close();
                udpListener.setPort(portCandidate);
                break;
            } catch (SocketException e) {
                continue;
            }
        }

        udpListener.start();
        waitForUDPServerStartup();
        assertNotSame(-1, udpModbusPort);
        assertNotSame(0, udpModbusPort);
    }

    private void waitForUDPServerStartup() {
        // Query server port. It seems to take time (probably due to thread starting)
        waitFor(() -> udpListener.getLocalPort() > 0, 5, 10_000);
        udpModbusPort = udpListener.getLocalPort();
    }

    private void startTCPServer() {
        // Serve single user at a time
        tcpListener = new ModbusTCPListener(SERVER_THREADS, localAddress(), tcpConnectionFactory);
        // Use any open port
        tcpListener.setPort(0);
        tcpListener.start();
        // Query server port. It seems to take time (probably due to thread starting)
        waitForTCPServerStartup();
        assertNotSame(-1, tcpModbusPort);
        assertNotSame(0, tcpModbusPort);
    }

    private void waitForTCPServerStartup() {
        waitFor(() -> tcpListener.getLocalPort() > 0, 10_000, 5);
        tcpModbusPort = tcpListener.getLocalPort();
    }

    private void startSerialServer() {
        serialServerThread.start();
        assertDoesNotThrow(() -> Thread.sleep(1000));
    }

    public ModbusSlaveEndpoint getEndpoint() {
        assert tcpModbusPort > 0;
        return new ModbusTCPSlaveEndpoint("127.0.0.1", tcpModbusPort, false);
    }

    /**
     * Transport factory that spies the created transport items
     */
    public class SpyingModbusTCPTransportFactory extends ModbusTCPTransportFactory {

        @Override
        public ModbusTransport create(@NonNullByDefault({}) Socket socket) {
            ModbusTransport transport = spy(super.create(socket));
            // Capture requests produced by our server transport
            assertDoesNotThrow(() -> doAnswer(modbustRequestCaptor).when(transport).readRequest());
            return transport;
        }
    }

    public class SpyingModbusUDPTransportFactory extends ModbusUDPTransportFactoryImpl {

        @Override
        public ModbusTransport create(@NonNullByDefault({}) UDPTerminal terminal) {
            ModbusTransport transport = spy(super.create(terminal));
            // Capture requests produced by our server transport
            assertDoesNotThrow(() -> doAnswer(modbustRequestCaptor).when(transport).readRequest());
            return transport;
        }
    }

    public class TCPSlaveConnectionFactoryImpl implements TCPSlaveConnectionFactory {

        @Override
        public TCPSlaveConnection create(@NonNullByDefault({}) Socket socket) {
            return new TCPSlaveConnection(socket, new SpyingModbusTCPTransportFactory());
        }
    }

    public class UDPSlaveTerminalFactoryImpl implements UDPSlaveTerminalFactory {

        @Override
        public UDPSlaveTerminal create(@NonNullByDefault({}) InetAddress interfac, int port) {
            UDPSlaveTerminal terminal = new UDPSlaveTerminal(interfac, new SpyingModbusUDPTransportFactory(), 1);
            terminal.setLocalPort(port);
            return terminal;
        }
    }

    public class SerialConnectionFactoryImpl implements SerialConnectionFactory {
        @Override
        public SerialConnection create(@NonNullByDefault({}) SerialParameters parameters) {
            SerialConnection serialConnection = new SerialConnection(parameters) {
                @Override
                public ModbusTransport getModbusTransport() {
                    ModbusTransport transport = spy(super.getModbusTransport());
                    assertDoesNotThrow(() -> doAnswer(modbustRequestCaptor).when(transport).readRequest());
                    return transport;
                }
            };
            return serialConnection;
        }
    }

    public static class NonOSGIModbusManager extends ModbusManagerImpl implements AutoCloseable {
        public NonOSGIModbusManager() {
            activate(new HashMap<>());
        }

        @Override
        public void close() {
            deactivate();
        }
    }
}
