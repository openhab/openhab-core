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
package org.openhab.core.sitemap.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Mapping;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class MappingImpl implements Mapping {

    private String cmd = "";
    private @Nullable String releaseCmd;
    private String label = "";
    private @Nullable String icon;

    @Override
    public String getCmd() {
        return cmd;
    }

    @Override
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public @Nullable String getReleaseCmd() {
        return releaseCmd;
    }

    @Override
    public void setReleaseCmd(@Nullable String releaseCmd) {
        this.releaseCmd = releaseCmd;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public @Nullable String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(@Nullable String icon) {
        this.icon = icon;
    }
}
