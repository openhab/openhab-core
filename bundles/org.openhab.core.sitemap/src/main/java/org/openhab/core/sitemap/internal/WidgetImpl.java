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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Widget;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class WidgetImpl implements Widget {

    private @Nullable Parent parent;

    private @Nullable String item;
    private @Nullable String label;

    private @Nullable String icon;
    private List<Rule> iconRules = new CopyOnWriteArrayList<>();
    private @Nullable Boolean staticIcon;

    private List<Rule> labelColor = new CopyOnWriteArrayList<>();
    private List<Rule> valueColor = new CopyOnWriteArrayList<>();
    private List<Rule> iconColor = new CopyOnWriteArrayList<>();
    private List<Rule> visibility = new CopyOnWriteArrayList<>();

    public WidgetImpl() {
    }

    public WidgetImpl(Parent parent) {
        this.parent = parent;
    }

    @Override
    public @Nullable Parent getParent() {
        return parent;
    }

    @Override
    public void setParent(Parent parent) {
        this.parent = parent;
    }

    @Override
    public @Nullable String getItem() {
        return item;
    }

    @Override
    public void setItem(@Nullable String item) {
        this.item = item;
    }

    @Override
    public @Nullable String getLabel() {
        return label;
    }

    @Override
    public void setLabel(@Nullable String label) {
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

    @Override
    public List<Rule> getIconRules() {
        return iconRules;
    }

    @Override
    public void setIconRules(List<Rule> iconRules) {
        this.iconRules = new CopyOnWriteArrayList<>(iconRules);
    }

    @Override
    public boolean isStaticIcon() {
        return staticIcon == null ? false : staticIcon;
    }

    @Override
    public void setStaticIcon(@Nullable Boolean staticIcon) {
        this.staticIcon = staticIcon;
    }

    @Override
    public List<Rule> getLabelColor() {
        return labelColor;
    }

    @Override
    public void setLabelColor(List<Rule> labelColor) {
        this.labelColor = new CopyOnWriteArrayList<>(labelColor);
    }

    @Override
    public List<Rule> getValueColor() {
        return valueColor;
    }

    @Override
    public void setValueColor(List<Rule> valueColor) {
        this.valueColor = new CopyOnWriteArrayList<>(valueColor);
    }

    @Override
    public List<Rule> getIconColor() {
        return iconColor;
    }

    @Override
    public void setIconColor(List<Rule> iconColor) {
        this.iconColor = new CopyOnWriteArrayList<>(iconColor);
    }

    @Override
    public List<Rule> getVisibility() {
        return visibility;
    }

    @Override
    public void setVisibility(List<Rule> visibility) {
        this.visibility = new CopyOnWriteArrayList<>(visibility);
    }

    @Override
    public String getWidgetType() {
        return this.getClass().getInterfaces()[0].getSimpleName();
    }
}
