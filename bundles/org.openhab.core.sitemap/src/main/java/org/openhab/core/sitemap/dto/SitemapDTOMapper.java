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
package org.openhab.core.sitemap.dto;

import java.util.List;
import java.util.Objects;

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
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Selection;
import org.openhab.core.sitemap.Setpoint;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Slider;
import org.openhab.core.sitemap.Switch;
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.Webview;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SitemapDTOMapper} is a utility class to map sitemaps into data transfer objects (DTO).
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SitemapDTOMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapDTOMapper.class);

    /**
     * Maps sitemaps into sitemap data transfer object (DTO).
     *
     * @param sitemap the sitemap
     * @return the sitemap DTO object
     */
    public static SitemapDefinitionDTO map(Sitemap sitemap) {
        SitemapDefinitionDTO sitemapDTO = new SitemapDefinitionDTO();
        sitemapDTO.name = sitemap.getName();
        sitemapDTO.label = sitemap.getLabel();
        sitemapDTO.icon = sitemap.getIcon();

        List<Widget> widgets = sitemap.getWidgets();
        if (!widgets.isEmpty()) {
            sitemapDTO.widgets = widgets.stream().map(SitemapDTOMapper::map).toList();
        }
        return sitemapDTO;
    }

    private static WidgetDefinitionDTO map(Widget widget) {
        WidgetDefinitionDTO widgetDTO = new WidgetDefinitionDTO();
        widgetDTO.type = widget.getWidgetType();
        widgetDTO.item = widget.getItem();
        widgetDTO.label = widget.getLabel();
        widgetDTO.icon = widget.getIcon();
        if (widget.isStaticIcon()) {
            widgetDTO.staticIcon = true;
        }

        List<Rule> iconRules = widget.getIconRules();
        if (!iconRules.isEmpty()) {
            widgetDTO.iconRules = iconRules.stream().map(SitemapDTOMapper::map).toList();
        }
        List<Rule> visibilityRules = widget.getVisibility();
        if (!visibilityRules.isEmpty()) {
            widgetDTO.visibilityRules = visibilityRules.stream().map(SitemapDTOMapper::map).toList();
        }
        List<Rule> labelColorRules = widget.getLabelColor();
        if (!labelColorRules.isEmpty()) {
            widgetDTO.labelColorRules = labelColorRules.stream().map(SitemapDTOMapper::map).toList();
        }
        List<Rule> valueColorRules = widget.getValueColor();
        if (!valueColorRules.isEmpty()) {
            widgetDTO.valueColorRules = valueColorRules.stream().map(SitemapDTOMapper::map).toList();
        }
        List<Rule> iconColorRules = widget.getIconColor();
        if (!iconColorRules.isEmpty()) {
            widgetDTO.iconColorRules = iconColorRules.stream().map(SitemapDTOMapper::map).toList();
        }

        switch (widget) {
            case Switch switchWidget -> {
                List<Mapping> mappings = switchWidget.getMappings();
                if (!mappings.isEmpty()) {
                    widgetDTO.mappings = mappings.stream().map(SitemapDTOMapper::map).toList();
                }
            }
            case Buttongrid buttongridWidget -> {
                List<ButtonDefinition> buttons = buttongridWidget.getButtons();
                if (!buttons.isEmpty()) {
                    widgetDTO.buttons = buttons.stream().map(SitemapDTOMapper::map).toList();
                }
            }
            case Button buttonWidget -> {
                widgetDTO.row = buttonWidget.getRow();
                widgetDTO.column = buttonWidget.getColumn();
                if (buttonWidget.isStateless()) {
                    widgetDTO.stateless = true;
                }
                widgetDTO.command = buttonWidget.getCmd();
                widgetDTO.releaseCommand = buttonWidget.getReleaseCmd();
            }
            case Selection selectionWidget -> {
                List<Mapping> mappings = selectionWidget.getMappings();
                if (!mappings.isEmpty()) {
                    widgetDTO.mappings = mappings.stream().map(SitemapDTOMapper::map).toList();
                }
            }
            case Setpoint setpointWidget -> {
                widgetDTO.minValue = setpointWidget.getMinValue();
                widgetDTO.maxValue = setpointWidget.getMaxValue();
                widgetDTO.step = setpointWidget.getStep();
            }
            case Slider sliderWidget -> {
                widgetDTO.minValue = sliderWidget.getMinValue();
                widgetDTO.maxValue = sliderWidget.getMaxValue();
                widgetDTO.step = sliderWidget.getStep();
                if (sliderWidget.isSwitchEnabled()) {
                    widgetDTO.switchSupport = true;
                }
                if (sliderWidget.isReleaseOnly()) {
                    widgetDTO.releaseOnly = true;
                }
            }
            case Colortemperaturepicker colortemperaturepickerWidget -> {
                widgetDTO.minValue = colortemperaturepickerWidget.getMinValue();
                widgetDTO.maxValue = colortemperaturepickerWidget.getMaxValue();
            }
            case Input inputWidget -> {
                widgetDTO.inputHint = inputWidget.getInputHint();
            }
            case Webview webviewWidget -> {
                widgetDTO.url = webviewWidget.getUrl();
                int height = webviewWidget.getHeight();
                if (height > 0) {
                    widgetDTO.height = height;
                }
            }
            case Mapview mapviewWidget -> {
                int height = mapviewWidget.getHeight();
                if (height > 0) {
                    widgetDTO.height = height;
                }
            }
            case Image imageWidget -> {
                widgetDTO.url = imageWidget.getUrl();
                int refresh = imageWidget.getRefresh();
                if (refresh > 0) {
                    widgetDTO.refresh = refresh;
                }
            }
            case Video videoWidget -> {
                widgetDTO.url = videoWidget.getUrl();
                widgetDTO.encoding = videoWidget.getEncoding();
            }
            case Chart chartWidget -> {
                int refresh = chartWidget.getRefresh();
                if (refresh > 0) {
                    widgetDTO.refresh = refresh;
                }
                widgetDTO.period = chartWidget.getPeriod();
                widgetDTO.service = chartWidget.getService();
                widgetDTO.legend = chartWidget.hasLegend();
                if (chartWidget.forceAsItem()) {
                    widgetDTO.forceAsItem = true;
                }
                widgetDTO.yAxisDecimalPattern = chartWidget.getYAxisDecimalPattern();
                widgetDTO.interpolation = chartWidget.getInterpolation();
            }
            case Default defaultWidget -> {
                int height = defaultWidget.getHeight();
                if (height > 0) {
                    widgetDTO.height = height;
                }
            }
            default -> {
                LOGGER.debug("Widget type {} is currently not supported", widget.getWidgetType());
            }
        }

        if (widget instanceof LinkableWidget linkableWidget) {
            List<Widget> widgets = linkableWidget.getWidgets();
            if (!widgets.isEmpty()) {
                widgetDTO.widgets = widgets.stream().map(SitemapDTOMapper::map).toList();
            }
        }
        return widgetDTO;
    }

    private static RuleDTO map(Rule rule) {
        RuleDTO ruleDTO = new RuleDTO();
        List<Condition> conditions = rule.getConditions();
        if (!conditions.isEmpty()) {
            ruleDTO.conditions = conditions.stream().map(SitemapDTOMapper::map).toList();
        }
        ruleDTO.argument = rule.getArgument();
        return ruleDTO;
    }

    private static ConditionDTO map(Condition condition) {
        ConditionDTO conditionDTO = new ConditionDTO();
        conditionDTO.item = condition.getItem();
        conditionDTO.condition = condition.getCondition();
        conditionDTO.value = condition.getValue();
        return conditionDTO;
    }

    private static MappingDTO map(Mapping mapping) {
        MappingDTO mappingDTO = new MappingDTO();
        mappingDTO.label = mapping.getLabel();
        mappingDTO.icon = mapping.getIcon();
        mappingDTO.command = mapping.getCmd();
        mappingDTO.releaseCommand = mapping.getReleaseCmd();
        return mappingDTO;
    }

    private static ButtonDefinitionDTO map(ButtonDefinition button) {
        ButtonDefinitionDTO buttonDTO = new ButtonDefinitionDTO();
        buttonDTO.row = button.getRow();
        buttonDTO.column = button.getColumn();
        buttonDTO.command = button.getCmd();
        buttonDTO.label = button.getLabel();
        buttonDTO.icon = button.getIcon();
        return buttonDTO;
    }

    public static Sitemap map(SitemapDefinitionDTO sitemapDTO, SitemapFactory sitemapFactory) {
        if (sitemapDTO.name == null) {
            throw new IllegalArgumentException("Sitemap name must not be null");
        }
        Sitemap sitemap = sitemapFactory.createSitemap(sitemapDTO.name);
        sitemap.setLabel(sitemapDTO.label);
        sitemap.setIcon(sitemapDTO.icon);
        List<WidgetDefinitionDTO> widgets = sitemapDTO.widgets != null ? sitemapDTO.widgets : List.of();
        sitemap.setWidgets(
                widgets.stream().map(widget -> map(widget, sitemap, sitemapFactory)).filter(Objects::nonNull).toList());
        return sitemap;
    }

    private @Nullable static Widget map(WidgetDefinitionDTO widgetDTO, Parent parent, SitemapFactory sitemapFactory) {
        if (widgetDTO.type == null) {
            throw new IllegalArgumentException("All widgets must have a type");
        }
        Widget widget = sitemapFactory.createWidget(widgetDTO.type, parent);
        if (widget == null) {
            return null;
        }
        switch (widget) {
            case Switch switchWidget -> {
                List<MappingDTO> mappings = widgetDTO.mappings != null ? widgetDTO.mappings : List.of();
                switchWidget.setMappings(mappings.stream().map(mapping -> map(mapping, sitemapFactory)).toList());
            }
            case Buttongrid buttongridWidget -> {
                List<ButtonDefinitionDTO> buttons = widgetDTO.buttons != null ? widgetDTO.buttons : List.of();
                buttongridWidget.setButtons(buttons.stream().map(button -> map(button, sitemapFactory)).toList());
            }
            case Button buttonWidget -> {
                if (widgetDTO.row != null) {
                    buttonWidget.setRow(widgetDTO.row);
                }
                if (widgetDTO.column != null) {
                    buttonWidget.setColumn(widgetDTO.column);
                }
                buttonWidget.setStateless(widgetDTO.stateless);
                if (widgetDTO.command != null) {
                    buttonWidget.setCmd(widgetDTO.command);
                }
                buttonWidget.setReleaseCmd(widgetDTO.releaseCommand);
            }
            case Selection selectionWidget -> {
                List<MappingDTO> mappings = widgetDTO.mappings != null ? widgetDTO.mappings : List.of();
                selectionWidget.setMappings(mappings.stream().map(mapping -> map(mapping, sitemapFactory)).toList());
            }
            case Setpoint setpointWidget -> {
                setpointWidget.setMinValue(widgetDTO.minValue);
                setpointWidget.setMaxValue(widgetDTO.maxValue);
                setpointWidget.setStep(widgetDTO.step);
            }
            case Slider sliderWidget -> {
                sliderWidget.setMinValue(widgetDTO.minValue);
                sliderWidget.setMaxValue(widgetDTO.maxValue);
                sliderWidget.setStep(widgetDTO.step);
                sliderWidget.setSwitchEnabled(widgetDTO.switchSupport);
                sliderWidget.setReleaseOnly(widgetDTO.releaseOnly);
            }
            case Colortemperaturepicker colortemperaturepickerWidget -> {
                colortemperaturepickerWidget.setMinValue(widgetDTO.minValue);
                colortemperaturepickerWidget.setMaxValue(widgetDTO.maxValue);
            }
            case Input inputWidget -> {
                inputWidget.setInputHint(widgetDTO.inputHint);
            }
            case Webview webviewWidget -> {
                if (widgetDTO.url != null) {
                    webviewWidget.setUrl(widgetDTO.url);
                }
                webviewWidget.setHeight(widgetDTO.height);
            }
            case Mapview mapviewWidget -> {
                mapviewWidget.setHeight(widgetDTO.height);
            }
            case Image imageWidget -> {
                imageWidget.setUrl(widgetDTO.url);
                imageWidget.setRefresh(widgetDTO.refresh);
            }
            case Video videoWidget -> {
                if (widgetDTO.url != null) {
                    videoWidget.setUrl(widgetDTO.url);
                }
                videoWidget.setEncoding(widgetDTO.encoding);
            }
            case Chart chartWidget -> {
                chartWidget.setRefresh(widgetDTO.refresh);
                if (widgetDTO.period != null) {
                    chartWidget.setPeriod(widgetDTO.period);
                }
                if (widgetDTO.service != null) {
                    chartWidget.setService(widgetDTO.service);
                }
                chartWidget.setLegend(widgetDTO.legend);
                chartWidget.setForceAsItem(widgetDTO.forceAsItem);
                chartWidget.setYAxisDecimalPattern(widgetDTO.yAxisDecimalPattern);
                chartWidget.setInterpolation(widgetDTO.interpolation);
            }
            case Default defaultWidget -> {
                defaultWidget.setHeight(widgetDTO.height);
            }
            default -> {
                // nothing to do
            }
        }
        ;

        widget.setItem(widgetDTO.item);
        widget.setLabel(widgetDTO.label);
        widget.setIcon(widgetDTO.icon);
        widget.setStaticIcon(widgetDTO.staticIcon);

        List<RuleDTO> iconRules = widgetDTO.iconRules != null ? widgetDTO.iconRules : List.of();
        widget.setIconRules(iconRules.stream().map(rule -> map(rule, sitemapFactory)).toList());
        List<RuleDTO> visibilityRules = widgetDTO.visibilityRules != null ? widgetDTO.visibilityRules : List.of();
        widget.setVisibility(visibilityRules.stream().map(rule -> map(rule, sitemapFactory)).toList());
        List<RuleDTO> labelColorRules = widgetDTO.labelColorRules != null ? widgetDTO.labelColorRules : List.of();
        widget.setLabelColor(labelColorRules.stream().map(rule -> map(rule, sitemapFactory)).toList());
        List<RuleDTO> valueColorRules = widgetDTO.valueColorRules != null ? widgetDTO.valueColorRules : List.of();
        widget.setValueColor(valueColorRules.stream().map(rule -> map(rule, sitemapFactory)).toList());
        List<RuleDTO> iconColorRules = widgetDTO.iconColorRules != null ? widgetDTO.iconColorRules : List.of();
        widget.setIconColor(iconColorRules.stream().map(rule -> map(rule, sitemapFactory)).toList());

        if (widget instanceof LinkableWidget linkableWidget) {
            List<WidgetDefinitionDTO> widgets = widgetDTO.widgets != null ? widgetDTO.widgets : List.of();
            linkableWidget
                    .setWidgets(widgets.stream().map(childWidget -> map(childWidget, linkableWidget, sitemapFactory))
                            .filter(Objects::nonNull).toList());
        }
        return widget;
    }

    private static Mapping map(MappingDTO mappingDTO, SitemapFactory sitemapFactory) {
        Mapping mapping = sitemapFactory.createMapping();
        if (mappingDTO.label != null) {
            mapping.setLabel(mappingDTO.label);
        }
        mapping.setIcon(mappingDTO.icon);
        if (mappingDTO.command != null) {
            mapping.setCmd(mappingDTO.command);
        }
        mapping.setReleaseCmd(mappingDTO.releaseCommand);
        return mapping;
    }

    private static ButtonDefinition map(ButtonDefinitionDTO buttonDefinitionDTO, SitemapFactory sitemapFactory) {
        ButtonDefinition buttonDefinition = sitemapFactory.createButtonDefinition();
        if (buttonDefinitionDTO.row != null) {
            buttonDefinition.setRow(buttonDefinitionDTO.row);
        }
        if (buttonDefinitionDTO.column != null) {
            buttonDefinition.setColumn(buttonDefinitionDTO.column);
        }
        if (buttonDefinitionDTO.command != null) {
            buttonDefinition.setCmd(buttonDefinitionDTO.command);
        }
        if (buttonDefinitionDTO.label != null) {
            buttonDefinition.setLabel(buttonDefinitionDTO.label);
        }
        buttonDefinition.setIcon(buttonDefinitionDTO.icon);
        return buttonDefinition;
    }

    private static Rule map(RuleDTO ruleDTO, SitemapFactory sitemapFactory) {
        Rule rule = sitemapFactory.createRule();
        rule.setArgument(ruleDTO.argument);
        List<ConditionDTO> conditions = ruleDTO.conditions != null ? ruleDTO.conditions : List.of();
        rule.setConditions(conditions.stream().map(condition -> map(condition, sitemapFactory)).toList());
        return rule;
    }

    private static Condition map(ConditionDTO conditionDTO, SitemapFactory sitemapFactory) {
        Condition condition = sitemapFactory.createCondition();
        condition.setItem(conditionDTO.item);
        condition.setCondition(conditionDTO.condition);
        if (conditionDTO.value != null) {
            condition.setValue(conditionDTO.value);
        }
        return condition;
    }
}
