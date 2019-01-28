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
package org.eclipse.smarthome.model.script.actions;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods that can be used in automation rules
 * for sending HTTP requests
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Jan N. Klug - add timeout methods
 *
 */
public class HTTP {

    /** Constant which represents the content type <code>application/json</code> */
    public final static String CONTENT_TYPE_JSON = "application/json";

    private static Logger logger = LoggerFactory.getLogger(HTTP.class);

    /**
     * Send out a GET-HTTP request. Errors will be logged, success returns response
     *
     * @param url the URL to be used for the GET request.
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpGetRequest(String url) {
        return sendHttpGetRequest(url, 5000);
    }

    /**
     * Send out a GET-HTTP request. Errors will be logged, success returns response
     *
     * @param url the URL to be used for the GET request.
     * @param timeout timeout in ms
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpGetRequest(String url, int timeout) {
        String response = null;
        try {
            return HttpUtil.executeUrl("GET", url, timeout);
        } catch (IOException e) {
            logger.error("Fatal transport error: {}", e.getMessage());
        }
        return response;
    }

    /**
     * Send out a PUT-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the PUT request.
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPutRequest(String url) {
        return sendHttpPutRequest(url, 1000);
    }

    /**
     * Send out a PUT-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the PUT request.
     * @param timeout timeout in ms
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPutRequest(String url, int timeout) {
        String response = null;
        try {
            response = HttpUtil.executeUrl("PUT", url, timeout);
        } catch (IOException e) {
            logger.error("Fatal transport error: {}", e.getMessage());
        }
        return response;
    }

    /**
     * Send out a PUT-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the PUT request.
     * @param contentType the content type of the given <code>content</code>
     * @param content the content to be send to the given <code>url</code> or <code>null</code> if no content should be
     *            send.
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPutRequest(String url, String contentType, String content) {
        return sendHttpPutRequest(url, contentType, content, 1000);
    }

    /**
     * Send out a PUT-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the PUT request.
     * @param contentType the content type of the given <code>content</code>
     * @param content the content to be send to the given <code>url</code> or <code>null</code> if no content should be
     *            send.
     * @param timeout timeout in ms
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPutRequest(String url, String contentType, String content, int timeout) {
        String response = null;
        try {
            response = HttpUtil.executeUrl("PUT", url, IOUtils.toInputStream(content), contentType, timeout);
        } catch (IOException e) {
            logger.error("Fatal transport error: {}", e.getMessage());
        }
        return response;
    }

    /**
     * Send out a POST-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the POST request.
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPostRequest(String url) {
        return sendHttpPostRequest(url, 1000);
    }

    /**
     * Send out a POST-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the POST request.
     * @param timeout timeout in ms
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPostRequest(String url, int timeout) {
        String response = null;
        try {
            response = HttpUtil.executeUrl("POST", url, timeout);
        } catch (IOException e) {
            logger.error("Fatal transport error: {}", e.getMessage());
        }
        return response;
    }

    /**
     * Send out a POST-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the POST request.
     * @param contentType the content type of the given <code>content</code>
     * @param content the content to be send to the given <code>url</code> or <code>null</code> if no content should be
     *            send.
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPostRequest(String url, String contentType, String content) {
        return sendHttpPostRequest(url, contentType, content, 1000);
    }

    /**
     * Send out a POST-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the POST request.
     * @param contentType the content type of the given <code>content</code>
     * @param content the content to be send to the given <code>url</code> or <code>null</code> if no content should be
     *            send.
     * @param timeout timeout in ms
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpPostRequest(String url, String contentType, String content, int timeout) {
        String response = null;
        try {
            response = HttpUtil.executeUrl("POST", url, IOUtils.toInputStream(content), contentType, timeout);
        } catch (IOException e) {
            logger.error("Fatal transport error: {}", e.getMessage());
        }
        return response;
    }

    /**
     * Send out a DELETE-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the DELETE request.
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpDeleteRequest(String url) {
        return sendHttpDeleteRequest(url, 1000);
    }

    /**
     * Send out a DELETE-HTTP request. Errors will be logged, returned values just ignored.
     *
     * @param url the URL to be used for the DELETE request.
     * @param timeout timeout in ms
     * @return the response body or <code>NULL</code> when the request went wrong
     */
    static public String sendHttpDeleteRequest(String url, int timeout) {
        String response = null;
        try {
            response = HttpUtil.executeUrl("DELETE", url, timeout);
        } catch (IOException e) {
            logger.error("Fatal transport error: {}", e.getMessage());
        }
        return response;
    }

}
