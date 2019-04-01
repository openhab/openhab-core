/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.io.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.auth.Credentials;
import org.eclipse.smarthome.core.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some common methods to be used in both HTTP-In-Binding and HTTP-Out-Binding
 *
 * @author Thomas.Eichstaedt-Engelen
 * @author Kai Kreuzer
 * @since 0.6.0
 */
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    /** {@link Pattern} which matches the credentials out of an URL */
    private static final Pattern URL_CREDENTIALS_PATTERN = Pattern.compile("http://(.*?):(.*?)@.*");

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>.
     * Furthermore the <code>http.proxyXXX</code> System variables are read and
     * set into the {@link HttpClient}.
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute (in milliseconds)
     * @param timeout the socket timeout to wait for data
     *
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    public static String executeUrl(String httpMethod, String url, int timeout) {
        return executeUrl(httpMethod, url, null, null, timeout);
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>.
     * Furthermore the <code>http.proxyXXX</code> System variables are read and
     * set into the {@link HttpClient}.
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute (in milliseconds)
     * @param content the content to be send to the given <code>url</code> or
     *            <code>null</code> if no content should be send.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout to wait for data
     *
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    public static String executeUrl(String httpMethod, String url, InputStream content, String contentType,
            int timeout) {

        return executeUrl(httpMethod, url, null, content, contentType, timeout);
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>.
     * Furthermore the <code>http.proxyXXX</code> System variables are read and
     * set into the {@link HttpClient}.
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute (in milliseconds)
     * @param httpHeaders optional http request headers which has to be sent within request
     * @param content the content to be send to the given <code>url</code> or
     *            <code>null</code> if no content should be send.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout to wait for data
     *
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    public static String executeUrl(String httpMethod, String url, Properties httpHeaders, InputStream content,
            String contentType, int timeout) {
        try {
            return org.eclipse.smarthome.io.net.http.HttpUtil.executeUrl(httpMethod, url, content, contentType,
                    timeout);
        } catch (IOException ioe) {
            logger.error("Fatal transport error: {}", ioe.toString());
        }
        return null;
    }

    /**
     * Executes the given <code>url</code> with the given <code>httpMethod</code>
     *
     * @param httpMethod the HTTP method to use
     * @param url the url to execute (in milliseconds)
     * @param httpHeaders optional HTTP headers which has to be set on request
     * @param content the content to be send to the given <code>url</code> or
     *            <code>null</code> if no content should be send.
     * @param contentType the content type of the given <code>content</code>
     * @param timeout the socket timeout to wait for data
     * @param proxyHost the hostname of the proxy
     * @param proxyPort the port of the proxy
     * @param proxyUser the username to authenticate with the proxy
     * @param proxyPassword the password to authenticate with the proxy
     * @param nonProxyHosts the hosts that won't be routed through the proxy
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    public static String executeUrl(String httpMethod, String url, Properties httpHeaders, InputStream content,
            String contentType, int timeout, String proxyHost, Integer proxyPort, String proxyUser,
            String proxyPassword, String nonProxyHosts) {
        try {
            return org.eclipse.smarthome.io.net.http.HttpUtil.executeUrl(httpMethod, url, httpHeaders, content,
                    contentType, timeout, proxyHost, proxyPort, proxyUser, proxyPassword, nonProxyHosts);
        } catch (IOException ioe) {
            logger.error("Fatal transport error: {}", ioe.toString());
        }
        return null;
    }

    /**
     * Extracts username and password from the given <code>url</code>. A valid
     * url to extract {@link Credentials} from looks like:
     *
     * <pre>
     * http://username:password@www.domain.org
     * </pre>
     *
     * @param url the URL to extract {@link Credentials} from
     *
     * @return the exracted Credentials or <code>null</code> if the given
     *         <code>url</code> does not contain credentials
     */
    protected static Credentials extractCredentials(String url) {

        Matcher matcher = URL_CREDENTIALS_PATTERN.matcher(url);

        if (matcher.matches()) {

            matcher.reset();

            String username = "";
            String password = "";

            while (matcher.find()) {
                username = matcher.group(1);
                password = matcher.group(2);
            }

            Credentials credentials = new UsernamePasswordCredentials(username, password);
            return credentials;
        }

        return null;
    }

}
