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
package org.eclipse.smarthome.io.net.http.internal;

import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.io.net.http.ExtensibleTrustManager;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.eclipse.smarthome.io.net.http.HttpClientInitializationException;
import org.eclipse.smarthome.io.net.http.TrustManagerProvider;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class to create Jetty web clients
 *
 * @author Michael Bock - Initial contribution
 * @author Kai Kreuzer - added web socket support
 * @author Martin van Wingerden - Add support for ESHTrustManager
 */
@Component(immediate = true, configurationPid = "org.eclipse.smarthome.webclient")
@NonNullByDefault
public class WebClientFactoryImpl implements HttpClientFactory, WebSocketFactory {

    private final Logger logger = LoggerFactory.getLogger(WebClientFactoryImpl.class);

    private static final String CONFIG_MIN_THREADS_SHARED = "minThreadsShared";
    private static final String CONFIG_MAX_THREADS_SHARED = "maxThreadsShared";
    private static final String CONFIG_KEEP_ALIVE_SHARED = "keepAliveTimeoutShared";
    private static final String CONFIG_MIN_THREADS_CUSTOM = "minThreadsCustom";
    private static final String CONFIG_MAX_THREADS_CUSTOM = "maxThreadsCustom";
    private static final String CONFIG_KEEP_ALIVE_CUSTOM = "keepAliveTimeoutCustom";

    private static final int MIN_CONSUMER_NAME_LENGTH = 4;
    private static final int MAX_CONSUMER_NAME_LENGTH = 20;
    private static final Pattern CONSUMER_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-]*");

    private volatile @Nullable TrustManagerProvider trustmanagerProvider;
    private @NonNullByDefault({}) ExtensibleTrustManager extensibleTrustManager;

    private @NonNullByDefault({}) QueuedThreadPool threadPool;
    private @NonNullByDefault({}) HttpClient commonHttpClient;
    private @NonNullByDefault({}) WebSocketClient commonWebSocketClient;

    private int minThreadsShared;
    private int maxThreadsShared;
    private int keepAliveTimeoutShared; // in s
    private int minThreadsCustom;
    private int maxThreadsCustom;
    private int keepAliveTimeoutCustom; // in s

    @Activate
    protected void activate(Map<String, Object> parameters) {
        getConfigParameters(parameters);
    }

    @Modified
    protected void modified(Map<String, Object> parameters) {
        getConfigParameters(parameters);
        if (threadPool != null) {
            threadPool.setMinThreads(minThreadsShared);
            threadPool.setMaxThreads(maxThreadsShared);
            threadPool.setIdleTimeout(keepAliveTimeoutShared * 1000);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (commonHttpClient != null) {
            try {
                commonHttpClient.stop();
            } catch (Exception e) {
                logger.error("error while stopping shared Jetty http client", e);
                // nothing else we can do here
            }
            commonHttpClient = null;
            logger.debug("Jetty shared http client stopped");
        }
        if (commonWebSocketClient != null) {
            try {
                commonWebSocketClient.stop();
            } catch (Exception e) {
                logger.error("error while stopping shared Jetty web socket client", e);
                // nothing else we can do here
            }
            commonWebSocketClient = null;
            logger.debug("Jetty shared web socket client stopped");
        }
        threadPool = null;
    }

    @Override
    @Deprecated
    public HttpClient createHttpClient(String consumerName, String endpoint) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        logger.debug("http client for endpoint {} requested", endpoint);
        checkConsumerName(consumerName);
        return createHttpClientInternal(consumerName, endpoint, false, null);
    }

    @Override
    public HttpClient createHttpClient(String consumerName) {
        logger.debug("http client for consumer {} requested", consumerName);
        checkConsumerName(consumerName);
        return createHttpClientInternal(consumerName, null, false, null);
    }

    @Override
    @Deprecated
    public WebSocketClient createWebSocketClient(String consumerName, String endpoint) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        logger.debug("web socket client for endpoint {} requested", endpoint);
        checkConsumerName(consumerName);
        return createWebSocketClientInternal(consumerName, endpoint, false, null);
    }

    @Override
    public WebSocketClient createWebSocketClient(String consumerName) {
        logger.debug("web socket client for consumer {} requested", consumerName);
        checkConsumerName(consumerName);
        return createWebSocketClientInternal(consumerName, null, false, null);
    }

    @Override
    public HttpClient getCommonHttpClient() {
        initialize();
        logger.debug("shared http client requested");
        return commonHttpClient;
    }

    @Override
    public WebSocketClient getCommonWebSocketClient() {
        initialize();
        logger.debug("shared web socket client requested");
        return commonWebSocketClient;
    }

    private void getConfigParameters(Map<String, Object> parameters) {
        minThreadsShared = getConfigParameter(parameters, CONFIG_MIN_THREADS_SHARED, 10);
        maxThreadsShared = getConfigParameter(parameters, CONFIG_MAX_THREADS_SHARED, 40);
        keepAliveTimeoutShared = getConfigParameter(parameters, CONFIG_KEEP_ALIVE_SHARED, 300);
        minThreadsCustom = getConfigParameter(parameters, CONFIG_MIN_THREADS_CUSTOM, 5);
        maxThreadsCustom = getConfigParameter(parameters, CONFIG_MAX_THREADS_CUSTOM, 10);
        keepAliveTimeoutCustom = getConfigParameter(parameters, CONFIG_KEEP_ALIVE_CUSTOM, 300);
    }

    @SuppressWarnings({ "null", "unused" })
    private int getConfigParameter(Map<String, Object> parameters, String parameter, int defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        Object value = parameters.get(parameter);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("ignoring invalid value {} for parameter {}", value, parameter);
                return defaultValue;
            }
        } else {
            logger.warn("ignoring invalid type {} for parameter {}", value.getClass().getName(), parameter);
            return defaultValue;
        }
    }

    private synchronized void initialize() {
        if (threadPool == null || commonHttpClient == null || commonWebSocketClient == null) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<@Nullable Void>() {
                    @Override
                    public @Nullable Void run() {
                        if (threadPool == null) {
                            threadPool = createThreadPool("common", minThreadsShared, maxThreadsShared,
                                    keepAliveTimeoutShared);
                        }

                        if (commonHttpClient == null) {
                            commonHttpClient = createHttpClientInternal("common", null, true, threadPool);
                            // we need to set the stop timeout AFTER the client has been started, because
                            // otherwise the Jetty client sets it back to the default value.
                            // We need the stop timeout in order to prevent blocking the deactivation of this
                            // component, see https://github.com/eclipse/smarthome/issues/6632
                            threadPool.setStopTimeout(0);
                            logger.debug("Jetty shared http client created");
                        }

                        if (commonWebSocketClient == null) {
                            commonWebSocketClient = createWebSocketClientInternal("common", null, true, threadPool);
                            logger.debug("Jetty shared web socket client created");
                        }

                        return null;
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new HttpClientInitializationException(
                            "unexpected checked exception during initialization of the jetty client", cause);
                }
            }
        }
    }

    private HttpClient createHttpClientInternal(String consumerName, @Nullable String endpoint, boolean startClient,
            @Nullable QueuedThreadPool threadPool) {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<HttpClient>() {
                @Override
                public HttpClient run() {
                    if (logger.isDebugEnabled()) {
                        if (endpoint == null) {
                            logger.debug("creating http client for consumer {}", consumerName);
                        } else {
                            logger.debug("creating http client for consumer {}, endpoint {}", consumerName, endpoint);
                        }
                    }

                    HttpClient httpClient = new HttpClient(createSslContextFactory(endpoint));
                    httpClient.setMaxConnectionsPerDestination(2);

                    if (threadPool != null) {
                        httpClient.setExecutor(threadPool);
                    } else {
                        final QueuedThreadPool queuedThreadPool = createThreadPool(consumerName, minThreadsCustom,
                                maxThreadsCustom, keepAliveTimeoutCustom);
                        httpClient.setExecutor(queuedThreadPool);
                    }

                    if (startClient) {
                        try {
                            httpClient.start();
                        } catch (Exception e) {
                            logger.error("Could not start Jetty http client", e);
                            throw new HttpClientInitializationException("Could not start Jetty http client", e);
                        }
                    }

                    return httpClient;
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new HttpClientInitializationException(
                        "unexpected checked exception during initialization of the Jetty http client", cause);
            }
        }
    }

    private WebSocketClient createWebSocketClientInternal(String consumerName, @Nullable String endpoint,
            boolean startClient, @Nullable QueuedThreadPool threadPool) {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<WebSocketClient>() {
                @Override
                public WebSocketClient run() {
                    if (logger.isDebugEnabled()) {
                        if (endpoint == null) {
                            logger.debug("creating web socket client for consumer {}", consumerName);
                        } else {
                            logger.debug("creating web socket client for consumer {}, endpoint {}", consumerName,
                                    endpoint);
                        }
                    }

                    WebSocketClient webSocketClient = new WebSocketClient(createSslContextFactory(endpoint));
                    if (threadPool != null) {
                        webSocketClient.setExecutor(threadPool);
                    } else {
                        final QueuedThreadPool queuedThreadPool = createThreadPool(consumerName, minThreadsCustom,
                                maxThreadsCustom, keepAliveTimeoutCustom);
                        webSocketClient.setExecutor(queuedThreadPool);
                    }

                    if (startClient) {
                        try {
                            webSocketClient.start();
                        } catch (Exception e) {
                            logger.error("Could not start Jetty web socket client", e);
                            throw new HttpClientInitializationException("Could not start Jetty web socket client", e);
                        }
                    }

                    return webSocketClient;
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new HttpClientInitializationException(
                        "unexpected checked exception during initialization of the Jetty web socket client", cause);
            }
        }
    }

    private QueuedThreadPool createThreadPool(String consumerName, int minThreads, int maxThreads,
            int keepAliveTimeout) {
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreads, minThreads, keepAliveTimeout * 1000);
        queuedThreadPool.setName("ESH-httpClient-" + consumerName);
        queuedThreadPool.setDaemon(true);
        return queuedThreadPool;
    }

    private void checkConsumerName(String consumerName) {
        Objects.requireNonNull(consumerName, "consumerName must not be null");
        if (consumerName.length() < MIN_CONSUMER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "consumerName " + consumerName + " too short, minimum " + MIN_CONSUMER_NAME_LENGTH);
        }
        if (consumerName.length() > MAX_CONSUMER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "consumerName " + consumerName + " too long, maximum " + MAX_CONSUMER_NAME_LENGTH);
        }
        if (!CONSUMER_NAME_PATTERN.matcher(consumerName).matches()) {
            throw new IllegalArgumentException(
                    "consumerName " + consumerName + " contains illegal character, allowed only [a-zA-Z0-9_-]");
        }
    }

    private SslContextFactory createSslContextFactory(@Nullable String endpoint) {
        if (endpoint == null) {
            return createSslContextFactoryFromExtensibleTrustManager();
        } else {
            return createSslContextFactoryFromTrustManagerProvider(endpoint);
        }
    }

    private SslContextFactory createSslContextFactoryFromExtensibleTrustManager() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
        if (extensibleTrustManager != null) {
            try {
                logger.debug("Setting up SSLContext for {}", extensibleTrustManager);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] { extensibleTrustManager }, null);
                sslContextFactory.setSslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                throw new HttpClientInitializationException("Cannot create an TLS context!", ex);
            }
        }

        String excludeCipherSuites[] = { "^.*_(MD5)$" };
        sslContextFactory.setExcludeCipherSuites(excludeCipherSuites);
        return sslContextFactory;
    }

    @Deprecated
    private SslContextFactory createSslContextFactoryFromTrustManagerProvider(@Nullable String endpoint) {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
        if (endpoint != null && trustmanagerProvider != null) {
            Stream<TrustManager> trustManagerStream = trustmanagerProvider.getTrustManagers(endpoint);
            TrustManager[] trustManagers = trustManagerStream.toArray(TrustManager[]::new);
            if (trustManagers.length > 0) {
                logger.debug("using custom trustmanagers (certificate pinning) for httpClient for endpoint {}",
                        endpoint);
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustManagers, null);
                    sslContextFactory.setSslContext(sslContext);
                } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                    throw new HttpClientInitializationException(
                            "Cannot create an TLS context for the endpoint '" + endpoint + "'!", ex);
                }
            }
        }

        String excludeCipherSuites[] = { "^.*_(MD5)$" };
        sslContextFactory.setExcludeCipherSuites(excludeCipherSuites);
        return sslContextFactory;
    }

    @Reference
    protected void setExtensibleTrustManager(ExtensibleTrustManager extensibleTrustManager) {
        this.extensibleTrustManager = extensibleTrustManager;
    }

    protected void unsetExtensibleTrustManager(ExtensibleTrustManager extensibleTrustManager) {
        if (this.extensibleTrustManager == extensibleTrustManager) {
            this.extensibleTrustManager = null;
        }
    }

    @Reference(service = TrustManagerProvider.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Deprecated
    protected void setTrustmanagerProvider(TrustManagerProvider trustmanagerProvider) {
        this.trustmanagerProvider = trustmanagerProvider;
    }

    @Deprecated
    protected void unsetTrustmanagerProvider(TrustManagerProvider trustmanagerProvider) {
        if (this.trustmanagerProvider == trustmanagerProvider) {
            this.trustmanagerProvider = null;
        }
    }

}
