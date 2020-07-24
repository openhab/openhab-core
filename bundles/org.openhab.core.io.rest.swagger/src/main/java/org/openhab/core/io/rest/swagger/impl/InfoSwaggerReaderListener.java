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
package org.openhab.core.io.rest.swagger.impl;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;

import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.Swagger;

/**
 * This class adds information about the REST API to the Swagger object.
 *
 * @author Yannick Schaus - initial contribution
 */
@Component
@NonNullByDefault
public class InfoSwaggerReaderListener implements ReaderListener {
    public static final String API_TITLE = "openHAB REST API";
    public static final String CONTACT_NAME = "openHAB";
    public static final String CONTACT_URL = "https://www.openhab.org/docs/";

    @Override
    public void beforeScan(@NonNullByDefault({}) Reader reader, @NonNullByDefault({}) Swagger swagger) {
        Info info = new Info();
        info.setTitle(API_TITLE);
        info.setVersion(RESTConstants.API_VERSION);
        Contact contact = new Contact();
        contact.setName(CONTACT_NAME);
        contact.setUrl(CONTACT_URL);
        info.setContact(contact);
        swagger.setInfo(info);
    }

    @Override
    public void afterScan(@NonNullByDefault({}) Reader reader, @NonNullByDefault({}) Swagger swagger) {
    }
}
