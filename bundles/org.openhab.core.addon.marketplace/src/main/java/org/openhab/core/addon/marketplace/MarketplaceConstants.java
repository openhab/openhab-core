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
package org.openhab.core.addon.marketplace;

/**
 * This class contains constants used in marketplace add-on services
 *
 * @author Jan N. Klug - Initial contribution
 */
public class MarketplaceConstants {
    public static final String JAR_CONTENT_TYPE = "application/vnd.openhab.bundle";
    public static final String KAR_CONTENT_TYPE = "application/vnd.openhab.feature;type=karfile";
    public static final String RULETEMPLATES_CONTENT_TYPE = "application/vnd.openhab.ruletemplate";
    public static final String UIWIDGETS_CONTENT_TYPE = "application/vnd.openhab.uicomponent;type=widget";
    public static final String BLOCKLIBRARIES_CONTENT_TYPE = "application/vnd.openhab.uicomponent;type=blocks";
    public static final String TRANSFORMATIONS_CONTENT_TYPE = "application/vnd.openhab.transformation";
    public static final String JAR_DOWNLOAD_URL_PROPERTY = "jar_download_url";
    public static final String KAR_DOWNLOAD_URL_PROPERTY = "kar_download_url";
    public static final String JSON_DOWNLOAD_URL_PROPERTY = "json_download_url";
    public static final String YAML_DOWNLOAD_URL_PROPERTY = "yaml_download_url";
}
