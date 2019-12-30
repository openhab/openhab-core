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
package org.openhab.core.auth.oauth2client.internal;

/**
 * Just a place to store all the important, reused keywords.
 *
 * @author Gary Tse - Initial contribution
 */
public interface Keyword {

    String CLIENT_ID = "client_id";
    String CLIENT_SECRET = "client_secret";

    String GRANT_TYPE = "grant_type";
    String USERNAME = "username";
    String PASSWORD = "password";
    String CLIENT_CREDENTIALS = "client_credentials";
    String AUTHORIZATION_CODE = "authorization_code";

    String SCOPE = "scope";

    String REFRESH_TOKEN = "refresh_token";
    String REDIRECT_URI = "redirect_uri";

    String CODE = "code"; // https://tools.ietf.org/html/rfc6749#section-4.1

    String STATE = "state"; // https://tools.ietf.org/html/rfc6749#section-4.1

}
