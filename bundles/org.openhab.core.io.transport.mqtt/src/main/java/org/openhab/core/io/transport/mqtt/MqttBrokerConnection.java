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
package org.openhab.core.io.transport.mqtt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.internal.Subscription;
import org.openhab.core.io.transport.mqtt.internal.client.Mqtt3AsyncClientWrapper;
import org.openhab.core.io.transport.mqtt.internal.client.Mqtt5AsyncClientWrapper;
import org.openhab.core.io.transport.mqtt.internal.client.MqttAsyncClientWrapper;
import org.openhab.core.io.transport.mqtt.reconnect.AbstractReconnectStrategy;
import org.openhab.core.io.transport.mqtt.reconnect.PeriodicReconnectStrategy;
import org.openhab.core.io.transport.mqtt.ssl.CustomTrustManagerFactory;
import org.openhab.core.io.transport.mqtt.sslcontext.CustomSSLContextProvider;
import org.openhab.core.io.transport.mqtt.sslcontext.SSLContextProvider;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * An MQTTBrokerConnection represents a single client connection to a MQTT broker.
 *
 * When a connection to an MQTT broker is lost, it will try to reconnect every 60 seconds.
 *
 * @author Davy Vanherbergen - Initial contribution
 * @author David Graeff - All operations are async now. More flexible sslContextProvider and reconnectStrategy added.
 * @author Markus Rathgeb - added connection state callback
 * @author Jan N. Klug - changed from PAHO to HiveMQ client
 */
@NonNullByDefault
public class MqttBrokerConnection {
    final Logger logger = LoggerFactory.getLogger(MqttBrokerConnection.class);
    public static final int DEFAULT_KEEPALIVE_INTERVAL = 60;
    public static final int DEFAULT_QOS = 0;

    /**
     * MQTT transport protocols
     */
    public enum Protocol {
        TCP,
        WEBSOCKETS
    }

    /**
     * MQTT version (currently v3 and v5)
     */
    public enum MqttVersion {
        V3,
        V5
    }

    // Connection parameters
    protected final Protocol protocol;
    protected final String host;
    protected final int port;
    protected final boolean secure;
    protected final MqttVersion mqttVersion;

    private @Nullable TrustManagerFactory trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
    private SSLContextProvider sslContextProvider = new CustomSSLContextProvider(trustManagerFactory);
    protected final String clientId;
    private @Nullable String user;
    private @Nullable String password;

    /// Configuration variables
    private int qos = DEFAULT_QOS;
    @Deprecated
    private boolean retain = false;
    private @Nullable MqttWillAndTestament lastWill;
    protected @Nullable AbstractReconnectStrategy reconnectStrategy;
    private int keepAliveInterval = DEFAULT_KEEPALIVE_INTERVAL;

    /// Runtime variables
    protected @Nullable MqttAsyncClientWrapper client;
    protected boolean isConnecting = false;
    protected final List<MqttConnectionObserver> connectionObservers = new CopyOnWriteArrayList<>();
    protected final Map<String, Subscription> subscribers = new ConcurrentHashMap<>();

    // Connection timeout handling
    protected final AtomicReference<@Nullable ScheduledFuture<?>> timeoutFuture = new AtomicReference<>(null);
    protected @Nullable ScheduledExecutorService timeoutExecutor;
    private int timeout = 1200; /* Connection timeout in milliseconds */

    /**
     * Create a listener object for being used as a callback for a connection attempt.
     * The callback will interact with the {@link AbstractReconnectStrategy} as well as inform registered
     * {@link MqttConnectionObserver}s.
     */
    @NonNullByDefault
    public class ConnectionCallback implements MqttClientConnectedListener, MqttClientDisconnectedListener {
        private final MqttBrokerConnection connection;
        private final Runnable cancelTimeoutFuture;
        private CompletableFuture<Boolean> future = new CompletableFuture<>();

        public ConnectionCallback(MqttBrokerConnection mqttBrokerConnectionImpl) {
            this.connection = mqttBrokerConnectionImpl;
            this.cancelTimeoutFuture = mqttBrokerConnectionImpl::cancelTimeoutFuture;
        }

        @Override
        public void onConnected(@Nullable MqttClientConnectedContext context) {
            cancelTimeoutFuture.run();

            connection.isConnecting = false;
            if (connection.reconnectStrategy != null) {
                connection.reconnectStrategy.connectionEstablished();
            }
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            connection.subscribers.forEach((topic, subscription) -> {
                futures.add(connection.subscribeRaw(topic, subscription));
            });

            // As soon as all subscriptions are performed, turn the connection future complete.
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenRun(() -> {
                future.complete(true);
                connection.connectionObservers
                        .forEach(o -> o.connectionStateChanged(connection.connectionState(), null));
            });
        }

        @Override
        public void onDisconnected(@Nullable MqttClientDisconnectedContext context) {
            if (context != null) {
                final Throwable throwable = context.getCause();
                onDisconnected(throwable);
            } else {
                onDisconnected(new Throwable("unknown disconnect reason"));
            }
        }

        public void onDisconnected(Throwable t) {
            cancelTimeoutFuture.run();

            final MqttConnectionState connectionState = connection.connectionState();
            future.complete(false);
            connection.connectionObservers.forEach(o -> o.connectionStateChanged(connectionState, t));

            // If we tried to connect via start(), use the reconnect strategy to try it again
            if (connection.isConnecting) {
                connection.isConnecting = false;
            }

            if (connection.reconnectStrategy != null) {
                connection.reconnectStrategy.lostConnection();
            }
        }

        public CompletableFuture<Boolean> createFuture() {
            future = new CompletableFuture<>();
            return future;
        }
    }

    // Connection callback object
    protected ConnectionCallback connectionCallback;

    /**
     * Create a new TCP MQTT3 client connection to a server with the given host and port.
     *
     * @param host A host name or address
     * @param port A port or null to select the default port for a secure or insecure connection
     * @param secure A secure connection
     * @param clientId Client id. Each client on a MQTT server has a unique client id. Sometimes client ids are
     *            used for access restriction implementations.
     *            If none is specified, a default is generated. The client id cannot be longer than 65535
     *            characters.
     * @throws IllegalArgumentException If the client id or port is not valid.
     */
    public MqttBrokerConnection(String host, @Nullable Integer port, boolean secure, @Nullable String clientId) {
        this(Protocol.TCP, MqttVersion.V3, host, port, secure, clientId);
    }

    /**
     * Create a new MQTT3 client connection to a server with the given protocol, mqtt client version, host and port.
     *
     * @param protocol The transport protocol
     * @param host A host name or address
     * @param port A port or null to select the default port for a secure or insecure connection
     * @param secure A secure connection
     * @param clientId Client id. Each client on a MQTT server has a unique client id. Sometimes client ids are
     *            used for access restriction implementations.
     *            If none is specified, a default is generated. The client id cannot be longer than 65535
     *            characters.
     * @throws IllegalArgumentException If the client id or port is not valid.
     */
    @Deprecated
    public MqttBrokerConnection(Protocol protocol, String host, @Nullable Integer port, boolean secure,
            @Nullable String clientId) {
        this(protocol, MqttVersion.V3, host, port, secure, clientId);
    }

    /**
     * Create a new MQTT client connection to a server with the given protocol, host and port.
     *
     * @param protocol The transport protocol
     * @param mqttVersion The version of the MQTT client (v3 or v5)
     * @param host A host name or address
     * @param port A port or null to select the default port for a secure or insecure connection
     * @param secure A secure connection
     * @param clientId Client id. Each client on a MQTT server has a unique client id. Sometimes client ids are
     *            used for access restriction implementations.
     *            If none is specified, a default is generated. The client id cannot be longer than 65535
     *            characters.
     * @throws IllegalArgumentException If the client id or port is not valid.
     */
    public MqttBrokerConnection(Protocol protocol, MqttVersion mqttVersion, String host, @Nullable Integer port,
            boolean secure, @Nullable String clientId) {
        this.protocol = protocol;
        this.host = host;
        this.secure = secure;
        this.mqttVersion = mqttVersion;
        String newClientID = clientId;
        if (newClientID == null) {
            newClientID = UUID.randomUUID().toString();
        } else if (newClientID.length() > 65535) {
            throw new IllegalArgumentException("Client ID cannot be longer than 65535 characters");
        }
        if (port != null && (port <= 0 || port > 65535)) {
            throw new IllegalArgumentException("Port is not within a valid range");
        }
        this.port = port != null ? port : (secure ? 8883 : 1883);
        this.clientId = newClientID;
        setReconnectStrategy(new PeriodicReconnectStrategy());
        connectionCallback = new ConnectionCallback(this);
    }

    /**
     * Set the reconnect strategy. The implementor will be called when the connection
     * state to the MQTT broker changed.
     *
     * The reconnect strategy will not be informed if the initial connection to the broker
     * timed out. You need a timeout executor additionally, see
     * {@link #setTimeoutExecutor(ScheduledExecutorService, int)}.
     *
     * @param reconnectStrategy The reconnect strategy. May not be null.
     */
    public void setReconnectStrategy(AbstractReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        reconnectStrategy.setBrokerConnection(this);
    }

    /**
     * @return Return the reconnect strategy
     */
    public @Nullable AbstractReconnectStrategy getReconnectStrategy() {
        return this.reconnectStrategy;
    }

    /**
     * Set a timeout executor. If none is set, you will not be notified of connection timeouts, this
     * also includes a non-firing reconnect strategy. The default executor is none.
     *
     * @param executor One timer will be created when a connection attempt happens
     * @param timeoutInMS Timeout in milliseconds
     */
    public void setTimeoutExecutor(@Nullable ScheduledExecutorService executor, int timeoutInMS) {
        timeoutExecutor = executor;
        this.timeout = timeoutInMS;
    }

    public void setTrustManagers(TrustManager[] trustManagers) {
        if (trustManagers.length != 0) {
            trustManagerFactory = new CustomTrustManagerFactory(trustManagers);
        } else {
            trustManagerFactory = null;
        }
        sslContextProvider = new CustomSSLContextProvider(trustManagerFactory);
    }

    public TrustManager[] getTrustManagers() {
        if (trustManagerFactory != null) {
            return trustManagerFactory.getTrustManagers();
        } else {
            return new TrustManager[] {};
        }
    }

    /**
     * Get the MQTT broker protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Get the MQTT version
     */
    public MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    /**
     * Get the MQTT broker host
     */
    public String getHost() {
        return host;
    }

    /**
     * Get the MQTT broker port
     */
    public int getPort() {
        return port;
    }

    /**
     * Return true if this is or will be an encrypted connection to the broker
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Set the optional user name and optional password to use when connecting to the MQTT broker.
     * The connection needs to be restarted for the new settings to take effect.
     *
     * @param user Name to use for connection.
     * @param password The password
     */
    public void setCredentials(@Nullable String user, @Nullable String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * @return connection password.
     */
    public @Nullable String getPassword() {
        return password;
    }

    /**
     * @return optional user name for the MQTT connection.
     */

    public @Nullable String getUser() {
        return user;
    }

    /**
     * @return quality of service level.
     */
    public int getQos() {
        return qos;
    }

    /**
     * Set quality of service. Valid values are 0, 1, 2 and mean
     * "at most once", "at least once" and "exactly once" respectively.
     * The connection needs to be restarted for the new settings to take effect.
     *
     * @param qos level.
     */
    public void setQos(int qos) {
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException();
        }
        this.qos = qos;
    }

    /**
     * use retain flags on message publish instead
     *
     * @return true if newly messages sent to the broker should be retained by the broker.
     */
    @Deprecated
    public boolean isRetain() {
        return retain;
    }

    /**
     * Set whether newly published messages should be retained by the broker.
     * use retain flags on message publish instead
     *
     * @param retain true to retain.
     */
    @Deprecated
    public void setRetain(boolean retain) {
        this.retain = retain;
    }

    /**
     * Return the last will object or null if there is none.
     */
    public @Nullable MqttWillAndTestament getLastWill() {
        return lastWill;
    }

    /**
     * Set the last will object.
     *
     * @param lastWill The last will object or null.
     * @param applyImmediately If true, the connection will stopped and started for the new last-will to take effect
     *            immediately.
     * @throws MqttException
     * @throws ConfigurationException
     */
    public void setLastWill(@Nullable MqttWillAndTestament lastWill, boolean applyImmediately)
            throws ConfigurationException, MqttException {
        this.lastWill = lastWill;
        if (applyImmediately) {
            stop();
            start();
        }
    }

    /**
     * Set the last will object.
     * The connection needs to be restarted for the new settings to take effect.
     *
     * @param lastWill The last will object or null.
     */
    public void setLastWill(@Nullable MqttWillAndTestament lastWill) {
        this.lastWill = lastWill;
    }

    /**
     * Sets the path for the persistence storage.
     *
     * A persistence mechanism is necessary to enable reliable messaging.
     * For messages sent at qualities of service (QoS) 1 or 2 to be reliably delivered, messages must be stored (on both
     * the client and server) until the delivery of the message is complete.
     * If messages are not safely stored when being delivered then a failure in the client or server can result in lost
     * messages.
     * A file persistence storage is used that uses the given path. If the path does not exist it will be created on
     * runtime (if possible). If it is set to {@code null} a implementation specific default path is used.
     *
     * @param persistencePath the path that should be used to store persistent data
     */
    @Deprecated
    public void setPersistencePath(final @Nullable Path persistencePath) {
    }

    /**
     * Get client id to use when connecting to the broker.
     *
     * @return value clientId to use.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the connection state
     */
    public MqttConnectionState connectionState() {
        if (isConnecting) {
            return MqttConnectionState.CONNECTING;
        }
        return (client != null && client.getState().isConnected()) ? MqttConnectionState.CONNECTED
                : MqttConnectionState.DISCONNECTED;
    }

    /**
     * Set the keep alive interval. The default interval is 60 seconds. If no heartbeat is received within this
     * timeframe, the connection will be considered dead. Set this to a higher value on systems which may not always be
     * able to process the heartbeat in time.
     *
     * @param keepAliveInterval interval in seconds
     */
    public void setKeepAliveInterval(int keepAliveInterval) {
        if (keepAliveInterval <= 0) {
            throw new IllegalArgumentException("Keep alive cannot be <=0");
        }
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * Return the keep alive internal in seconds
     */
    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Return the ssl context provider.
     */
    @Deprecated
    public SSLContextProvider getSSLContextProvider() {
        return sslContextProvider;
    }

    /**
     * Set the ssl context provider. The default provider is {@see AcceptAllCertifcatesSSLContext}.
     *
     * @return The ssl context provider. Should not be null, but the ssl context will in fact
     *         only be used if a ssl:// url is given.
     */
    @Deprecated
    public void setSSLContextProvider(SSLContextProvider sslContextProvider) {
        this.sslContextProvider = sslContextProvider;
        trustManagerFactory = new CustomTrustManagerFactory(sslContextProvider);
    }

    /**
     * Return true if there are subscribers registered via {@link #subscribe(String, MqttMessageSubscriber)}.
     * Call {@link #unsubscribe(String, MqttMessageSubscriber)} or {@link #unsubscribeAll()} if necessary.
     */
    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }

    /**
     * Add a new message consumer to this connection. Multiple subscribers with the same
     * topic are allowed. This method will not protect you from adding a subscriber object
     * multiple times!
     *
     * If there is a retained message for the topic, you are guaranteed to receive a callback
     * for each new subscriber, even for the same topic.
     *
     * @param topic The topic to subscribe to.
     * @param subscriber The callback listener for received messages for the given topic.
     * @return Completes with true if successful. Completes with false if not connected yet. Exceptionally otherwise.
     */
    public CompletableFuture<Boolean> subscribe(String topic, MqttMessageSubscriber subscriber) {
        final Subscription subscription;
        final boolean needsSubscribe;
        synchronized (subscribers) {
            subscription = subscribers.computeIfAbsent(topic, t -> new Subscription());

            needsSubscribe = subscription.isEmpty();

            subscription.add(subscriber);
        }

        if (needsSubscribe) {
            return subscribeRaw(topic, subscription);
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Subscribes to a topic on the given connection, but does not alter the subscriber list.
     *
     * @param topic The topic to subscribe to.
     * @return Completes with true if successful. Exceptionally otherwise.
     */
    protected CompletableFuture<Boolean> subscribeRaw(String topic, Subscription subscription) {
        logger.trace("subscribeRaw message consumer for topic '{}' from broker '{}'", topic, host);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final MqttAsyncClientWrapper mqttClient = this.client;
        if (mqttClient != null && mqttClient.getState().isConnected()) {
            mqttClient.subscribe(topic, qos, subscription).whenComplete((s, t) -> {
                if (t == null) {
                    logger.trace("Successfully subscribed to topic {}", topic);
                    future.complete(true);
                } else {
                    logger.warn("Failed subscribing to topic {}", topic, t);
                    future.completeExceptionally(new MqttException(t));
                }
            });
        } else {
            future.complete(false);
        }
        return future;
    }

    /**
     * Remove a previously registered consumer from this connection.
     * If no more consumers are registered for a topic, the topic will be unsubscribed from.
     *
     * @param topic The topic to unsubscribe from.
     * @param subscriber The callback listener to remove.
     * @return Completes with true if successful. Exceptionally otherwise.
     */
    @SuppressWarnings({ "null", "unused" })
    public CompletableFuture<Boolean> unsubscribe(String topic, MqttMessageSubscriber subscriber) {
        final boolean needsUnsubscribe;

        synchronized (subscribers) {
            final @Nullable Subscription subscription = subscribers.get(topic);
            if (subscription == null) {
                logger.trace("Tried to unsubscribe {} from topic {}, but subscriber list is empty", subscriber, topic);
                return CompletableFuture.completedFuture(true);
            }
            subscription.remove(subscriber);

            if (subscription.isEmpty()) {
                needsUnsubscribe = true;
                subscribers.remove(topic);
            } else {
                needsUnsubscribe = false;
            }
        }
        if (needsUnsubscribe) {
            MqttAsyncClientWrapper mqttClient = this.client;
            if (mqttClient != null) {
                logger.trace("Subscriber list is empty after removing {}, unsubscribing topic {} from client",
                        subscriber, topic);
                return unsubscribeRaw(mqttClient, topic);
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Unsubscribes from a topic on the given connection, but does not alter the subscriber list.
     *
     * @param client The client connection
     * @param topic The topic to unsubscribe from
     * @return Completes with true if successful. Completes with false if no broker connection is established.
     *         Exceptionally otherwise.
     */
    protected CompletableFuture<Boolean> unsubscribeRaw(MqttAsyncClientWrapper client, String topic) {
        logger.trace("Unsubscribing message consumer for topic '{}' from broker '{}'", topic, host);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (client.getState().isConnected()) {
            client.unsubscribe(topic).whenComplete((s, t) -> {
                if (t == null) {
                    future.complete(true);
                } else {
                    future.completeExceptionally(new MqttException(t));
                }
            });
        } else {
            return CompletableFuture.completedFuture(false);
        }
        return future;
    }

    /**
     * Add a new connection observer to this connection.
     *
     * @param connectionObserver The connection observer that should be added.
     */
    public synchronized void addConnectionObserver(MqttConnectionObserver connectionObserver) {
        connectionObservers.add(connectionObserver);
    }

    /**
     * Remove a previously registered connection observer from this connection.
     *
     * @param connectionObserver The connection observer that should be removed.
     */
    public synchronized void removeConnectionObserver(MqttConnectionObserver connectionObserver) {
        connectionObservers.remove(connectionObserver);
    }

    /**
     * Return true if there are connection observers registered via addConnectionObserver().
     */
    public boolean hasConnectionObservers() {
        return !connectionObservers.isEmpty();
    }

    /**
     * This will establish a connection to the MQTT broker and if successful, notify all
     * publishers and subscribers that the connection has become active. This method will
     * do nothing if there is already an active connection.
     *
     * @return Returns a future that completes with true if already connected or connecting,
     *         completes with false if a connection timeout has happened and completes exceptionally otherwise.
     */
    public CompletableFuture<Boolean> start() {
        // We don't want multiple concurrent threads to start a connection
        synchronized (this) {
            if (connectionState() != MqttConnectionState.DISCONNECTED) {
                return CompletableFuture.completedFuture(true);
            }

            // Perform the connection attempt
            isConnecting = true;
            connectionObservers.forEach(o -> o.connectionStateChanged(MqttConnectionState.CONNECTING, null));
        }

        // Ensure the reconnect strategy is started
        if (reconnectStrategy != null) {
            reconnectStrategy.start();
        }

        // Close client if there is still one existing

        if (this.client != null) {
            this.client.disconnect();
            this.client = null;
        }

        CompletableFuture<Boolean> future = connectionCallback.createFuture();

        // Create the client
        MqttAsyncClientWrapper client = createClient();
        this.client = client;

        // connect
        client.connect(lastWill, keepAliveInterval, user, password);

        logger.info("Starting MQTT broker connection to '{}' with clientid {}", host, getClientId());

        // Connect timeout
        ScheduledExecutorService executor = timeoutExecutor;
        if (executor != null) {
            final ScheduledFuture<?> timeoutFuture = this.timeoutFuture.getAndSet(executor.schedule(
                    () -> connectionCallback.onDisconnected(new TimeoutException("connect timed out")), timeout,
                    TimeUnit.MILLISECONDS));
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
        }
        return future;
    }

    protected MqttAsyncClientWrapper createClient() {
        if (mqttVersion == MqttVersion.V3) {
            return new Mqtt3AsyncClientWrapper(host, port, clientId, protocol, secure, connectionCallback,
                    trustManagerFactory);
        } else {
            return new Mqtt5AsyncClientWrapper(host, port, clientId, protocol, secure, connectionCallback,
                    trustManagerFactory);
        }
    }

    /**
     * After a successful disconnect, the underlying library objects need to be closed and connection observers want to
     * be notified.
     *
     * @param v A passthrough boolean value
     * @return Returns the value of the parameter v.
     */
    protected boolean finalizeStopAfterDisconnect(boolean v) {
        final MqttAsyncClientWrapper client = this.client;
        if (client != null && connectionState() != MqttConnectionState.DISCONNECTED) {
            client.disconnect();
        }
        this.client = null;
        connectionObservers.forEach(o -> o.connectionStateChanged(MqttConnectionState.DISCONNECTED, null));
        return v;
    }

    /**
     * Unsubscribe from all topics
     *
     * @return Returns a future that completes as soon as all subscriptions have been canceled.
     */
    public CompletableFuture<Void> unsubscribeAll() {
        MqttAsyncClientWrapper client = this.client;
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        if (client != null) {
            subscribers.forEach((topic, subscription) -> {
                futures.add(unsubscribeRaw(client, topic));
            });
            subscribers.clear();
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    /**
     * Unsubscribes from all subscribed topics, stops the reconnect strategy, disconnect and close the client.
     *
     * You can re-establish a connection calling {@link #start()} again. Do not call start, before the closing process
     * has finished completely.
     *
     * @return Returns a future that completes as soon as the disconnect process has finished.
     */
    public CompletableFuture<Boolean> stop() {
        MqttAsyncClientWrapper client = this.client;
        if (client == null) {
            return CompletableFuture.completedFuture(true);
        }

        logger.trace("Closing the MQTT broker connection '{}'", host);

        // Abort a connection attempt
        isConnecting = false;

        // Cancel the timeout future. If stop is called we can safely assume there is no interest in a connection
        // anymore.
        cancelTimeoutFuture();

        // Stop the reconnect strategy
        if (reconnectStrategy != null) {
            reconnectStrategy.stop();
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        // Close connection
        if (client.getState().isConnected()) {
            unsubscribeAll().thenRun(() -> {
                client.disconnect().whenComplete((m, t) -> {
                    if (t == null) {
                        future.complete(true);
                    } else {
                        future.complete(false);
                    }
                });
            });
        } else {
            future.complete(true);
        }

        return future.thenApply(this::finalizeStopAfterDisconnect);
    }

    /**
     * Publish a message to the broker.
     *
     * @param topic The topic
     * @param payload The message payload
     * @param listener A listener to be notified of success or failure of the delivery.
     */
    @Deprecated
    public void publish(String topic, byte[] payload, MqttActionCallback listener) {
        publish(topic, payload, getQos(), isRetain(), listener);
    }

    /**
     * Publish a message to the broker with the given QoS and retained flag.
     *
     * @param topic The topic
     * @param payload The message payload
     * @param qos The quality of service for this message
     * @param retain Set to true to retain the message on the broker
     * @param listener A listener to be notified of success or failure of the delivery.
     */
    @Deprecated
    public void publish(String topic, byte[] payload, int qos, boolean retain, MqttActionCallback listener) {
        final MqttAsyncClientWrapper client = this.client;
        if (client == null) {
            listener.onFailure(topic, new MqttException(new Throwable()));
            return;
        }

        client.publish(topic, payload, retain, qos).whenComplete((m, t) -> {
            if (t != null) {
                listener.onFailure(topic, new MqttException(t));
            } else {
                listener.onSuccess(topic);
            }
        });
        logger.debug("Publishing message to topic '{}'", topic);
    }

    /**
     * Publish a message to the broker.
     *
     * @param topic The topic
     * @param payload The message payload
     * @return Returns a future that completes with a result of true if the publishing succeeded and completes
     *         exceptionally on an error or with a result of false if no broker connection is established.
     */
    public CompletableFuture<Boolean> publish(String topic, byte[] payload) {
        return publish(topic, payload, getQos(), isRetain());
    }

    /**
     * Publish a message to the broker with the given QoS and retained flag.
     *
     * @param topic The topic
     * @param payload The message payload
     * @param qos The quality of service for this message
     * @param retain Set to true to retain the message on the broker
     * @return Returns a future that completes with a result of true if the publishing succeeded and completes
     *         exceptionally on an error or with a result of false if no broker connection is established.
     */
    public CompletableFuture<Boolean> publish(String topic, byte[] payload, int qos, boolean retain) {
        final MqttAsyncClientWrapper client = this.client;
        if (client == null) {
            return CompletableFuture.completedFuture(false);
        }

        // publish message asynchronously
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        client.publish(topic, payload, retain, qos).whenComplete((m, t) -> {
            if (t == null) {
                future.complete(true);
            } else {
                future.completeExceptionally(new MqttException(t));
            }
        });

        return future;
    }

    /**
     * The connection process is limited by a timeout, realized with a {@link CompletableFuture}. Cancel that future
     * now, if it exists.
     */
    protected void cancelTimeoutFuture() {
        final ScheduledFuture<?> timeoutFuture = this.timeoutFuture.getAndSet(null);
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

}
