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

import java.util.ArrayList;
import java.util.List;

/**
 * This is a data transfer object that is used to serialize page content.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class PageDTO {

    public String id;

    public String title;
    public String icon;
    public String link;
    public PageDTO parent;
    public boolean leaf;
    public boolean timeout;

    public List<WidgetDTO> widgets = new ArrayList<>();

    public PageDTO() {
    }
}
