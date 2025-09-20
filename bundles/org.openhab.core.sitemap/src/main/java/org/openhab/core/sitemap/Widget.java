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
package org.openhab.core.sitemap;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Widget {

    /**
     * Get the direct parent {@link Widget} or {@link Sitemap}.
     *
     * @return parent
     */
    @Nullable
    Parent getParent();

    /**
     * Sets the parent {@link Widget} or {@link Sitemap}.
     * Widgets in a sitemap should always have a parent. Implementations of {@link Widget} should have a constructor
     * with {@link Parent} parameter to make building a sitemap easier.
     *
     * @param parent
     */
    void setParent(Parent parent);

    /**
     * Gets the item name for the widget. For specific widget type, the item is required and for these widgets, this
     * method should not return null.
     *
     * @return item, or null if no item defined for the widget
     */
    @Nullable
    String getItem();

    /**
     * Sets the widget item.
     *
     * @param item
     */
    void setItem(@Nullable String item);

    /**
     * Get widget label.
     *
     * @return label
     */
    @Nullable
    String getLabel();

    /**
     * Set widget label.
     *
     * @param label
     */
    void setLabel(@Nullable String label);

    /**
     * Get widget icon.
     *
     * @return icon
     */
    @Nullable
    String getIcon();

    /**
     * Set widget icon.
     *
     * @param icon
     */
    void setIcon(@Nullable String icon);

    /**
     * Get the widget icon rules. This method should return a modifiable list, allowing updates to the icon rules.
     *
     * @return icon rules
     */
    List<Rule> getIconRules();

    /**
     * Replace the widget icon rules with a new list of icon rules.
     *
     * @param iconRules
     */
    void setIconRules(List<Rule> iconRules);

    /**
     * True if the widget icon is static, false otherwise.
     *
     * @return static icon
     */
    boolean isStaticIcon();

    /**
     * Set to true if the widget icon is static.
     *
     * @param staticIcon
     */
    void setStaticIcon(@Nullable Boolean staticIcon);

    /**
     * Get the widget label color rules. This method should return a modifiable list, allowing updates to the label
     * color rules.
     *
     * @return label color rules
     */
    List<Rule> getLabelColor();

    /**
     * Replace the widget label color rules with a new list of label color rules.
     *
     * @param labelColorRules
     */
    void setLabelColor(List<Rule> labelColorRules);

    /**
     * Get the widget value color rules. This method should return a modifiable list, allowing updates to the value
     * color rules.
     *
     * @return value color rules
     */
    List<Rule> getValueColor();

    /**
     * Replace the widget value color rules with a new list of value color rules.
     *
     * @param valueColorRules
     */
    void setValueColor(List<Rule> valueColorRules);

    /**
     * Get the widget icon color rules. This method should return a modifiable list, allowing updates to the icon
     * color rules.
     *
     * @return icon color rules
     */
    List<Rule> getIconColor();

    /**
     * Replace the widget icon color rules with a new list of icon color rules.
     *
     * @param iconColorRules
     */
    void setIconColor(List<Rule> iconColorRules);

    /**
     * Get the widget visibility rules. This method should return a modifiable list, allowing updates to the visibility
     * rules.
     *
     * @return visibility rules
     */
    List<Rule> getVisibility();

    /**
     * Replace the widget visibility rules with a new list of visibility rules.
     *
     * @param visibilityRules
     */
    void setVisibility(List<Rule> visibilityRules);

    /**
     * Get type of widget.
     *
     * @return widget type
     */
    String getWidgetType();
}
