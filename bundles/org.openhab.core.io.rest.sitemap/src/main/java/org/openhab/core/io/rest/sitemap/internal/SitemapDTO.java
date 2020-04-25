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
package org.openhab.core.io.rest.sitemap.internal;

/**
 * This is a data transfer object that is used to serialize sitemaps.
 * 
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 */
public class SitemapDTO {

    public String name;
    public String icon;
    public String label;

    public String link;

    public PageDTO homepage;
}
