/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.net.http;

import static org.eclipse.jetty.http.HttpMethod.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.library.types.RawType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some common methods to be used in HTTP-In-Binding, HTTP-Out-Binding and other bindings
 *
 * For advanced usage direct use of the Jetty client is preferred
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Svilen Valkanov - replaced Apache HttpClient with Jetty
 */
@Component(immediate = true)
public class HttpUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private static final String PROXY_CLIENT_CONSUMER_NAME = "HttpUtil-proxy";

    private static volatile HttpClientFactory httpClientFactory;

    /**
     * Maximum number of dedicated proxy http clients that are kept at the same time. Every client has its own thread
     * pool, so the number must stay small.
     */
    private static final int MAX_PROXY_HTTP_CLIENTS = 8;

    /**
     * Proxy http clients that were not used for this long are retired.
     */
    private static final long PROXY_HTTP_CLIENT_IDLE_NANOS = TimeUnit.MINUTES.toNanos(10);

    /**
     * Guards {@link #PROXY_HTTP_CLIENTS}, the leases of its entries and the binding of {@link #httpClientFactory}, so
     * that a client can never be published for a factory that is already gone.
     */
    private static final Object PROXY_LOCK = new Object();

    /**
     * Dedicated http clients for requests that have to be routed through a proxy, one per distinct proxy
     * configuration, in access order (least recently used first). The shared common http client must never be
     * modified, since it is used concurrently by everyone else. Guarded by {@link #PROXY_LOCK}.
     */
    private static final Map<ProxyKey, ProxyClient> PROXY_HTTP_CLIENTS = new LinkedHashMap<>(16, 0.75f, true);

    private record ProxyKey(String host, int port, String user, String password) {
    }

    /**
     * A cached proxy http client. A retired client is no longer handed out and is stopped as soon as the last request
     * that uses it has finished. All fields are guarded by {@link #PROXY_LOCK}.
     */
    private static final class ProxyClient {
        final HttpClient client;
        long lastUsedNanos = System.nanoTime();
        int leases;
        boolean retired;

        ProxyClient(HttpClient client) {
            this.client = client;
        }
    }

    private static class ProxyParams {
        String proxyHost;
        int proxyPort = 80;
        String proxyUser;
        String proxyPassword;
        String nonProxyHosts;
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>.
     * Furthermore the <code>http.proxyXXX</code> System variables are read and
     * set into the {@link HttpClient}.
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute
     * @param timeout the socket timeout in milliseconds to wait for data
     * @return the response body or <code>NULL</code> when the request went wrong
     * @throws IOException when the request execution failed, timed out or it was interrupted
     */
    public static String executeUrl(String httpMethod, String url, int timeout) throws IOException {
        return executeUrl(httpMethod, url, null, null, timeout);
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>.
     * Furthermore the <code>http.proxyXXX</code> System variables are read and
     * set into the {@link HttpClient}.
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute
     * @param content the content to be sent to the given <code>url</code> or <code>null</code> if no content should
     *            be sent.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout in milliseconds to wait for data
     * @return the response body or <code>NULL</code> when the request went wrong
     * @throws IOException when the request execution failed, timed out or it was interrupted
     */
    public static String executeUrl(String httpMethod, String url, InputStream content, String contentType, int timeout)
            throws IOException {
        return executeUrl(httpMethod, url, null, content, contentType, timeout);
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>.
     * Furthermore the <code>http.proxyXXX</code> System variables are read and
     * set into the {@link HttpClient}.
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute
     * @param httpHeaders optional http request headers which has to be sent within request
     * @param content the content to be sent to the given <code>url</code> or <code>null</code> if no content should
     *            be sent.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout in milliseconds to wait for data
     * @return the response body or <code>NULL</code> when the request went wrong
     * @throws IOException when the request execution failed, timed out or it was interrupted
     */
    public static String executeUrl(String httpMethod, String url, Properties httpHeaders, InputStream content,
            String contentType, int timeout) throws IOException {
        final ProxyParams proxyParams = prepareProxyParams();

        return executeUrl(httpMethod, url, httpHeaders, content, contentType, timeout, proxyParams.proxyHost,
                proxyParams.proxyPort, proxyParams.proxyUser, proxyParams.proxyPassword, proxyParams.nonProxyHosts);
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute
     * @param httpHeaders optional HTTP headers which has to be set on request
     * @param content the content to be sent to the given <code>url</code> or <code>null</code> if no content
     *            should be sent.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout in milliseconds to wait for data
     * @param proxyHost the hostname of the proxy
     * @param proxyPort the port of the proxy
     * @param proxyUser the username to authenticate with the proxy
     * @param proxyPassword the password to authenticate with the proxy
     * @param nonProxyHosts the hosts that won't be routed through the proxy
     * @return the response body or <code>NULL</code> when the request went wrong
     * @throws IOException when the request execution failed, timed out or it was interrupted
     */
    public static String executeUrl(String httpMethod, String url, Properties httpHeaders, InputStream content,
            String contentType, int timeout, String proxyHost, Integer proxyPort, String proxyUser,
            String proxyPassword, String nonProxyHosts) throws IOException {
        ContentResponse response = executeUrlAndGetResponse(httpMethod, url, httpHeaders, content, contentType, timeout,
                proxyHost, proxyPort, proxyUser, proxyPassword, nonProxyHosts);
        String encoding = response.getEncoding() != null ? response.getEncoding().replace("\"", "").trim()
                : StandardCharsets.UTF_8.name();
        String responseBody;
        try {
            responseBody = new String(response.getContent(), encoding);
        } catch (UnsupportedEncodingException e) {
            responseBody = null;
        }
        return responseBody;
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute
     * @param httpHeaders optional HTTP headers which has to be set on request
     * @param content the content to be sent to the given <code>url</code> or <code>null</code> if no content
     *            should be sent.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout in milliseconds to wait for data
     * @param proxyHost the hostname of the proxy
     * @param proxyPort the port of the proxy
     * @param proxyUser the username to authenticate with the proxy
     * @param proxyPassword the password to authenticate with the proxy
     * @param nonProxyHosts the hosts that won't be routed through the proxy
     * @return the response as a ContentResponse object or <code>NULL</code> when the request went wrong
     * @throws IOException when the request execution failed, timed out or it was interrupted
     */
    private static ContentResponse executeUrlAndGetResponse(String httpMethod, String url, Properties httpHeaders,
            InputStream content, String contentType, int timeout, String proxyHost, Integer proxyPort, String proxyUser,
            String proxyPassword, String nonProxyHosts) throws IOException {
        // Referenced http client factory not available
        final HttpClientFactory factory = httpClientFactory;
        if (factory == null) {
            throw new IllegalStateException("Http client factory not available");
        }
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (NullPointerException | URISyntaxException e) {
            LOGGER.debug("String {} can not be parsed as URI reference", url);
            throw new IOException(e);
        }

        // Only use a proxy if a host is provided. Requests without proxy use the shared http client from the
        // factory "on-demand", requests with proxy use a dedicated client, so that the shared client is never
        // modified.
        ProxyClient proxyClient = null;
        final HttpClient httpClient;
        if (proxyHost != null && !proxyHost.isBlank() && proxyPort != null && shouldUseProxy(uri, nonProxyHosts)) {
            proxyClient = acquireProxyClient(proxyHost, proxyPort, proxyUser, proxyPassword);
            httpClient = proxyClient.client;
        } else {
            httpClient = factory.getCommonHttpClient();
        }

        try {
            return sendRequest(httpClient, httpMethod, uri, httpHeaders, content, contentType, timeout);
        } finally {
            if (proxyClient != null) {
                releaseProxyClient(proxyClient);
            }
        }
    }

    private static ContentResponse sendRequest(HttpClient httpClient, String httpMethod, URI uri,
            Properties httpHeaders, InputStream content, String contentType, int timeout) throws IOException {
        final HttpMethod method = HttpUtil.createHttpMethod(httpMethod);

        final Request request = httpClient.newRequest(uri).method(method).timeout(timeout, TimeUnit.MILLISECONDS);

        if (httpHeaders != null) {
            for (String httpHeaderKey : httpHeaders.stringPropertyNames()) {
                if (httpHeaderKey.equalsIgnoreCase(HttpHeader.USER_AGENT.toString())) {
                    request.agent(httpHeaders.getProperty(httpHeaderKey));
                } else {
                    request.header(httpHeaderKey, httpHeaders.getProperty(httpHeaderKey));
                }
            }
        }

        // add basic auth header, if url contains user info
        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getUserInfo().split(":");

            String user = userInfo[0];
            String password = userInfo[1];

            String basicAuthentication = "Basic "
                    + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
            request.header(HttpHeader.AUTHORIZATION, basicAuthentication);
        }

        // add content if a valid method is given ...
        if (content != null && (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)
                || HttpMethod.PATCH.equals(method))) {
            // Close this outmost stream again after use!
            try (final InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(
                    content)) {
                request.content(inputStreamContentProvider, contentType);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("About to execute {}", request.getURI());
        }

        try {
            ContentResponse response = request.send();
            int statusCode = response.getStatus();
            if (LOGGER.isDebugEnabled() && statusCode >= HttpStatus.BAD_REQUEST_400) {
                String statusLine = statusCode + " " + response.getReason();
                LOGGER.debug("Method failed: {}", statusLine);
            }

            return response;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns the proxy http client for the given proxy configuration, creating it if necessary, and leases it. The
     * caller must call {@link #releaseProxyClient(ProxyClient)} when the request has finished.
     */
    private static ProxyClient acquireProxyClient(String proxyHost, int proxyPort, String proxyUser,
            String proxyPassword) throws IOException {
        final ProxyKey key = new ProxyKey(proxyHost, proxyPort, proxyUser == null ? "" : proxyUser,
                proxyPassword == null ? "" : proxyPassword);
        final List<HttpClient> toStop = new ArrayList<>();
        try {
            synchronized (PROXY_LOCK) {
                // The factory is read under the lock that unbinding also takes, so a client is never created for or
                // published after a factory that is already gone.
                final HttpClientFactory factory = httpClientFactory;
                if (factory == null) {
                    throw new IllegalStateException("Http client factory not available");
                }
                final long now = System.nanoTime();
                retireIdleProxyClients(now, toStop);

                ProxyClient entry = PROXY_HTTP_CLIENTS.get(key);
                if (entry == null) {
                    final HttpClient client = factory.createHttpClient(PROXY_CLIENT_CONSUMER_NAME);
                    try {
                        // drop proxies inherited from system properties, this client must only use the given one
                        client.getProxyConfiguration().getProxies().clear();
                        HttpProxy proxy = new HttpProxy(proxyHost, proxyPort);
                        client.getProxyConfiguration().getProxies().add(proxy);
                        if (!key.user().isBlank()) {
                            client.getAuthenticationStore().addAuthentication(new BasicAuthentication(proxy.getURI(),
                                    Authentication.ANY_REALM, key.user(), key.password()));
                        }
                        client.start();
                    } catch (Exception e) {
                        toStop.add(client);
                        throw new IOException("Could not start http client for proxy " + proxyHost + ":" + proxyPort,
                                e);
                    }
                    entry = new ProxyClient(client);
                    PROXY_HTTP_CLIENTS.put(key, entry);
                    retireOverflowingProxyClients(entry, toStop);
                }
                entry.lastUsedNanos = now;
                entry.leases++;
                return entry;
            }
        } finally {
            toStop.forEach(HttpUtil::stopQuietly);
        }
    }

    private static void releaseProxyClient(ProxyClient entry) {
        boolean stop = false;
        synchronized (PROXY_LOCK) {
            entry.leases--;
            entry.lastUsedNanos = System.nanoTime();
            stop = entry.retired && entry.leases == 0;
        }
        if (stop) {
            stopQuietly(entry.client);
        }
    }

    /**
     * Retires all clients that were not used for a long time. Must be called while holding {@link #PROXY_LOCK}.
     */
    private static void retireIdleProxyClients(long now, List<HttpClient> toStop) {
        final Iterator<ProxyClient> it = PROXY_HTTP_CLIENTS.values().iterator();
        while (it.hasNext()) {
            final ProxyClient entry = it.next();
            if (entry.leases == 0 && now - entry.lastUsedNanos > PROXY_HTTP_CLIENT_IDLE_NANOS) {
                it.remove();
                retire(entry, toStop);
            }
        }
    }

    /**
     * Retires the least recently used clients while there are too many. Must be called while holding
     * {@link #PROXY_LOCK}.
     */
    private static void retireOverflowingProxyClients(ProxyClient keep, List<HttpClient> toStop) {
        final Iterator<ProxyClient> it = PROXY_HTTP_CLIENTS.values().iterator();
        while (PROXY_HTTP_CLIENTS.size() > MAX_PROXY_HTTP_CLIENTS && it.hasNext()) {
            final ProxyClient entry = it.next();
            if (entry != keep) {
                it.remove();
                retire(entry, toStop);
            }
        }
    }

    /**
     * Marks the client as retired. It is stopped right away if no request uses it, otherwise by the last request that
     * releases it. Must be called while holding {@link #PROXY_LOCK}.
     */
    private static void retire(ProxyClient entry, List<HttpClient> toStop) {
        entry.retired = true;
        if (entry.leases == 0) {
            toStop.add(entry.client);
        }
    }

    private static void retireAllProxyHttpClients() {
        final List<HttpClient> toStop = new ArrayList<>();
        synchronized (PROXY_LOCK) {
            PROXY_HTTP_CLIENTS.values().forEach(entry -> retire(entry, toStop));
            PROXY_HTTP_CLIENTS.clear();
        }
        toStop.forEach(HttpUtil::stopQuietly);
    }

    private static void stopQuietly(HttpClient client) {
        try {
            client.stop();
        } catch (Exception e) {
            LOGGER.debug("Error while stopping proxy http client", e);
        }
    }

    /**
     * Load proxy parameters in global variables proxyHost, proxyPort, proxyUser, proxyPassword and nonProxyHosts
     */
    private static ProxyParams prepareProxyParams() {
        final ProxyParams proxyParams = new ProxyParams();
        String proxySet = System.getProperty("http.proxySet");
        if ("true".equalsIgnoreCase(proxySet)) {
            proxyParams.proxyHost = System.getProperty("http.proxyHost");
            String proxyPortString = System.getProperty("http.proxyPort");
            if (proxyPortString != null && !proxyPortString.isBlank()) {
                try {
                    proxyParams.proxyPort = Integer.parseInt(proxyPortString);
                } catch (NumberFormatException e) {
                    LOGGER.warn("'{}' is not a valid proxy port - using default port ({}) instead", proxyPortString,
                            proxyParams.proxyPort);
                }
            }
            proxyParams.proxyUser = System.getProperty("http.proxyUser");
            proxyParams.proxyPassword = System.getProperty("http.proxyPassword");
            proxyParams.nonProxyHosts = System.getProperty("http.nonProxyHosts");
        }
        return proxyParams;
    }

    /**
     * Determines whether the list of <code>nonProxyHosts</code> contains the
     * url host (which is part of the given <code>uri</code> or not.
     *
     * @param uri
     * @param nonProxyHosts
     * @return <code>false</code> if the host of the given <code>uri</code> is contained in
     *         <code>nonProxyHosts</code>-list and <code>true</code> otherwise
     */
    private static boolean shouldUseProxy(URI uri, String nonProxyHosts) {
        if (nonProxyHosts != null && !nonProxyHosts.isBlank()) {
            String givenHost = uri.toString();

            try {
                givenHost = uri.toURL().getHost();
            } catch (IllegalArgumentException | MalformedURLException e) {
                LOGGER.error("the given url {} is malformed", uri.toString());
            }

            String[] hosts = nonProxyHosts.split("\\|");
            for (String host : hosts) {
                if (host.contains("*")) {
                    // the nonProxyHots-pattern allows wildcards '*' which must
                    // be masked to be used with regular expressions
                    String hostRegexp = host.replace(".", "\\.");
                    hostRegexp = hostRegexp.replaceAll("\\*", ".*");
                    if (givenHost.matches(hostRegexp)) {
                        return false;
                    }
                } else {
                    if (givenHost.equals(host)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Factory method to create a {@link HttpMethod}-object according to the given String <code>httpMethodString</code>
     *
     * @param httpMethodString the name of the {@link HttpMethod} to create
     * @throws IllegalArgumentException if <code>httpMethod</code> is none of <code>GET</code>, <code>PUT</code>,
     *             <code>POST</code>, <code>PATCH</code>, or <code>DELETE</code>
     */
    public static HttpMethod createHttpMethod(String httpMethodString) {
        // @formatter:off
        return Optional.ofNullable(HttpMethod.fromString(httpMethodString))
                .filter(m -> m == GET || m == POST || m == PUT || m == PATCH || m == DELETE)
                .orElseThrow(() -> new IllegalArgumentException("Given HTTP Method '" + httpMethodString + "' is unknown"));
        // @formatter:on
    }

    /**
     * Download the image data from a URL.
     *
     * If content type is not found in the headers, the data is scanned to determine the content type.
     *
     * @param url the URL of the image to be downloaded
     * @return a RawType object containing the image, null if the content type could not be found or the content type is
     *         not an image
     */
    public static RawType downloadImage(String url) {
        return downloadImage(url, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Download the image data from a URL.
     *
     * If content type is not found in the headers, the data is scanned to determine the content type.
     *
     * @param url the URL of the image to be downloaded
     * @param timeout the socket timeout in milliseconds to wait for data
     * @return a RawType object containing the image, null if the content type could not be found or the content type is
     *         not an image
     */
    public static RawType downloadImage(String url, int timeout) {
        return downloadImage(url, true, -1, timeout);
    }

    /**
     * Download the image data from a URL.
     *
     * @param url the URL of the image to be downloaded
     * @param scanTypeInContent true to allow the scan of data to determine the content type if not found in the headers
     * @param maxContentLength the maximum data size in bytes to trigger the download; any negative value to ignore the
     *            data size
     * @return a RawType object containing the image, null if the content type could not be found or the content type is
     *         not an image or the data size is too big
     */
    public static RawType downloadImage(String url, boolean scanTypeInContent, long maxContentLength) {
        return downloadImage(url, scanTypeInContent, maxContentLength, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Download the image data from a URL.
     *
     * @param url the URL of the image to be downloaded
     * @param scanTypeInContent true to allow the scan of data to determine the content type if not found in the headers
     * @param maxContentLength the maximum data size in bytes to trigger the download; any negative value to ignore the
     *            data size
     * @param timeout the socket timeout in milliseconds to wait for data
     * @return a RawType object containing the image, null if the content type could not be found or the content type is
     *         not an image or the data size is too big
     */
    public static RawType downloadImage(String url, boolean scanTypeInContent, long maxContentLength, int timeout) {
        final ProxyParams proxyParams = prepareProxyParams();

        return downloadImage(url, scanTypeInContent, maxContentLength, timeout, proxyParams.proxyHost,
                proxyParams.proxyPort, proxyParams.proxyUser, proxyParams.proxyPassword, proxyParams.nonProxyHosts);
    }

    /**
     * Download the image data from a URL.
     *
     * @param url the URL of the image to be downloaded
     * @param scanTypeInContent true to allow the scan of data to determine the content type if not found in the headers
     * @param maxContentLength the maximum data size in bytes to trigger the download; any negative value to ignore the
     *            data size
     * @param timeout the socket timeout in milliseconds to wait for data
     * @param proxyHost the hostname of the proxy
     * @param proxyPort the port of the proxy
     * @param proxyUser the username to authenticate with the proxy
     * @param proxyPassword the password to authenticate with the proxy
     * @param nonProxyHosts the hosts that won't be routed through the proxy
     * @return a RawType object containing the image, null if the content type could not be found or the content type is
     *         not an image or the data size is too big
     */
    public static RawType downloadImage(String url, boolean scanTypeInContent, long maxContentLength, int timeout,
            String proxyHost, Integer proxyPort, String proxyUser, String proxyPassword, String nonProxyHosts) {
        return downloadData(url, "image/.*", scanTypeInContent, maxContentLength, timeout, proxyHost, proxyPort,
                proxyUser, proxyPassword, nonProxyHosts);
    }

    /**
     * Download the data from a URL.
     *
     * @param url the URL of the data to be downloaded
     * @param contentTypeRegex the REGEX the content type must match; null to ignore the content type
     * @param scanTypeInContent true to allow the scan of data to determine the content type if not found in the headers
     * @param maxContentLength the maximum data size in bytes to trigger the download; any negative value to ignore the
     *            data size
     * @return a RawType object containing the downloaded data, null if the content type does not match the expected
     *         type or the data size is too big
     */
    public static RawType downloadData(String url, String contentTypeRegex, boolean scanTypeInContent,
            long maxContentLength) {
        return downloadData(url, contentTypeRegex, scanTypeInContent, maxContentLength, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Download the data from a URL.
     *
     * @param url the URL of the data to be downloaded
     * @param contentTypeRegex the REGEX the content type must match; null to ignore the content type
     * @param scanTypeInContent true to allow the scan of data to determine the content type if not found in the headers
     * @param maxContentLength the maximum data size in bytes to trigger the download; any negative value to ignore the
     *            data size
     * @param timeout the socket timeout in milliseconds to wait for data
     * @return a RawType object containing the downloaded data, null if the content type does not match the expected
     *         type or the data size is too big
     */
    public static RawType downloadData(String url, String contentTypeRegex, boolean scanTypeInContent,
            long maxContentLength, int timeout) {
        final ProxyParams proxyParams = prepareProxyParams();

        return downloadData(url, contentTypeRegex, scanTypeInContent, maxContentLength, timeout, proxyParams.proxyHost,
                proxyParams.proxyPort, proxyParams.proxyUser, proxyParams.proxyPassword, proxyParams.nonProxyHosts);
    }

    /**
     * Download the data from a URL.
     *
     * @param url the URL of the data to be downloaded
     * @param contentTypeRegex the REGEX the content type must match; null to ignore the content type
     * @param scanTypeInContent true to allow the scan of data to determine the content type if not found in the headers
     * @param maxContentLength the maximum data size in bytes to trigger the download; any negative value to ignore the
     *            data size
     * @param timeout the socket timeout in milliseconds to wait for data
     * @param proxyHost the hostname of the proxy
     * @param proxyPort the port of the proxy
     * @param proxyUser the username to authenticate with the proxy
     * @param proxyPassword the password to authenticate with the proxy
     * @param nonProxyHosts the hosts that won't be routed through the proxy
     * @return a RawType object containing the downloaded data, null if the content type does not match the expected
     *         type or the data size is too big
     */
    public static RawType downloadData(String url, String contentTypeRegex, boolean scanTypeInContent,
            long maxContentLength, int timeout, String proxyHost, Integer proxyPort, String proxyUser,
            String proxyPassword, String nonProxyHosts) {
        RawType rawData = null;
        try {
            ContentResponse response = executeUrlAndGetResponse("GET", url, null, null, null, timeout, proxyHost,
                    proxyPort, proxyUser, proxyPassword, nonProxyHosts);
            byte[] data = response.getContent();
            if (data == null) {
                data = new byte[0];
            }
            long length = data.length;
            String mediaType = response.getMediaType();
            LOGGER.debug("Media download response: status {} content length {} media type {} (URL {})",
                    response.getStatus(), length, mediaType, url);

            if (response.getStatus() != HttpStatus.OK_200 || length == 0) {
                LOGGER.debug("Media download failed: unexpected return code {} (URL {})", response.getStatus(), url);
                return null;
            }

            if (maxContentLength >= 0 && length > maxContentLength) {
                LOGGER.debug("Media download aborted: content length {} too big (URL {})", length, url);
                return null;
            }

            String contentType = mediaType;
            if (contentTypeRegex != null) {
                if ((contentType == null || contentType.isEmpty()) && scanTypeInContent) {
                    // We try to get the type from the data
                    contentType = guessContentTypeFromData(data);
                    LOGGER.debug("Media download: content type from data: {} (URL {})", contentType, url);
                }
                if (contentType != null && contentType.isEmpty()) {
                    contentType = null;
                }
                if (contentType == null) {
                    LOGGER.debug("Media download aborted: unknown content type (URL {})", url);
                    return null;
                } else if (!contentType.matches(contentTypeRegex)) {
                    LOGGER.debug("Media download aborted: unexpected content type \"{}\" (URL {})", contentType, url);
                    return null;
                }
            } else if (contentType == null || contentType.isEmpty()) {
                contentType = RawType.DEFAULT_MIME_TYPE;
            }

            rawData = new RawType(data, contentType);

            LOGGER.debug("Media downloaded: size {} type {} (URL {})", rawData.getBytes().length, rawData.getMimeType(),
                    url);
        } catch (IOException e) {
            LOGGER.debug("Media download failed (URL {}) : {}", url, e.getMessage());
        }
        return rawData;
    }

    /**
     * Determine the content type from the data.
     *
     * @param data the data as a buffer of bytes
     * @return the MIME type of the content, null if the content type could not be found
     */
    public static String guessContentTypeFromData(byte[] data) {
        String contentType = null;

        // URLConnection.guessContentTypeFromStream(input) is not sufficient to detect all JPEG files
        if (isJpeg(data)) {
            return "image/jpeg";
        }

        try (final ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            try {
                contentType = URLConnection.guessContentTypeFromStream(input);
                if (contentType != null && contentType.isEmpty()) {
                    contentType = null;
                }
            } catch (final IOException e) {
                LOGGER.debug("Failed to determine content type: {}", e.getMessage());
            }
        } catch (final IOException ex) {
            // Error on closing input stream -- nothing we can do here.
        }
        return contentType;
    }

    /**
     * Check whether the content data is a JPEG file checking file start and end bytes.
     * {@link URLConnection#guessContentTypeFromStream(InputStream)} is wrong for some JPEG files.
     *
     * @see <a href="https://en.wikipedia.org/wiki/JPEG#Syntax_and_structure">JPEG Syntax and structure</a>
     *
     * @param data the data as buffer of bytes
     * @return <code>true</code> if the content is a JPEG file, <code>false</code> otherwise
     */
    private static boolean isJpeg(byte[] data) {
        return (data.length >= 2 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8
                && data[data.length - 2] == (byte) 0xFF && data[data.length - 1] == (byte) 0xD9);
    }

    @Reference
    protected void setHttpClientFactory(final HttpClientFactory httpClientFactory) {
        synchronized (PROXY_LOCK) {
            HttpUtil.httpClientFactory = httpClientFactory;
        }
    }

    protected void unsetHttpClientFactory(final HttpClientFactory httpClientFactory) {
        synchronized (PROXY_LOCK) {
            if (HttpUtil.httpClientFactory == httpClientFactory) {
                HttpUtil.httpClientFactory = null;
            }
        }
        retireAllProxyHttpClients();
    }
}
