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
import org.openhab.core.sitemap.Button;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class ButtonImpl extends NonLinkableWidgetImpl implements Button {

    private int row;
    private int column;
    private @Nullable Boolean stateless;
    private String cmd = "";
    private @Nullable String releaseCmd;

    public ButtonImpl() {
        super();
    }

    public ButtonImpl(Parent parent) {
        super(parent);
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public boolean isStateless() {
        return stateless != null ? stateless : false;
    }

    @Override
    public void setStateless(@Nullable Boolean stateless) {
        this.stateless = stateless;
    }

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
}
