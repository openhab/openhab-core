/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.rest.internal.resources.beans;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.io.rest.RESTConstants;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is a java bean that is used to define the root entry
 * page of the REST interface.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Add runtime info
 */
@NonNullByDefault
public class RootBean {

    public final String version = RESTConstants.API_VERSION;

    public final String locale;

    @Schema(allowableValues = { "SI", "US" })
    public final String measurementSystem;

    public final String timezone;

    public final RuntimeInfo runtimeInfo = new RuntimeInfo();

    public final List<Links> links = new ArrayList<>();

    public RootBean(LocaleProvider localeProvider, UnitProvider unitProvider, TimeZoneProvider timeZoneProvider) {
        this.locale = localeProvider.getLocale().toString();
        this.measurementSystem = unitProvider.getMeasurementSystem().getName();
        this.timezone = timeZoneProvider.getTimeZone().toString();
    }

    public static class RuntimeInfo {
        public final String version = OpenHAB.getVersion();
        public final String buildString = OpenHAB.buildString();
    }

    public static class Links {
        public Links(String type, String url) {
            this.type = type;
            this.url = url;
        }

        public String type;
        public String url;
    }
}
