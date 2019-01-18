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
package org.eclipse.smarthome.io.net.http;

import static org.eclipse.jetty.http.HttpMethod.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.smarthome.core.library.types.RawType;
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

    private static HttpClientFactory httpClientFactory;

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
        ContentResponse response = executeUrlAndGetReponse(httpMethod, url, httpHeaders, content, contentType, timeout,
                proxyHost, proxyPort, proxyUser, proxyPassword, nonProxyHosts);
        String encoding = response.getEncoding() != null ? response.getEncoding().replaceAll("\"", "").trim() : "UTF-8";
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
    private static ContentResponse executeUrlAndGetReponse(String httpMethod, String url, Properties httpHeaders,
            InputStream content, String contentType, int timeout, String proxyHost, Integer proxyPort, String proxyUser,
            String proxyPassword, String nonProxyHosts) throws IOException {
        // Referenced http client factory not available
        if (httpClientFactory == null) {
            throw new IllegalStateException("Http client factory not available");
        }
        // Get shared http client from factory "on-demand"
        final HttpClient httpClient = httpClientFactory.getCommonHttpClient();

        HttpProxy proxy = null;
        // Only configure a proxy if a host is provided
        if (StringUtils.isNotBlank(proxyHost) && proxyPort != null && shouldUseProxy(url, nonProxyHosts)) {
            AuthenticationStore authStore = httpClient.getAuthenticationStore();
            ProxyConfiguration proxyConfig = httpClient.getProxyConfiguration();
            List<Proxy> proxies = proxyConfig.getProxies();

            proxy = new HttpProxy(proxyHost, proxyPort);
            proxies.add(proxy);

            authStore.addAuthentication(
                    new BasicAuthentication(proxy.getURI(), Authentication.ANY_REALM, proxyUser, proxyPassword));
        }

        final HttpMethod method = HttpUtil.createHttpMethod(httpMethod);

        final Request request = httpClient.newRequest(url).method(method).timeout(timeout, TimeUnit.MILLISECONDS);

        if (httpHeaders != null) {
            for (String httpHeaderKey : httpHeaders.stringPropertyNames()) {
                request.header(httpHeaderKey, httpHeaders.getProperty(httpHeaderKey));
            }
        }

        // add basic auth header, if url contains user info
        try {
            URI uri = new URI(url);
            if (uri.getUserInfo() != null) {
                String[] userInfo = uri.getUserInfo().split(":");

                String user = userInfo[0];
                String password = userInfo[1];

                String basicAuthentication = "Basic " + B64Code.encode(user + ":" + password, StringUtil.__ISO_8859_1);
                request.header(HttpHeader.AUTHORIZATION, basicAuthentication);
            }
        } catch (URISyntaxException e) {
            LOGGER.debug("String {} can not be parsed as URI reference", url);
        }

        // add content if a valid method is given ...
        if (content != null && (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT))) {
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
        } finally {
            if (proxy != null) {
                // Remove the proxy, that has been added for this request
                httpClient.getProxyConfiguration().getProxies().remove(proxy);
            }
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
            if (StringUtils.isNotBlank(proxyPortString)) {
                try {
                    proxyParams.proxyPort = Integer.valueOf(proxyPortString);
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
     * host (which is part of the given <code>urlString</code> or not.
     *
     * @param urlString
     * @param nonProxyHosts
     * @return <code>false</code> if the host of the given <code>urlString</code> is contained in
     *         <code>nonProxyHosts</code>-list and <code>true</code> otherwise
     */
    private static boolean shouldUseProxy(String urlString, String nonProxyHosts) {
        if (StringUtils.isNotBlank(nonProxyHosts)) {
            String givenHost = urlString;

            try {
                URL url = new URL(urlString);
                givenHost = url.getHost();
            } catch (MalformedURLException e) {
                LOGGER.error("the given url {} is malformed", urlString);
            }

            String[] hosts = nonProxyHosts.split("\\|");
            for (String host : hosts) {
                if (host.contains("*")) {
                    // the nonProxyHots-pattern allows wildcards '*' which must
                    // be masked to be used with regular expressions
                    String hostRegexp = host.replaceAll("\\.", "\\\\.");
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
     *             <code>POST</POST> or <code>DELETE</code>
     */
    public static HttpMethod createHttpMethod(String httpMethodString) {
        // @formatter:off
        return Optional.ofNullable(HttpMethod.fromString(httpMethodString))
                .filter(m -> m == GET || m == POST || m == PUT || m == DELETE)
                .orElseThrow(() -> new IllegalArgumentException("Given HTTP Method '" + httpMethodString + "' is unknown"));
        // @formatter:on
    }

    /**
     * Download the image data from an URL.
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
     * Download the image data from an URL.
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
     * Download the image data from an URL.
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
     * Download the image data from an URL.
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
        return downloadData(url, "image/.*", scanTypeInContent, maxContentLength, timeout);
    }

    /**
     * Download the data from an URL.
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
     * Download the data from an URL.
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

        RawType rawData = null;
        try {
            ContentResponse response = executeUrlAndGetReponse("GET", url, null, null, null, timeout,
                    proxyParams.proxyHost, proxyParams.proxyPort, proxyParams.proxyUser, proxyParams.proxyPassword,
                    proxyParams.nonProxyHosts);
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
     * @see https://en.wikipedia.org/wiki/JPEG#Syntax_and_structure
     * @param data the data as buffer of bytes
     * @return <code>true</code> if the content is a JPEG file, <code>false</code> otherwise
     */
    private static boolean isJpeg(byte[] data) {
        return (data.length >= 2 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8
                && data[data.length - 2] == (byte) 0xFF && data[data.length - 1] == (byte) 0xD9);
    }

    @Reference
    protected void setHttpClientFactory(final HttpClientFactory httpClientFactory) {
        HttpUtil.httpClientFactory = httpClientFactory;
    }

    protected void unsetHttpClientFactory(final HttpClientFactory httpClientFactory) {
        HttpUtil.httpClientFactory = null;
    }

}
