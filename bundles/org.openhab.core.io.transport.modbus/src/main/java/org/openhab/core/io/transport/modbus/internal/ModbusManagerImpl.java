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
package org.openhab.core.io.transport.modbus.internal;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.IIOException;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.SwallowedExceptionListener;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.transport.modbus.AsyncModbusFailure;
import org.openhab.core.io.transport.modbus.AsyncModbusWriteResult;
import org.openhab.core.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.core.io.transport.modbus.ModbusFailureCallback;
import org.openhab.core.io.transport.modbus.ModbusManager;
import org.openhab.core.io.transport.modbus.ModbusReadCallback;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusResultCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.core.io.transport.modbus.PollTask;
import org.openhab.core.io.transport.modbus.TaskWithEndpoint;
import org.openhab.core.io.transport.modbus.WriteTask;
import org.openhab.core.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSerialSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpointVisitor;
import org.openhab.core.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.openhab.core.io.transport.modbus.endpoint.ModbusUDPSlaveEndpoint;
import org.openhab.core.io.transport.modbus.exception.ModbusConnectionException;
import org.openhab.core.io.transport.modbus.exception.ModbusUnexpectedResponseFunctionCodeException;
import org.openhab.core.io.transport.modbus.exception.ModbusUnexpectedResponseSizeException;
import org.openhab.core.io.transport.modbus.exception.ModbusUnexpectedTransactionIdException;
import org.openhab.core.io.transport.modbus.internal.pooling.ModbusSlaveConnectionFactoryImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.net.ModbusSlaveConnection;

/**
 * Main implementation of ModbusManager
 *
 * We use connection pool to ensure that only single transaction is ongoing per each endpoint. This is especially
 * important with serial slaves but practice has shown that even many tcp slaves have limited
 * capability to handle many connections at the same time
 *
 * @author Sami Salonen - Initial contribution
 */
@Component(service = ModbusManager.class, configurationPid = "transport.modbus")
@NonNullByDefault
public class ModbusManagerImpl implements ModbusManager {

    static class PollTaskUnregistered extends Exception {
        public PollTaskUnregistered(String msg) {
            super(msg);
        }

        private static final long serialVersionUID = 6939730579178506885L;
    }

    @FunctionalInterface
    private interface ModbusOperation<T> {

        /**
         * Execute the operation.
         *
         * All errors should be raised. There should not be any retry mechanism implemented at this level
         *
         * @param timer aggregate stop watch for performance profiling
         * @param task task to execute
         * @param connection connection to use
         *
         * @throws IIOException on generic IO errors
         * @throws ModbusException on Modbus protocol errors (e.g. ModbusIOException on I/O, ModbusSlaveException on
         *             slave exception responses)
         * @throws ModbusUnexpectedTransactionIdException when transaction IDs of the request and
         *             response do not match
         * @throws ModbusUnexpectedResponseFunctionCodeException when response function code does not match the request
         *             (ill-behaving slave)
         * @throws ModbusUnexpectedResponseSizeException when data length of the response and request do not match
         */
        public void accept(AggregateStopWatch timer, T task, ModbusSlaveConnection connection)
                throws ModbusException, IIOException, ModbusUnexpectedTransactionIdException,
                ModbusUnexpectedResponseFunctionCodeException, ModbusUnexpectedResponseSizeException;
    }

    /**
     * Check that transaction id of the response and request match
     *
     * @param response response from the slave corresponding to request
     * @param libRequest modbus request
     * @param operationId operation id for logging
     * @throws ModbusUnexpectedTransactionIdException when transaction IDs of the request and
     *             response do not match
     */
    private <R> void checkTransactionId(ModbusResponse response, ModbusRequest libRequest, String operationId)
            throws ModbusUnexpectedTransactionIdException {
        // Compare request and response transaction ID. NOTE: ModbusTransaction.getTransactionID() is static and
        // not safe to use
        if ((response.getTransactionID() != libRequest.getTransactionID()) && !response.isHeadless()) {
            throw new ModbusUnexpectedTransactionIdException(libRequest.getTransactionID(),
                    response.getTransactionID());
        }
    }

    /**
     * Check that function code of the response and request match
     *
     * @param response response from the slave corresponding to request
     * @param libRequest modbus request
     * @param operationId operation id for logging
     * @throws ModbusUnexpectedResponseFunctionCodeException when response function code does not match the request
     *             (ill-behaving slave)
     */
    private <R> void checkFunctionCode(ModbusResponse response, ModbusRequest libRequest, String operationId)
            throws ModbusUnexpectedResponseFunctionCodeException {
        if ((response.getFunctionCode() != libRequest.getFunctionCode())) {
            throw new ModbusUnexpectedResponseFunctionCodeException(libRequest.getTransactionID(),
                    response.getTransactionID());
        }
    }

    /**
     * Check that number of bits/registers/discrete inputs is not less than what was requested.
     *
     * According to modbus protocol, we should get always get always equal amount of registers data back as response.
     * With coils and discrete inputs, we can get more since responses are in 8 bit chunks.
     *
     * However, in no case we expect less items in response.
     *
     * This is to identify clearly invalid responses which might cause problems downstream when using the data.
     *
     * @param response response response from the slave corresponding to request
     * @param request modbus request
     * @param operationId operation id for logging
     * @throws ModbusUnexpectedResponseSizeException when data length of the response and request do not match
     */
    private <R> void checkResponseSize(ModbusResponse response, ModbusReadRequestBlueprint request, String operationId)
            throws ModbusUnexpectedResponseSizeException {
        final int responseCount = ModbusLibraryWrapper.getNumberOfItemsInResponse(response, request);
        if (responseCount < request.getDataLength()) {
            throw new ModbusUnexpectedResponseSizeException(request.getDataLength(), responseCount);
        }
    }

    /**
     * Implementation for the PollTask operation
     *
     * @author Sami Salonen - Initial contribution
     *
     */
    private class PollOperation implements ModbusOperation<PollTask> {
        @Override
        public void accept(AggregateStopWatch timer, PollTask task, ModbusSlaveConnection connection)
                throws ModbusException, ModbusUnexpectedTransactionIdException,
                ModbusUnexpectedResponseFunctionCodeException, ModbusUnexpectedResponseSizeException {
            ModbusSlaveEndpoint endpoint = task.getEndpoint();
            ModbusReadRequestBlueprint request = task.getRequest();
            ModbusReadCallback callback = task.getResultCallback();
            String operationId = timer.operationId;

            ModbusTransaction transaction = ModbusLibraryWrapper.createTransactionForEndpoint(endpoint, connection);
            ModbusRequest libRequest = ModbusLibraryWrapper.createRequest(request);
            transaction.setRequest(libRequest);

            logger.trace("Going execute transaction with request request (FC={}): {} [operation ID {}]",
                    request.getFunctionCode(), libRequest.getHexMessage(), operationId);
            // Might throw ModbusIOException (I/O error) or ModbusSlaveException (explicit exception response from
            // slave)
            timer.transaction.timeRunnableWithModbusException(() -> transaction.execute());
            ModbusResponse response = transaction.getResponse();
            logger.trace("Response for read request (FC={}, transaction ID={}): {} [operation ID {}]",
                    response.getFunctionCode(), response.getTransactionID(), response.getHexMessage(), operationId);
            checkTransactionId(response, libRequest, operationId);
            checkFunctionCode(response, libRequest, operationId);
            checkResponseSize(response, request, operationId);
            timer.callback
                    .timeRunnable(() -> ModbusLibraryWrapper.invokeCallbackWithResponse(request, callback, response));
        }
    }

    /**
     * Implementation for WriteTask operation
     *
     * @author Sami Salonen - Initial contribution
     *
     */
    private class WriteOperation implements ModbusOperation<WriteTask> {
        @Override
        public void accept(AggregateStopWatch timer, WriteTask task, ModbusSlaveConnection connection)
                throws ModbusException, ModbusUnexpectedTransactionIdException,
                ModbusUnexpectedResponseFunctionCodeException {
            ModbusSlaveEndpoint endpoint = task.getEndpoint();
            ModbusWriteRequestBlueprint request = task.getRequest();
            @Nullable
            ModbusWriteCallback callback = task.getResultCallback();
            String operationId = timer.operationId;

            ModbusTransaction transaction = ModbusLibraryWrapper.createTransactionForEndpoint(endpoint, connection);
            ModbusRequest libRequest = ModbusLibraryWrapper.createRequest(request);
            transaction.setRequest(libRequest);

            logger.trace("Going execute transaction with read request (FC={}): {} [operation ID {}]",
                    request.getFunctionCode(), libRequest.getHexMessage(), operationId);

            // Might throw ModbusIOException (I/O error) or ModbusSlaveException (explicit exception response from
            // slave)
            timer.transaction.timeRunnableWithModbusException(() -> transaction.execute());
            ModbusResponse response = transaction.getResponse();
            logger.trace("Response for write request (FC={}, transaction ID={}): {} [operation ID {}]",
                    response.getFunctionCode(), response.getTransactionID(), response.getHexMessage(), operationId);
            checkTransactionId(response, libRequest, operationId);
            checkFunctionCode(response, libRequest, operationId);
            timer.callback.timeRunnable(
                    () -> invokeCallbackWithResponse(request, callback, new ModbusResponseImpl(response)));
        }
    }

    private final Logger logger = LoggerFactory.getLogger(ModbusManagerImpl.class);
    private final Logger pollMonitorLogger = LoggerFactory
            .getLogger(ModbusManagerImpl.class.getName() + ".PollMonitor");

    /**
     * Time to wait between connection passive+borrow, i.e. time to wait between
     * transactions
     * Default 60ms for TCP slaves, Siemens S7 1212 PLC couldn't handle faster
     * requests with default settings.
     */
    public static final long DEFAULT_TCP_INTER_TRANSACTION_DELAY_MILLIS = 60;

    /**
     * Time to wait between connection passive+borrow, i.e. time to wait between
     * transactions
     * Default 35ms for Serial slaves, motivation discussed
     * here https://community.openhab.org/t/connection-pooling-in-modbus-binding/5246/111?u=ssalonen
     */
    public static final long DEFAULT_SERIAL_INTER_TRANSACTION_DELAY_MILLIS = 35;

    /**
     * Thread naming for modbus read & write requests. Also used by the monitor thread
     */
    private static final String MODBUS_POLLER_THREAD_POOL_NAME = "modbusManagerPollerThreadPool";

    /**
     * Log message with WARN level if the task queues exceed this limit.
     *
     * If the queues grow too large, it might be an issue with consumer of the ModbusManager.
     *
     * You can generate large queue by spamming ModbusManager with one-off read or writes (submitOnTimePoll or
     * submitOneTimeWrite).
     *
     * Note that there is no issue registering many regular polls, those do not "queue" the same way.
     *
     * Presumably slow callbacks can increase queue size with callbackThreadPool
     */
    private static final long WARN_QUEUE_SIZE = 500;
    private static final long MONITOR_QUEUE_INTERVAL_MILLIS = 10000;

    private final PollOperation pollOperation = new PollOperation();
    private final WriteOperation writeOperation = new WriteOperation();

    private volatile long lastQueueMonitorLog = -1;

    /**
     * We use connection pool to ensure that only single transaction is ongoing per each endpoint. This is especially
     * important with serial slaves but practice has shown that even many tcp slaves have limited
     * capability to handle many connections at the same time
     *
     * Relevant discussion at the time of implementation:
     * - https://community.openhab.org/t/modbus-connection-problem/6108/
     * - https://community.openhab.org/t/connection-pooling-in-modbus-binding/5246/
     */

    private volatile @Nullable KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> connectionPool;
    private volatile @Nullable ModbusSlaveConnectionFactoryImpl connectionFactory;
    private volatile Map<PollTask, ScheduledFuture<?>> scheduledPollTasks = new ConcurrentHashMap<>();
    /**
     * Executor for requests
     */
    private volatile @Nullable ScheduledExecutorService scheduledThreadPoolExecutor;
    private volatile @Nullable ScheduledFuture<?> monitorFuture;
    private volatile Set<ModbusCommunicationInterfaceImpl> communicationInterfaces = new ConcurrentHashSet<>();

    private void constructConnectionPool() {
        ModbusSlaveConnectionFactoryImpl connectionFactory = new ModbusSlaveConnectionFactoryImpl();
        connectionFactory.setDefaultPoolConfigurationFactory(endpoint -> {
            return endpoint.accept(new ModbusSlaveEndpointVisitor<EndpointPoolConfiguration>() {

                @Override
                public @NonNull EndpointPoolConfiguration visit(ModbusTCPSlaveEndpoint modbusIPSlavePoolingKey) {
                    EndpointPoolConfiguration endpointPoolConfig = new EndpointPoolConfiguration();
                    endpointPoolConfig.setInterTransactionDelayMillis(DEFAULT_TCP_INTER_TRANSACTION_DELAY_MILLIS);
                    endpointPoolConfig.setConnectMaxTries(Modbus.DEFAULT_RETRIES);
                    return endpointPoolConfig;
                }

                @Override
                public @NonNull EndpointPoolConfiguration visit(ModbusSerialSlaveEndpoint modbusSerialSlavePoolingKey) {
                    EndpointPoolConfiguration endpointPoolConfig = new EndpointPoolConfiguration();
                    // never "disconnect" (close/open serial port) serial connection between borrows
                    endpointPoolConfig.setReconnectAfterMillis(-1);
                    endpointPoolConfig.setInterTransactionDelayMillis(DEFAULT_SERIAL_INTER_TRANSACTION_DELAY_MILLIS);
                    endpointPoolConfig.setConnectMaxTries(Modbus.DEFAULT_RETRIES);
                    return endpointPoolConfig;
                }

                @Override
                public @NonNull EndpointPoolConfiguration visit(ModbusUDPSlaveEndpoint modbusUDPSlavePoolingKey) {
                    EndpointPoolConfiguration endpointPoolConfig = new EndpointPoolConfiguration();
                    endpointPoolConfig.setInterTransactionDelayMillis(DEFAULT_TCP_INTER_TRANSACTION_DELAY_MILLIS);
                    endpointPoolConfig.setConnectMaxTries(Modbus.DEFAULT_RETRIES);
                    return endpointPoolConfig;
                }
            });
        });

        GenericKeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> genericKeyedObjectPool = new ModbusConnectionPool(
                connectionFactory);
        genericKeyedObjectPool.setSwallowedExceptionListener(new SwallowedExceptionListener() {

            @SuppressWarnings("null")
            @Override
            public void onSwallowException(@Nullable Exception e) {
                LoggerFactory.getLogger(ModbusManagerImpl.class).warn(
                        "Connection pool swallowed unexpected exception:{} {}",
                        Optional.ofNullable(e).map(ex -> ex.getClass().getSimpleName()).orElse(""),
                        Optional.ofNullable(e).map(ex -> ex.getMessage()).orElse("<null>"), e);
            }
        });
        connectionPool = genericKeyedObjectPool;
        this.connectionFactory = connectionFactory;
    }

    private Optional<ModbusSlaveConnection> borrowConnection(ModbusSlaveEndpoint endpoint) {
        Optional<ModbusSlaveConnection> connection = Optional.empty();
        KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> pool = connectionPool;
        if (pool == null) {
            return connection;
        }
        long start = System.currentTimeMillis();
        try {
            connection = Optional.ofNullable(pool.borrowObject(endpoint));
        } catch (Exception e) {
            logger.warn("Error getting a new connection for endpoint {}. Error was: {} {}", endpoint,
                    e.getClass().getName(), e.getMessage());
        }
        if (connection.isPresent()) {
            ModbusSlaveConnection slaveConnection = connection.get();
            if (!slaveConnection.isConnected()) {
                logger.trace(
                        "Received connection which is unconnected, preventing use by returning connection to pool.");
                returnConnection(endpoint, connection);
                connection = Optional.empty();
            }
        }
        logger.trace("borrowing connection (got {}) for endpoint {} took {} ms", connection, endpoint,
                System.currentTimeMillis() - start);
        return connection;
    }

    private void invalidate(ModbusSlaveEndpoint endpoint, Optional<ModbusSlaveConnection> connection) {
        KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> pool = connectionPool;
        if (pool == null) {
            return;
        }
        long start = System.currentTimeMillis();
        connection.ifPresent(con -> {
            try {
                pool.invalidateObject(endpoint, con);
            } catch (Exception e) {
                logger.warn("Error invalidating connection in pool for endpoint {}. Error was: {} {}", endpoint,
                        e.getClass().getName(), e.getMessage(), e);
            }
        });
        logger.trace("invalidating connection for endpoint {} took {} ms", endpoint,
                System.currentTimeMillis() - start);
    }

    private void returnConnection(ModbusSlaveEndpoint endpoint, Optional<ModbusSlaveConnection> connection) {
        KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> pool = connectionPool;
        if (pool == null) {
            return;
        }
        long start = System.currentTimeMillis();
        connection.ifPresent(con -> {
            try {
                pool.returnObject(endpoint, con);
                logger.trace("returned connection to pool for endpoint {}", endpoint);
            } catch (Exception e) {
                logger.warn("Error returning connection to pool for endpoint {}. Error was: {} {}", endpoint,
                        e.getClass().getName(), e.getMessage(), e);
            }
        });
        logger.trace("returning connection for endpoint {} took {} ms", endpoint, System.currentTimeMillis() - start);
    }

    /**
     * Establishes connection to the endpoint specified by the task
     *
     * In case connection cannot be established, callback is called with {@link ModbusConnectionException}
     *
     * @param operationId id appened to log messages for identifying the operation
     * @param oneOffTask whether this is one-off, or execution of previously scheduled poll
     * @param task task representing the read or write operation
     * @return {@link ModbusSlaveConnection} to the endpoint as specified by the task, or empty {@link Optional} when
     *         connection cannot be established
     * @throws PollTaskUnregistered
     */
    private <R, C extends ModbusResultCallback, F extends ModbusFailureCallback<R>, T extends TaskWithEndpoint<R, C, F>> Optional<ModbusSlaveConnection> getConnection(
            AggregateStopWatch timer, boolean oneOffTask, @NonNull T task) throws PollTaskUnregistered {
        KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> connectionPool = this.connectionPool;
        if (connectionPool == null) {
            return Optional.empty();
        }
        String operationId = timer.operationId;
        logger.trace(
                "Executing task {} (oneOff={})! Waiting for connection. Idle connections for this endpoint: {}, and active {} [operation ID {}]",
                task, oneOffTask, connectionPool.getNumIdle(task.getEndpoint()),
                connectionPool.getNumActive(task.getEndpoint()), operationId);
        long connectionBorrowStart = System.currentTimeMillis();
        ModbusFailureCallback<R> failureCallback = task.getFailureCallback();
        ModbusSlaveEndpoint endpoint = task.getEndpoint();

        R request = task.getRequest();
        Optional<ModbusSlaveConnection> connection = timer.connection.timeSupplier(() -> borrowConnection(endpoint));
        logger.trace("Executing task {} (oneOff={})! Connection received in {} ms [operation ID {}]", task, oneOffTask,
                System.currentTimeMillis() - connectionBorrowStart, operationId);
        if (scheduledThreadPoolExecutor == null) {
            // manager deactivated
            timer.connection.timeRunnable(() -> invalidate(endpoint, connection));
            return Optional.empty();
        }
        if (!connection.isPresent()) {
            logger.warn("Could not connect to endpoint {} -- aborting request {} [operation ID {}]", endpoint, request,
                    operationId);
            timer.callback.timeRunnable(
                    () -> invokeCallbackWithError(request, failureCallback, new ModbusConnectionException(endpoint)));
        }
        return connection;
    }

    private <R> void invokeCallbackWithError(R request, ModbusFailureCallback<R> callback, Exception error) {
        try {
            logger.trace("Calling error response callback {} for request {}. Error was {} {}", callback, request,
                    error.getClass().getName(), error.getMessage());
            callback.handle(new AsyncModbusFailure<R>(request, error));
        } finally {
            logger.trace("Called write response callback {} for request {}. Error was {} {}", callback, request,
                    error.getClass().getName(), error.getMessage());
        }
    }

    private void invokeCallbackWithResponse(ModbusWriteRequestBlueprint request, ModbusWriteCallback callback,
            org.openhab.core.io.transport.modbus.ModbusResponse response) {
        try {
            logger.trace("Calling write response callback {} for request {}. Response was {}", callback, request,
                    response);
            callback.handle(new AsyncModbusWriteResult(request, response));
        } finally {
            logger.trace("Called write response callback {} for request {}. Response was {}", callback, request,
                    response);
        }
    }

    private void verifyTaskIsRegistered(PollTask task) throws PollTaskUnregistered {
        if (!this.scheduledPollTasks.containsKey(task)) {
            String msg = String.format("Poll task %s is unregistered", task);
            logger.debug(msg);
            throw new PollTaskUnregistered(msg);
        }
    }

    /**
     * Execute operation using a retry mechanism.
     *
     * This is a helper function for executing read and write operations and handling the exceptions in a common way.
     *
     * With some connection types, the connection is reseted (disconnected), and new connection is received from the
     * pool. This means that potentially other operations queuing for the connection can be executed in-between.
     *
     * With some other connection types, the operation is retried without reseting the connection type.
     *
     * @param task
     * @param oneOffTask
     * @param operation
     */
    private <R, C extends ModbusResultCallback, F extends ModbusFailureCallback<R>, T extends TaskWithEndpoint<R, C, F>> void executeOperation(
            T task, boolean oneOffTask, ModbusOperation<T> operation) {
        AggregateStopWatch timer = new AggregateStopWatch();
        timer.total.resume();
        String operationId = timer.operationId;

        ModbusSlaveConnectionFactoryImpl connectionFactory = this.connectionFactory;
        if (connectionFactory == null) {
            // deactivated manager
            logger.trace("Deactivated manager - aborting operation.");
            return;
        }

        logTaskQueueInfo();
        R request = task.getRequest();
        ModbusSlaveEndpoint endpoint = task.getEndpoint();
        F failureCallback = task.getFailureCallback();
        int maxTries = task.getMaxTries();
        AtomicReference<@Nullable Exception> lastError = new AtomicReference<>();
        @SuppressWarnings("null") // since cfg in lambda cannot be really null
        long retryDelay = Optional.ofNullable(connectionFactory.getEndpointPoolConfiguration(endpoint))
                .map(cfg -> cfg.getInterTransactionDelayMillis()).orElse(0L);

        if (maxTries <= 0) {
            throw new IllegalArgumentException("maxTries should be positive");
        }

        Optional<ModbusSlaveConnection> connection = Optional.empty();
        try {
            logger.trace("Starting new operation with task {}. Trying to get connection [operation ID {}]", task,
                    operationId);
            connection = getConnection(timer, oneOffTask, task);
            logger.trace("Operation with task {}. Got a connection {} [operation ID {}]", task,
                    connection.isPresent() ? "successfully" : "which was unconnected (connection issue)", operationId);
            if (!connection.isPresent()) {
                // Could not acquire connection, time to abort
                // Error logged already, error callback called as well
                logger.trace("Initial connection was not successful, aborting. [operation ID {}]", operationId);
                return;
            }

            if (scheduledThreadPoolExecutor == null) {
                logger.debug("Manager has been shut down, aborting proecssing request {} [operation ID {}]", request,
                        operationId);
                return;
            }

            int tryIndex = 0;
            /**
             * last execution is tracked such that the endpoint is not spammed on retry. First retry can be executed
             * right away since getConnection ensures enough time has passed since last transaction. More precisely,
             * ModbusSlaveConnectionFactoryImpl sleeps on activate() (i.e. before returning connection).
             */
            @Nullable
            Long lastTryMillis = null;
            while (tryIndex < maxTries) {
                logger.trace("Try {} out of {} [operation ID {}]", tryIndex + 1, maxTries, operationId);
                if (!connection.isPresent()) {
                    // Connection was likely reseted with previous try, and connection was not successfully
                    // re-established. Error has been logged, time to abort.
                    logger.trace("Try {} out of {}. Connection was not successful, aborting. [operation ID {}]",
                            tryIndex + 1, maxTries, operationId);
                    return;
                }
                if (Thread.interrupted()) {
                    logger.warn("Thread interrupted. Aborting operation [operation ID {}]", operationId);
                    return;
                }
                // Check poll task is still registered (this is all asynchronous)
                if (!oneOffTask && task instanceof PollTask) {
                    verifyTaskIsRegistered((PollTask) task);
                }
                // Let's ensure that enough time is between the retries
                logger.trace(
                        "Ensuring that enough time passes before retrying again. Sleeping if necessary [operation ID {}]",
                        operationId);
                long slept = ModbusSlaveConnectionFactoryImpl.waitAtleast(lastTryMillis, retryDelay);
                logger.trace("Sleep ended, slept {} [operation ID {}]", slept, operationId);

                boolean willRetry = false;
                try {
                    tryIndex++;
                    willRetry = tryIndex < maxTries;
                    operation.accept(timer, task, connection.get());
                    lastError.set(null);
                    break;
                } catch (IOException e) {
                    lastError.set(new ModbusSlaveIOExceptionImpl(e));
                    // IO exception occurred, we re-establish new connection hoping it would fix the issue (e.g.
                    // broken pipe on write)
                    if (willRetry) {
                        logger.warn(
                                "Try {} out of {} failed when executing request ({}). Will try again soon. Error was I/O error, so reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, maxTries, request, e.getClass().getName(), e.getMessage(), operationId);
                    } else {
                        logger.error(
                                "Last try {} failed when executing request ({}). Aborting. Error was I/O error, so reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, request, e.getClass().getName(), e.getMessage(), operationId);
                    }
                    // Invalidate connection, and empty (so that new connection is acquired before new retry)
                    timer.connection.timeConsumer(c -> invalidate(endpoint, c), connection);
                    connection = Optional.empty();
                    continue;
                } catch (ModbusIOException e) {
                    lastError.set(new ModbusSlaveIOExceptionImpl(e));
                    // IO exception occurred, we re-establish new connection hoping it would fix the issue (e.g.
                    // broken pipe on write)
                    if (willRetry) {
                        logger.warn(
                                "Try {} out of {} failed when executing request ({}). Will try again soon. Error was I/O error, so reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, maxTries, request, e.getClass().getName(), e.getMessage(), operationId);
                    } else {
                        logger.error(
                                "Last try {} failed when executing request ({}). Aborting. Error was I/O error, so reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, request, e.getClass().getName(), e.getMessage(), operationId);
                    }
                    // Invalidate connection, and empty (so that new connection is acquired before new retry)
                    timer.connection.timeConsumer(c -> invalidate(endpoint, c), connection);
                    connection = Optional.empty();
                    continue;
                } catch (ModbusSlaveException e) {
                    lastError.set(new ModbusSlaveErrorResponseExceptionImpl(e));
                    // Slave returned explicit error response, no reason to re-establish new connection
                    if (willRetry) {
                        logger.warn(
                                "Try {} out of {} failed when executing request ({}). Will try again soon. Error was: {} {} [operation ID {}]",
                                tryIndex, maxTries, request, e.getClass().getName(), e.getMessage(), operationId);
                    } else {
                        logger.error(
                                "Last try {} failed when executing request ({}). Aborting. Error was: {} {} [operation ID {}]",
                                tryIndex, request, e.getClass().getName(), e.getMessage(), operationId);
                    }
                    continue;
                } catch (ModbusUnexpectedTransactionIdException | ModbusUnexpectedResponseFunctionCodeException
                        | ModbusUnexpectedResponseSizeException e) {
                    lastError.set(e);
                    // transaction error details already logged
                    if (willRetry) {
                        logger.warn(
                                "Try {} out of {} failed when executing request ({}). Will try again soon. The response did not match the request. Reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, maxTries, request, e.getClass().getName(), e.getMessage(), operationId);
                    } else {
                        logger.error(
                                "Last try {} failed when executing request ({}). Aborting. The response did not match the request. Reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, request, e.getClass().getName(), e.getMessage(), operationId);
                    }
                    // Invalidate connection, and empty (so that new connection is acquired before new retry)
                    timer.connection.timeConsumer(c -> invalidate(endpoint, c), connection);
                    connection = Optional.empty();
                    continue;
                } catch (ModbusException e) {
                    lastError.set(e);
                    // Some other (unexpected) exception occurred
                    if (willRetry) {
                        logger.warn(
                                "Try {} out of {} failed when executing request ({}). Will try again soon. Error was unexpected error, so reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, maxTries, request, e.getClass().getName(), e.getMessage(), operationId, e);
                    } else {
                        logger.error(
                                "Last try {} failed when executing request ({}). Aborting. Error was unexpected error, so reseting the connection. Error details: {} {} [operation ID {}]",
                                tryIndex, request, e.getClass().getName(), e.getMessage(), operationId, e);
                    }
                    // Invalidate connection, and empty (so that new connection is acquired before new retry)
                    timer.connection.timeConsumer(c -> invalidate(endpoint, c), connection);
                    connection = Optional.empty();
                    continue;
                } finally {
                    lastTryMillis = System.currentTimeMillis();
                    // Connection was reseted in error handling and needs to be reconnected.
                    // Try to re-establish connection.
                    if (willRetry && !connection.isPresent()) {
                        connection = getConnection(timer, oneOffTask, task);
                    }
                }
            }
            Exception exception = lastError.get();
            if (exception != null) {
                // All retries failed with some error
                timer.callback.timeRunnable(() -> {
                    invokeCallbackWithError(request, failureCallback, exception);
                });
            }
        } catch (PollTaskUnregistered e) {
            logger.warn("Poll task was unregistered -- not executing/proceeding with the poll: {} [operation ID {}]",
                    e.getMessage(), operationId);
            return;
        } catch (InterruptedException e) {
            logger.warn("Poll task was canceled -- not executing/proceeding with the poll: {} [operation ID {}]",
                    e.getMessage(), operationId);
            // Invalidate connection, and empty (so that new connection is acquired before new retry)
            timer.connection.timeConsumer(c -> invalidate(endpoint, c), connection);
            connection = Optional.empty();
        } finally {
            timer.connection.timeConsumer(c -> returnConnection(endpoint, c), connection);
            logger.trace("Connection was returned to the pool, ending operation [operation ID {}]", operationId);
            timer.suspendAllRunning();
            logger.debug("Modbus operation ended, timing info: {} [operation ID {}]", timer, operationId);
        }
    }

    private class ModbusCommunicationInterfaceImpl implements ModbusCommunicationInterface {

        private volatile ModbusSlaveEndpoint endpoint;
        private volatile Set<PollTask> pollTasksRegisteredByThisCommInterface = new ConcurrentHashSet<>();
        private volatile boolean closed;
        private @Nullable EndpointPoolConfiguration configuration;

        @SuppressWarnings("null")
        public ModbusCommunicationInterfaceImpl(ModbusSlaveEndpoint endpoint,
                @Nullable EndpointPoolConfiguration configuration) {
            this.endpoint = endpoint;
            this.configuration = configuration;
            connectionFactory.setEndpointPoolConfiguration(endpoint, configuration);
        }

        @Override
        public Future<?> submitOneTimePoll(ModbusReadRequestBlueprint request, ModbusReadCallback resultCallback,
                ModbusFailureCallback<ModbusReadRequestBlueprint> failureCallback) {
            if (closed) {
                throw new IllegalStateException("Communication interface is closed already!");
            }
            ScheduledExecutorService executor = scheduledThreadPoolExecutor;
            Objects.requireNonNull(executor, "Not activated!");
            long scheduleTime = System.currentTimeMillis();
            BasicPollTask task = new BasicPollTask(endpoint, request, resultCallback, failureCallback);
            logger.debug("Scheduling one-off poll task {}", task);
            Future<?> future = executor.submit(() -> {
                long millisInThreadPoolWaiting = System.currentTimeMillis() - scheduleTime;
                logger.debug("Will now execute one-off poll task {}, waited in thread pool for {}", task,
                        millisInThreadPoolWaiting);
                executeOperation(task, true, pollOperation);
            });
            return future;
        }

        @Override
        public PollTask registerRegularPoll(ModbusReadRequestBlueprint request, long pollPeriodMillis,
                long initialDelayMillis, ModbusReadCallback resultCallback,
                ModbusFailureCallback<ModbusReadRequestBlueprint> failureCallback) {
            synchronized (ModbusManagerImpl.this) {
                if (closed) {
                    throw new IllegalStateException("Communication interface is closed already!");
                }
                ScheduledExecutorService executor = scheduledThreadPoolExecutor;
                Objects.requireNonNull(executor, "Not activated!");
                BasicPollTask task = new BasicPollTask(endpoint, request, resultCallback, failureCallback);
                logger.trace("Registering poll task {} with period {} using initial delay {}", task, pollPeriodMillis,
                        initialDelayMillis);
                if (scheduledPollTasks.containsKey(task)) {
                    logger.trace("Unregistering previous poll task (possibly with different period)");
                    unregisterRegularPoll(task);
                }
                ScheduledFuture<?> future = executor.scheduleWithFixedDelay(() -> {
                    long started = System.currentTimeMillis();
                    logger.debug("Executing scheduled ({}ms) poll task {}. Current millis: {}", pollPeriodMillis, task,
                            started);
                    try {
                        executeOperation(task, false, pollOperation);
                    } catch (RuntimeException e) {
                        // We want to catch all unexpected exceptions since all unhandled exceptions make
                        // ScheduledExecutorService halt the polling. It is better to print out the exception, and try
                        // again
                        // (on next poll cycle)
                        logger.warn(
                                "Execution of scheduled ({}ms) poll task {} failed unexpectedly. Ignoring exception, polling again according to poll interval.",
                                pollPeriodMillis, task, e);
                    }
                    long finished = System.currentTimeMillis();
                    logger.debug(
                            "Execution of scheduled ({}ms) poll task {} finished at {}. Was started at millis: {} (=duration of {} millis)",
                            pollPeriodMillis, task, finished, started, finished - started);
                }, initialDelayMillis, pollPeriodMillis, TimeUnit.MILLISECONDS);

                scheduledPollTasks.put(task, future);
                pollTasksRegisteredByThisCommInterface.add(task);
                logger.trace("Registered poll task {} with period {} using initial delay {}", task, pollPeriodMillis,
                        initialDelayMillis);
                return task;
            }
        }

        @SuppressWarnings({ "null", "unused" })
        @Override
        public boolean unregisterRegularPoll(PollTask task) {
            synchronized (ModbusManagerImpl.this) {
                if (closed) {
                    // Closed already, nothing to unregister
                    return false;
                }
                pollTasksRegisteredByThisCommInterface.remove(task);
                ModbusSlaveConnectionFactoryImpl localConnectionFactory = connectionFactory;
                Objects.requireNonNull(localConnectionFactory, "Not activated!");

                // cancel poller
                @Nullable
                ScheduledFuture<?> future = scheduledPollTasks.remove(task);
                if (future == null) {
                    // No such poll task
                    logger.warn("Caller tried to unregister nonexisting poll task {}", task);
                    return false;
                }
                logger.debug("Unregistering regular poll task {} (interrupting if necessary)", task);
                future.cancel(true);
                logger.debug("Poll task {} canceled", task);
                return true;
            }
        }

        @Override
        public Future<?> submitOneTimeWrite(ModbusWriteRequestBlueprint request, ModbusWriteCallback resultCallback,
                ModbusFailureCallback<ModbusWriteRequestBlueprint> failureCallback) {
            if (closed) {
                throw new IllegalStateException("Communication interface is closed already!");
            }
            ScheduledExecutorService localScheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
            Objects.requireNonNull(localScheduledThreadPoolExecutor, "Not activated!");
            WriteTask task = new BasicWriteTask(endpoint, request, resultCallback, failureCallback);
            long scheduleTime = System.currentTimeMillis();
            logger.debug("Scheduling one-off write task {}", task);
            Future<?> future = localScheduledThreadPoolExecutor.submit(() -> {
                long millisInThreadPoolWaiting = System.currentTimeMillis() - scheduleTime;
                logger.debug("Will now execute one-off write task {}, waited in thread pool for {}", task,
                        millisInThreadPoolWaiting);
                executeOperation(task, true, writeOperation);
            });
            return future;
        }

        @Override
        public void close() throws Exception {
            synchronized (ModbusManagerImpl.this) {
                if (closed) {
                    // Closed already, nothing to unregister
                    return;
                }
                // Iterate over all tasks registered by this communication interface, and unregister those
                // We copy pollTasksRegisteredByThisCommInterface temporarily so that unregisterRegularPoll can
                // remove entries from pollTasksRegisteredByThisCommInterface
                Iterable<PollTask> tasksToUnregister = new LinkedList<>(pollTasksRegisteredByThisCommInterface);
                for (PollTask task : tasksToUnregister) {
                    unregisterRegularPoll(task);
                }
                unregisterCommunicationInterface(this);
                closed = true;
            }
        }

        @Override
        public ModbusSlaveEndpoint getEndpoint() {
            return endpoint;
        }
    }

    @Override
    public ModbusCommunicationInterface newModbusCommunicationInterface(ModbusSlaveEndpoint endpoint,
            @Nullable EndpointPoolConfiguration configuration) throws IllegalArgumentException {
        boolean openCommFoundWithSameEndpointDifferentConfig = communicationInterfaces.stream()
                .filter(comm -> comm.endpoint.equals(endpoint))
                .anyMatch(comm -> comm.configuration != null && !comm.configuration.equals(configuration));
        if (openCommFoundWithSameEndpointDifferentConfig) {
            throw new IllegalArgumentException(
                    "Communication interface is already open with different configuration to this same endpoint");
        }

        ModbusCommunicationInterfaceImpl comm = new ModbusCommunicationInterfaceImpl(endpoint, configuration);
        communicationInterfaces.add(comm);
        return comm;
    }

    @Override
    public @Nullable EndpointPoolConfiguration getEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint) {
        Objects.requireNonNull(connectionFactory, "Not activated!");
        return connectionFactory.getEndpointPoolConfiguration(endpoint);
    }

    private void unregisterCommunicationInterface(ModbusCommunicationInterface commInterface) {
        communicationInterfaces.remove(commInterface);
        maybeCloseConnections(commInterface.getEndpoint());
    }

    private void maybeCloseConnections(ModbusSlaveEndpoint endpoint) {
        boolean lastCommWithThisEndpointWasRemoved = communicationInterfaces.stream()
                .filter(comm -> comm.endpoint.equals(endpoint)).count() == 0L;
        if (lastCommWithThisEndpointWasRemoved) {
            // Since last communication interface pointing to this endpoint was closed, we can clean up resources
            // and disconnect connections.

            // Make sure connections to this endpoint are closed when they are returned to pool (which
            // is usually pretty soon as transactions should be relatively short-lived)
            ModbusSlaveConnectionFactoryImpl localConnectionFactory = connectionFactory;
            if (localConnectionFactory != null) {
                localConnectionFactory.disconnectOnReturn(endpoint, System.currentTimeMillis());
                try {
                    // Close all idle connections as well (they will be reconnected if necessary on borrow)
                    if (connectionPool != null) {
                        connectionPool.clear(endpoint);
                    }
                } catch (Exception e) {
                    logger.warn("Could not clear endpoint {}. Stack trace follows", endpoint, e);
                }
            }
        }
    }

    @Activate
    protected void activate(Map<String, Object> configProperties) {
        synchronized (this) {
            logger.info("Modbus manager activated");
            if (connectionPool == null) {
                constructConnectionPool();
            }
            ScheduledExecutorService scheduledThreadPoolExecutor = this.scheduledThreadPoolExecutor;
            if (scheduledThreadPoolExecutor == null) {
                this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor = ThreadPoolManager
                        .getScheduledPool(MODBUS_POLLER_THREAD_POOL_NAME);
            }
            if (scheduledThreadPoolExecutor.isShutdown()) {
                logger.warn("Thread pool is shut down! Aborting activation of ModbusMangerImpl");
                throw new IllegalStateException("Thread pool(s) shut down! Aborting activation of ModbusMangerImpl");
            }
            monitorFuture = scheduledThreadPoolExecutor.scheduleWithFixedDelay(this::logTaskQueueInfo, 0,
                    MONITOR_QUEUE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Deactivate
    protected void deactivate() {
        synchronized (this) {
            KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> connectionPool = this.connectionPool;
            if (connectionPool != null) {

                for (ModbusCommunicationInterface commInterface : this.communicationInterfaces) {
                    try {
                        commInterface.close();
                    } catch (Exception e) {
                        logger.warn("Error when closing communication interface", e);
                    }
                }

                connectionPool.close();
                this.connectionPool = connectionPool = null;
            }

            if (monitorFuture != null) {
                monitorFuture.cancel(true);
                monitorFuture = null;
            }
            // Note that it is not allowed to shutdown the executor, since they will be reused when
            // when pool is received from ThreadPoolManager is called
            scheduledThreadPoolExecutor = null;
            connectionFactory = null;
            logger.debug("Modbus manager deactivated");
        }
    }

    private void logTaskQueueInfo() {
        synchronized (pollMonitorLogger) {
            ScheduledExecutorService scheduledThreadPoolExecutor = this.scheduledThreadPoolExecutor;
            if (scheduledThreadPoolExecutor == null) {
                return;
            }
            // Avoid excessive spamming with queue monitor when many tasks are executed
            if (System.currentTimeMillis() - lastQueueMonitorLog < MONITOR_QUEUE_INTERVAL_MILLIS) {
                return;
            }
            lastQueueMonitorLog = System.currentTimeMillis();
            pollMonitorLogger.trace("<POLL MONITOR>");
            this.scheduledPollTasks.forEach((task, future) -> {
                pollMonitorLogger.trace(
                        "POLL MONITOR: scheduled poll task. FC: {}, start {}, length {}, done: {}, canceled: {}, delay: {}. Full task {}",
                        task.getRequest().getFunctionCode(), task.getRequest().getReference(),
                        task.getRequest().getDataLength(), future.isDone(), future.isCancelled(),
                        future.getDelay(TimeUnit.MILLISECONDS), task);
            });
            if (scheduledThreadPoolExecutor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor executor = ((ThreadPoolExecutor) scheduledThreadPoolExecutor);
                pollMonitorLogger.trace(
                        "POLL MONITOR: scheduledThreadPoolExecutor queue size: {}, remaining space {}. Active threads {}",
                        executor.getQueue().size(), executor.getQueue().remainingCapacity(), executor.getActiveCount());
                if (executor.getQueue().size() >= WARN_QUEUE_SIZE) {
                    pollMonitorLogger.warn(
                            "Many ({}) tasks queued in scheduledThreadPoolExecutor! This might be sign of bad design or bug in the binding code.",
                            executor.getQueue().size());
                }
            }

            pollMonitorLogger.trace("</POLL MONITOR>");
        }
    }
}
