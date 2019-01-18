/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.transport.mqtt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.io.transport.mqtt.internal.ClientCallback;
import org.eclipse.smarthome.io.transport.mqtt.internal.MqttActionAdapterCallback;
import org.eclipse.smarthome.io.transport.mqtt.internal.TopicSubscribers;
import org.eclipse.smarthome.io.transport.mqtt.reconnect.AbstractReconnectStrategy;
import org.eclipse.smarthome.io.transport.mqtt.reconnect.PeriodicReconnectStrategy;
import org.eclipse.smarthome.io.transport.mqtt.sslcontext.AcceptAllCertificatesSSLContext;
import org.eclipse.smarthome.io.transport.mqtt.sslcontext.SSLContextProvider;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An MQTTBrokerConnection represents a single client connection to a MQTT broker.
 *
 * When a connection to an MQTT broker is lost, it will try to reconnect every 60 seconds.
 *
 * @author David Graeff - All operations are async now. More flexible sslContextProvider and reconnectStrategy added.
 * @author Davy Vanherbergen
 * @author Markus Rathgeb - added connection state callback
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
    };

    /// Connection parameters
    protected final Protocol protocol;
    protected final String host;
    protected final int port;
    protected final boolean secure;
    protected final String clientId;
    private @Nullable String user;
    private @Nullable String password;

    /// Configuration variables
    private int qos = DEFAULT_QOS;
    private boolean retain = false;
    private @Nullable MqttWillAndTestament lastWill;
    private @Nullable Path persistencePath;
    protected @Nullable AbstractReconnectStrategy reconnectStrategy;
    private SSLContextProvider sslContextProvider = new AcceptAllCertificatesSSLContext();
    private int keepAliveInterval = DEFAULT_KEEPALIVE_INTERVAL;

    /// Runtime variables
    protected @Nullable MqttAsyncClient client;
    protected @Nullable MqttClientPersistence dataStore;
    protected boolean isConnecting = false;
    protected final List<MqttConnectionObserver> connectionObservers = new CopyOnWriteArrayList<>();

    protected final Map<String, TopicSubscribers> subscribers = new HashMap<>();

    // Connection timeout handling
    protected final AtomicReference<@Nullable ScheduledFuture<?>> timeoutFuture = new AtomicReference<>(null);
    protected @Nullable ScheduledExecutorService timeoutExecutor;
    private int timeout = 1200; /* Connection timeout in milliseconds */

    /**
     * Create a IMqttActionListener object for being used as a callback for a connection attempt.
     * The callback will interact with the {@link AbstractReconnectStrategy} as well as inform registered
     * {@link MqttConnectionObserver}s.
     */
    @NonNullByDefault({})
    public class ConnectionCallback implements IMqttActionListener {
        private final MqttBrokerConnection connection;
        private final Runnable cancelTimeoutFuture;
        private CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();

        public ConnectionCallback(MqttBrokerConnection mqttBrokerConnectionImpl) {
            this.connection = mqttBrokerConnectionImpl;
            this.cancelTimeoutFuture = mqttBrokerConnectionImpl::cancelTimeoutFuture;
        }

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            cancelTimeoutFuture.run();

            connection.isConnecting = false;
            if (connection.reconnectStrategy != null) {
                connection.reconnectStrategy.connectionEstablished();
            }
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            connection.subscribers.forEach((topic, subscriberList) -> {
                futures.add(connection.subscribeRaw(topic));
            });

            // As soon as all subscriptions are performed, turn the connection future complete.
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenRun(() -> {
                future.complete(true);
                connection.connectionObservers
                        .forEach(o -> o.connectionStateChanged(connection.connectionState(), null));
            });
        }

        @Override
        public void onFailure(@Nullable IMqttToken token, @Nullable Throwable error) {
            cancelTimeoutFuture.run();

            final Throwable throwable = (token != null && token.getException() != null) ? token.getException() : error;

            final MqttConnectionState connectionState = connection.connectionState();
            future.complete(false);
            connection.connectionObservers.forEach(o -> o.connectionStateChanged(connectionState, throwable));

            // If we tried to connect via start(), use the reconnect strategy to try it again
            if (connection.isConnecting) {
                connection.isConnecting = false;
                if (connection.reconnectStrategy != null) {
                    connection.reconnectStrategy.lostConnection();
                }
            }
        }

        public CompletableFuture<Boolean> createFuture() {
            future = new CompletableFuture<Boolean>();
            return future;
        }
    }

    /** Client callback object */
    protected ClientCallback clientCallback = new ClientCallback(this, connectionObservers, subscribers);
    /** Connection callback object */
    protected ConnectionCallback connectionCallback;
    /** Action callback object */
    protected IMqttActionListener actionCallback = new MqttActionAdapterCallback();

    /**
     * Create a new TCP MQTT client connection to a server with the given host and port.
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
        this(Protocol.TCP, host, port, secure, clientId);
    }

    /**
     * Create a new MQTT client connection to a server with the given protocol, host and port.
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
    public MqttBrokerConnection(Protocol protocol, String host, @Nullable Integer port, boolean secure,
            @Nullable String clientId) {
        this.protocol = protocol;
        this.host = host;
        this.secure = secure;
        String newClientID = clientId;
        if (newClientID == null) {
            newClientID = MqttClient.generateClientId();
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
     * timed out. You need a timeout executor additionally, see {@link #setTimeoutExecutor(Executor)}.
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

    /**
     * Get the MQTT broker protocol
     */
    public Protocol getProtocol() {
        return protocol;
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
        if (qos >= 0 && qos <= 2) {
            this.qos = qos;
        } else {
            throw new IllegalArgumentException("The quality of service parameter must be >=0 and <=2.");
        }
    }

    /**
     * @return true if newly messages sent to the broker should be retained by the broker.
     */
    public boolean isRetain() {
        return retain;
    }

    /**
     * Set whether newly published messages should be retained by the broker.
     *
     * @param retain true to retain.
     */
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
    public void setPersistencePath(final @Nullable Path persistencePath) {
        this.persistencePath = persistencePath;
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
        return (client != null && client.isConnected()) ? MqttConnectionState.CONNECTED
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
    public SSLContextProvider getSSLContextProvider() {
        return sslContextProvider;
    }

    /**
     * Set the ssl context provider. The default provider is {@see AcceptAllCertifcatesSSLContext}.
     *
     * @return The ssl context provider. Should not be null, but the ssl context will in fact
     *         only be used if a ssl:// url is given.
     */
    public void setSSLContextProvider(SSLContextProvider sslContextProvider) {
        this.sslContextProvider = sslContextProvider;
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
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        synchronized (subscribers) {
            TopicSubscribers subscriberList = subscribers.getOrDefault(topic, new TopicSubscribers(topic));
            subscribers.put(topic, subscriberList);
            subscriberList.add(subscriber);
        }
        final MqttAsyncClient client = this.client;
        if (client == null) {
            future.completeExceptionally(new Exception("No MQTT client"));
            return future;
        }
        if (client.isConnected()) {
            try {
                client.subscribe(topic, qos, future, actionCallback);
            } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
                future.completeExceptionally(e);
            }
        } else {
            // The subscription will be performed on connecting.
            future.complete(false);
        }
        return future;
    }

    /**
     * Subscribes to a topic on the given connection, but does not alter the subscriber list.
     *
     * @param topic The topic to subscribe to.
     * @return Completes with true if successful. Exceptionally otherwise.
     */
    protected CompletableFuture<Boolean> subscribeRaw(String topic) {
        logger.trace("subscribeRaw message consumer for topic '{}' from broker '{}'", topic, host);
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        try {
            MqttAsyncClient client = this.client;
            if (client != null && client.isConnected()) {
                client.subscribe(topic, qos, future, actionCallback);
            } else {
                future.complete(false);
            }
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            logger.info("Error subscribing to topic {}", topic, e);
            future.completeExceptionally(e);
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

        synchronized (subscribers) {
            final @Nullable List<MqttMessageSubscriber> list = subscribers.get(topic);
            if (list == null) {
                return CompletableFuture.completedFuture(true);
            }
            list.remove(subscriber);
            if (!list.isEmpty()) {
                return CompletableFuture.completedFuture(true);
            }
            // Remove from subscriber list
            subscribers.remove(topic);
            // No more subscribers to this topic. Unsubscribe topic on the broker
            MqttAsyncClient client = this.client;
            if (client != null) {
                return unsubscribeRaw(client, topic);
            } else {
                return CompletableFuture.completedFuture(false);
            }
        }
    }

    /**
     * Unsubscribes from a topic on the given connection, but does not alter the subscriber list.
     *
     * @param client The client connection
     * @param topic The topic to unsubscribe from
     * @return Completes with true if successful. Completes with false if no broker connection is established.
     *         Exceptionally otherwise.
     */
    protected CompletableFuture<Boolean> unsubscribeRaw(MqttAsyncClient client, String topic) {
        logger.trace("Unsubscribing message consumer for topic '{}' from broker '{}'", topic, host);
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        try {
            if (client.isConnected()) {
                client.unsubscribe(topic, future, actionCallback);
            } else {
                future.complete(false);
            }
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            logger.info("Error unsubscribing topic from broker", e);
            future.completeExceptionally(e);
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
     * Create a MqttConnectOptions object using the fields of this MqttBrokerConnection instance.
     * Package local, for testing.
     */
    MqttConnectOptions createMqttOptions() throws ConfigurationException {
        MqttConnectOptions options = new MqttConnectOptions();

        if (!StringUtils.isBlank(user)) {
            options.setUserName(user);
        }
        if (!StringUtils.isBlank(password) && password != null) {
            options.setPassword(password.toCharArray());
        }
        if (secure) {
            options.setSocketFactory(sslContextProvider.getContext().getSocketFactory());
        }

        if (lastWill != null) {
            MqttWillAndTestament lastWill = this.lastWill; // Make eclipse happy
            options.setWill(lastWill.getTopic(), lastWill.getPayload(), lastWill.getQos(), lastWill.isRetain());
        }

        options.setKeepAliveInterval(keepAliveInterval);
        return options;
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
        if (client != null) {
            try {
                client.close();
            } catch (org.eclipse.paho.client.mqttv3.MqttException ignore) {
            }
            client = null;
        }

        CompletableFuture<Boolean> future = connectionCallback.createFuture();

        StringBuilder serverURI = new StringBuilder();
        switch (protocol) {
            case TCP:
                serverURI.append(secure ? "ssl://" : "tcp://");
                break;
            case WEBSOCKETS:
                serverURI.append(secure ? "wss://" : "ws://");
                break;
            default:
                future.completeExceptionally(new ConfigurationException("protocol", "Protocol unknown"));
                return future;
        }
        serverURI.append(host);
        serverURI.append(":");
        serverURI.append(port);

        // Storage
        Path persistencePath = this.persistencePath;
        if (persistencePath == null) {
            persistencePath = Paths.get(ConfigConstants.getUserDataFolder()).resolve("mqtt").resolve(host);
        }
        try {
            persistencePath = Files.createDirectories(persistencePath);
        } catch (IOException e) {
            future.completeExceptionally(new MqttException(e));
            return future;
        }
        MqttDefaultFilePersistence _dataStore = new MqttDefaultFilePersistence(persistencePath.toString());

        // Create the client
        MqttAsyncClient _client;
        try {
            _client = createClient(serverURI.toString(), clientId, _dataStore);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            future.completeExceptionally(new MqttException(e));
            return future;
        }

        // Assign to object
        this.client = _client;
        this.dataStore = _dataStore;

        // Connect
        _client.setCallback(clientCallback);
        try {
            _client.connect(createMqttOptions(), null, connectionCallback);
            logger.info("Starting MQTT broker connection to '{}' with clientid {} and file store '{}'", host,
                    getClientId(), persistencePath);
        } catch (org.eclipse.paho.client.mqttv3.MqttException | ConfigurationException e) {
            future.completeExceptionally(new MqttException(e));
            return future;
        }

        // Connect timeout
        ScheduledExecutorService executor = timeoutExecutor;
        if (executor != null) {
            final ScheduledFuture<?> timeoutFuture = this.timeoutFuture.getAndSet(executor.schedule(
                    () -> connectionCallback.onFailure(null, new TimeoutException()), timeout, TimeUnit.MILLISECONDS));
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
        }
        return future;
    }

    /**
     * Encapsulates the creation of the paho MqttAsyncClient
     *
     * @param serverURI A paho uri like ssl://host:port, tcp://host:port, ws[s]://host:port
     * @param clientId the mqtt client ID
     * @param dataStore The datastore to save qos!=0 messages until they are delivered.
     * @return Returns a valid MqttAsyncClient
     * @throws org.eclipse.paho.client.mqttv3.MqttException
     */
    protected MqttAsyncClient createClient(String serverURI, String clientId, MqttClientPersistence dataStore)
            throws org.eclipse.paho.client.mqttv3.MqttException {
        return new MqttAsyncClient(serverURI, clientId, dataStore);
    }

    /**
     * After a successful disconnect, the underlying library objects need to be closed and connection observers want to
     * be notified.
     *
     * @param v A passthrough boolean value
     * @return Returns the value of the parameter v.
     */
    protected boolean finalizeStopAfterDisconnect(boolean v) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignore) {
            }
        }
        client = null;
        if (dataStore != null) {
            try {
                dataStore.close();
            } catch (Exception ignore) {
            }
            dataStore = null;
        }
        connectionObservers.forEach(o -> o.connectionStateChanged(MqttConnectionState.DISCONNECTED, null));
        return v;
    }

    /**
     * Unsubscribe from all topics
     *
     * @return Returns a future that completes as soon as all subscriptions have been canceled.
     */
    public CompletableFuture<Void> unsubscribeAll() {
        MqttAsyncClient client = this.client;
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        if (client != null) {
            subscribers.forEach((topic, subList) -> {
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
        MqttAsyncClient client = this.client;
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

        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        // Close connection
        if (client.isConnected()) {
            // We need to thread change here. Because paho does not allow to disconnect within a callback method
            unsubscribeAll().thenRunAsync(() -> {
                try {
                    client.disconnect(100).waitForCompletion(100);
                    if (client.isConnected()) {
                        client.disconnectForcibly();
                    }
                    future.complete(true);
                } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
                    logger.debug("Error while closing connection to broker", e);
                    future.complete(false);
                }
            });
        } else {
            future.complete(true);
        }

        return future.thenApply(this::finalizeStopAfterDisconnect);
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
    public void publish(String topic, byte[] payload, int qos, boolean retain, MqttActionCallback listener) {
        MqttAsyncClient client_ = client;
        if (client_ == null) {
            listener.onFailure(topic, new MqttException(0));
            return;
        }
        try {
            IMqttDeliveryToken deliveryToken = client_.publish(topic, payload, qos, retain, listener, actionCallback);
            logger.debug("Publishing message {} to topic '{}'", deliveryToken.getMessageId(), topic);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            listener.onFailure(topic, new MqttException(e));
        }
    }

    /**
     * Publish a message to the broker.
     *
     * @param topic The topic
     * @param payload The message payload
     * @param listener A listener to be notified of success or failure of the delivery.
     */
    public void publish(String topic, byte[] payload, MqttActionCallback listener) {
        publish(topic, payload, qos, retain, listener);
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
        return publish(topic, payload, qos, retain);
    }

    /**
     * Publish a message to the broker with the given QoS and retained flag.
     *
     * @param topic The topic
     * @param payload The message payload
     * @param qos The quality of service for this message
     * @param retain Set to true to retain the message on the broker
     * @param listener An optional listener to be notified of success or failure of the delivery.
     * @return Returns a future that completes with a result of true if the publishing succeeded and completes
     *         exceptionally on an error or with a result of false if no broker connection is established.
     */
    public CompletableFuture<Boolean> publish(String topic, byte[] payload, int qos, boolean retain) {
        MqttAsyncClient client = this.client;
        if (client == null) {
            return CompletableFuture.completedFuture(false);
        }
        // publish message asynchronously
        CompletableFuture<Boolean> f = new CompletableFuture<Boolean>();
        try {
            client.publish(topic, payload, qos, retain, f, actionCallback);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            f.completeExceptionally(new MqttException(e));
        }
        return f;
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
