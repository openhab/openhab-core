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
package org.openhab.core.ui.internal.components;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Button;
import org.openhab.core.sitemap.ButtonDefinition;
import org.openhab.core.sitemap.Buttongrid;
import org.openhab.core.sitemap.Chart;
import org.openhab.core.sitemap.Colortemperaturepicker;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Default;
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Input;
import org.openhab.core.sitemap.LinkableWidget;
import org.openhab.core.sitemap.Mapping;
import org.openhab.core.sitemap.Mapview;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Selection;
import org.openhab.core.sitemap.Setpoint;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Slider;
import org.openhab.core.sitemap.Switch;
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.Webview;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UIComponentSitemapMapper} is a utility class to map sitemaps into UI Component objects.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class UIComponentSitemapMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIComponentSitemapMapper.class);

    public static RootUIComponent map(Sitemap element) {
        String sitemapName = element.getName();
        sitemapName = sitemapName.startsWith(UIComponentSitemapProvider.SITEMAP_PREFIX)
                ? sitemapName.substring(UIComponentSitemapProvider.SITEMAP_PREFIX.length())
                : sitemapName;
        RootUIComponent sitemapComponent = new RootUIComponent(sitemapName, "Sitemap");
        addConfig(sitemapComponent, "label", element.getLabel());
        addConfig(sitemapComponent, "icon", element.getIcon());

        if (!element.getWidgets().isEmpty()) {
            sitemapComponent.addSlot("widgets");
            element.getWidgets().stream().map(UIComponentSitemapMapper::map)
                    .forEach(component -> sitemapComponent.addComponent("widgets", component));
        }

        return sitemapComponent;
    }

    public static UIComponent map(Widget widget) {
        UIComponent widgetComponent = new UIComponent(widget.getWidgetType());
        addConfig(widgetComponent, "item", widget.getItem());
        addConfig(widgetComponent, "label", widget.getLabel());
        addConfig(widgetComponent, "icon", widget.getIcon());
        if (widget.isStaticIcon()) {
            addConfig(widgetComponent, "staticIcon", true);
        }

        addConfig(widgetComponent, "iconRules", map(widget.getIconRules()));
        addConfig(widgetComponent, "visibility", map(widget.getVisibility()));
        addConfig(widgetComponent, "labelColor", map(widget.getLabelColor()));
        addConfig(widgetComponent, "valueColor", map(widget.getValueColor()));
        addConfig(widgetComponent, "iconColor", map(widget.getIconColor()));

        switch (widget) {
            case Switch switchWidget -> {
                addConfig(widgetComponent, "mappings", map(switchWidget.getMappings()));
            }
            case Buttongrid buttongridWidget -> {
                addConfig(widgetComponent, "buttons", map(buttongridWidget.getButtons()));
            }
            case Button buttonWidget -> {
                addConfig(widgetComponent, "row", buttonWidget.getRow());
                addConfig(widgetComponent, "column", buttonWidget.getColumn());
                addConfig(widgetComponent, "cmd", buttonWidget.getCmd());
                addConfig(widgetComponent, "releaseCmd", buttonWidget.getReleaseCmd());
                if (buttonWidget.isStateless()) {
                    addConfig(widgetComponent, "stateless", true);
                }
            }
            case Selection selectionWidget -> {
                addConfig(widgetComponent, "mappings", map(selectionWidget.getMappings()));
            }
            case Setpoint setpointWidget -> {
                addConfig(widgetComponent, "minValue", setpointWidget.getMinValue());
                addConfig(widgetComponent, "maxValue", setpointWidget.getMaxValue());
                addConfig(widgetComponent, "step", setpointWidget.getStep());
            }
            case Slider sliderWidget -> {
                addConfig(widgetComponent, "minValue", sliderWidget.getMinValue());
                addConfig(widgetComponent, "maxValue", sliderWidget.getMaxValue());
                addConfig(widgetComponent, "step", sliderWidget.getStep());
                if (sliderWidget.isSwitchEnabled()) {
                    addConfig(widgetComponent, "switchEnabled", true);
                }
                if (sliderWidget.isReleaseOnly()) {
                    addConfig(widgetComponent, "releaseOnly", true);
                }
            }
            case Colortemperaturepicker colorTemperaturePickerWidget -> {
                addConfig(widgetComponent, "minValue", colorTemperaturePickerWidget.getMinValue());
                addConfig(widgetComponent, "maxValue", colorTemperaturePickerWidget.getMaxValue());
            }
            case Input inputWidget -> {
                addConfig(widgetComponent, "inputHint", inputWidget.getInputHint());
            }
            case Webview webviewWidget -> {
                addConfig(widgetComponent, "url", webviewWidget.getUrl());
                int height = webviewWidget.getHeight();
                if (height > 0) {
                    addConfig(widgetComponent, "height", height);
                }
            }
            case Mapview mapviewWidget -> {
                int height = mapviewWidget.getHeight();
                if (height > 0) {
                    addConfig(widgetComponent, "height", height);
                }
            }
            case Image imageWidget -> {
                addConfig(widgetComponent, "url", imageWidget.getUrl());
                int refresh = imageWidget.getRefresh();
                if (refresh > 0) {
                    addConfig(widgetComponent, "refresh", refresh);
                }
            }
            case Video videoWidget -> {
                addConfig(widgetComponent, "url", videoWidget.getUrl());
                addConfig(widgetComponent, "encoding", videoWidget.getEncoding());
            }
            case Chart chartWidget -> {
                int refresh = chartWidget.getRefresh();
                if (refresh > 0) {
                    addConfig(widgetComponent, "refresh", refresh);
                }
                addConfig(widgetComponent, "period", chartWidget.getPeriod());
                addConfig(widgetComponent, "service", chartWidget.getService());
                addConfig(widgetComponent, "legend", chartWidget.hasLegend());
                addConfig(widgetComponent, "forceAsItem", chartWidget.forceAsItem());
                addConfig(widgetComponent, "yAxisDecimalPattern", chartWidget.getYAxisDecimalPattern());
                addConfig(widgetComponent, "interpolation", chartWidget.getInterpolation());
            }
            case Default defaultWidget -> {
                int height = defaultWidget.getHeight();
                if (height > 0) {
                    addConfig(widgetComponent, "height", height);
                }
            }
            default -> {
                LOGGER.debug("Widget type {} is currently not supported", widget.getWidgetType());
            }
        }

        if (widget instanceof LinkableWidget linkableWidget && !linkableWidget.getWidgets().isEmpty()) {
            widgetComponent.addSlot("widgets");
            linkableWidget.getWidgets().stream().map(UIComponentSitemapMapper::map)
                    .forEach(component -> widgetComponent.addComponent("widgets", component));
        }
        return widgetComponent;
    }

    private static @Nullable Collection<String> map(List<?> objects) {
        if (objects.isEmpty()) {
            return null;
        }
        return objects.stream().map(UIComponentSitemapMapper::map).filter(Objects::nonNull).toList();
    }

    private static @Nullable String map(Object object) {
        if (object instanceof Rule rule) {
            return map(rule);
        } else if (object instanceof Mapping mapping) {
            return map(mapping);
        } else if (object instanceof ButtonDefinition buttonDefinition) {
            return map(buttonDefinition);
        }
        return null;
    }

    private static String map(Rule rule) {
        String ruleString = rule.getConditions().stream().map(UIComponentSitemapMapper::map)
                .collect(Collectors.joining(" AND "));
        String argument = rule.getArgument();
        if (argument != null) {
            ruleString = ruleString + "=\"" + argument + "\"";
        }
        return ruleString;
    }

    private static String map(Condition condition) {
        StringBuilder builder = new StringBuilder();
        String item = condition.getItem();
        if (item != null) {
            builder.append(item);
        }
        String operator = condition.getCondition();
        if (operator != null) {
            builder.append(operator);
        }
        builder.append("\"").append(condition.getValue()).append("\"");
        return builder.toString();
    }

    private static String map(Mapping mapping) {
        StringBuilder builder = new StringBuilder();
        builder.append(mapping.getCmd());
        String releaseCmd = mapping.getReleaseCmd();
        if (releaseCmd != null) {
            builder.append(":").append(releaseCmd);
        }
        builder.append("=\"").append(mapping.getLabel()).append("\"");
        String icon = mapping.getIcon();
        if (icon != null) {
            builder.append("=").append(icon);
        }
        return builder.toString();
    }

    private static String map(ButtonDefinition button) {
        StringBuilder builder = new StringBuilder();
        builder.append(button.getRow()).append(":").append(button.getColumn()).append(":").append(button.getCmd());
        builder.append("=").append(button.getLabel());
        String icon = button.getIcon();
        if (icon != null) {
            builder.append("=").append(icon);
        }
        return builder.toString();
    }

    private static void addConfig(UIComponent component, String key, @Nullable Object value) {
        if (value != null) {
            component.addConfig(key, value);
        }
    }
}
