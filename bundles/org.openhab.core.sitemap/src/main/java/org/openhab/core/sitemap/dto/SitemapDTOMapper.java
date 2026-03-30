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
import org.openhab.core.sitemap.Colorpicker;
import org.openhab.core.sitemap.Colortemperaturepicker;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Default;
import org.openhab.core.sitemap.Frame;
import org.openhab.core.sitemap.Group;
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
import org.openhab.core.sitemap.Text;
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.Webview;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.internal.ButtonDefinitionImpl;
import org.openhab.core.sitemap.internal.ButtonImpl;
import org.openhab.core.sitemap.internal.ButtongridImpl;
import org.openhab.core.sitemap.internal.ChartImpl;
import org.openhab.core.sitemap.internal.ColorpickerImpl;
import org.openhab.core.sitemap.internal.ColortemperaturepickerImpl;
import org.openhab.core.sitemap.internal.ConditionImpl;
import org.openhab.core.sitemap.internal.DefaultImpl;
import org.openhab.core.sitemap.internal.FrameImpl;
import org.openhab.core.sitemap.internal.GroupImpl;
import org.openhab.core.sitemap.internal.ImageImpl;
import org.openhab.core.sitemap.internal.InputImpl;
import org.openhab.core.sitemap.internal.MappingImpl;
import org.openhab.core.sitemap.internal.MapviewImpl;
import org.openhab.core.sitemap.internal.RuleImpl;
import org.openhab.core.sitemap.internal.SelectionImpl;
import org.openhab.core.sitemap.internal.SetpointImpl;
import org.openhab.core.sitemap.internal.SitemapImpl;
import org.openhab.core.sitemap.internal.SliderImpl;
import org.openhab.core.sitemap.internal.SwitchImpl;
import org.openhab.core.sitemap.internal.TextImpl;
import org.openhab.core.sitemap.internal.VideoImpl;
import org.openhab.core.sitemap.internal.WebviewImpl;

/**
 * The {@link SitemapDTOMapper} is a utility class to map sitemaps into data transfer objects (DTO).
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SitemapDTOMapper {

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
                widgetDTO.forceAsItem = chartWidget.forceAsItem();
                widgetDTO.yAxisDecimalPattern = chartWidget.getYAxisDecimalPattern();
                widgetDTO.interpolation = chartWidget.getInterpolation();
            }
            default -> {
                // nothing to do
            }
        }

        if (widget instanceof LinkableWidget linkableWidget) {
            widgetDTO.widgets = linkableWidget.getWidgets().stream().map(SitemapDTOMapper::map).toList();
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
        return buttonDTO;
    }

    public static Sitemap map(SitemapDefinitionDTO sitemapDTO) {
        Sitemap sitemap = new SitemapImpl(sitemapDTO.name);
        sitemap.setLabel(sitemapDTO.label);
        sitemap.setIcon(sitemapDTO.icon);
        sitemap.setWidgets(sitemapDTO.widgets.stream().map(SitemapDTOMapper::map).filter(Objects::nonNull).toList());
        return sitemap;
    }

    private @Nullable static Widget map(WidgetDefinitionDTO widgetDTO) {
        Widget widget = null;
        switch (widgetDTO.type) {
            case "Frame" -> {
                Frame frameWidget = new FrameImpl();
                widget = frameWidget;
            }
            case "Default" -> {
                Default defaultWidget = new DefaultImpl();
                widget = defaultWidget;
            }
            case "Text" -> {
                Text textWidget = new TextImpl();
                widget = textWidget;
            }
            case "Group" -> {
                Group groupWidget = new GroupImpl();
                widget = groupWidget;
            }
            case "Switch" -> {
                Switch switchWidget = new SwitchImpl();
                switchWidget.setMappings(widgetDTO.mappings.stream().map(SitemapDTOMapper::map).toList());
                widget = switchWidget;
            }
            case "Buttongrid" -> {
                Buttongrid buttongridWidget = new ButtongridImpl();
                buttongridWidget.setButtons(widgetDTO.buttons.stream().map(SitemapDTOMapper::map).toList());
                widget = buttongridWidget;
            }
            case "Button" -> {
                Button buttonWidget = new ButtonImpl();
                buttonWidget.setRow(widgetDTO.row);
                buttonWidget.setColumn(widgetDTO.column);
                buttonWidget.setStateless(widgetDTO.stateless);
                buttonWidget.setCmd(widgetDTO.command);
                buttonWidget.setReleaseCmd(widgetDTO.releaseCommand);
                widget = buttonWidget;
            }
            case "Selection" -> {
                Selection selectionWidget = new SelectionImpl();
                selectionWidget.setMappings(widgetDTO.mappings.stream().map(SitemapDTOMapper::map).toList());
                widget = selectionWidget;
            }
            case "Setpoint" -> {
                Setpoint setpointWidget = new SetpointImpl();
                setpointWidget.setMinValue(widgetDTO.minValue);
                setpointWidget.setMaxValue(widgetDTO.maxValue);
                setpointWidget.setStep(widgetDTO.step);
                widget = setpointWidget;
            }
            case "Slider" -> {
                Slider sliderWidget = new SliderImpl();
                sliderWidget.setMinValue(widgetDTO.minValue);
                sliderWidget.setMaxValue(widgetDTO.maxValue);
                sliderWidget.setStep(widgetDTO.step);
                sliderWidget.setSwitchEnabled(widgetDTO.switchSupport);
                sliderWidget.setReleaseOnly(widgetDTO.releaseOnly);
                widget = sliderWidget;
            }
            case "Colorpicker" -> {
                Colorpicker colorpickerWidget = new ColorpickerImpl();
                widget = colorpickerWidget;
            }
            case "Colortemperaturepicker" -> {
                Colortemperaturepicker colortemperaturepickerWidget = new ColortemperaturepickerImpl();
                colortemperaturepickerWidget.setMinValue(widgetDTO.minValue);
                colortemperaturepickerWidget.setMaxValue(widgetDTO.maxValue);
                widget = colortemperaturepickerWidget;
            }
            case "Input" -> {
                Input inputWidget = new InputImpl();
                inputWidget.setInputHint(widgetDTO.inputHint);
                widget = inputWidget;
            }
            case "Webview" -> {
                Webview webviewWidget = new WebviewImpl();
                webviewWidget.setUrl(widgetDTO.url);
                webviewWidget.setHeight(widgetDTO.height);
                widget = webviewWidget;
            }
            case "Mapview" -> {
                Mapview mapviewWidget = new MapviewImpl();
                mapviewWidget.setHeight(widgetDTO.height);
                widget = mapviewWidget;
            }
            case "Image" -> {
                Image imageWidget = new ImageImpl();
                imageWidget.setUrl(widgetDTO.url);
                imageWidget.setRefresh(widgetDTO.refresh);
                widget = imageWidget;
            }
            case "Video" -> {
                Video videoWidget = new VideoImpl();
                videoWidget.setUrl(widgetDTO.url);
                videoWidget.setEncoding(widgetDTO.encoding);
                widget = videoWidget;
            }
            case "Chart" -> {
                Chart chartWidget = new ChartImpl();
                chartWidget.setRefresh(widgetDTO.refresh);
                chartWidget.setPeriod(widgetDTO.period);
                chartWidget.setService(widgetDTO.service);
                chartWidget.setLegend(widgetDTO.legend);
                chartWidget.setForceAsItem(widgetDTO.forceAsItem);
                chartWidget.setYAxisDecimalPattern(widgetDTO.yAxisDecimalPattern);
                chartWidget.setInterpolation(widgetDTO.interpolation);
                widget = chartWidget;
            }
            default -> {
                return null;
            }
        }

        widget.setItem(widgetDTO.item);
        widget.setLabel(widgetDTO.label);
        widget.setIcon(widgetDTO.icon);
        widget.setStaticIcon(widgetDTO.staticIcon);

        widget.setIconRules(widgetDTO.iconRules.stream().map(SitemapDTOMapper::map).toList());
        widget.setVisibility(widgetDTO.visibilityRules.stream().map(SitemapDTOMapper::map).toList());
        widget.setLabelColor(widgetDTO.labelColorRules.stream().map(SitemapDTOMapper::map).toList());
        widget.setValueColor(widgetDTO.valueColorRules.stream().map(SitemapDTOMapper::map).toList());
        widget.setIconColor(widgetDTO.iconColorRules.stream().map(SitemapDTOMapper::map).toList());

        if (widget instanceof LinkableWidget linkableWidget) {
            linkableWidget.setWidgets(
                    widgetDTO.widgets.stream().map(SitemapDTOMapper::map).filter(Objects::nonNull).toList());
        }
        return widget;
    }

    private static Mapping map(MappingDTO mappingDTO) {
        Mapping mapping = new MappingImpl();
        mapping.setLabel(mappingDTO.label);
        mapping.setIcon(mappingDTO.icon);
        mapping.setCmd(mappingDTO.command);
        mapping.setReleaseCmd(mappingDTO.releaseCommand);
        return mapping;
    }

    private static ButtonDefinition map(ButtonDefinitionDTO buttonDefinitionDTO) {
        ButtonDefinition buttonDefinition = new ButtonDefinitionImpl();
        buttonDefinition.setRow(buttonDefinitionDTO.row);
        buttonDefinition.setColumn(buttonDefinitionDTO.column);
        buttonDefinition.setCmd(buttonDefinitionDTO.command);
        buttonDefinition.setLabel(buttonDefinitionDTO.label);
        return buttonDefinition;
    }

    private static Rule map(RuleDTO ruleDTO) {
        Rule rule = new RuleImpl();
        rule.setArgument(ruleDTO.argument);
        rule.setConditions(ruleDTO.conditions.stream().map(SitemapDTOMapper::map).toList());
        return rule;
    }

    private static Condition map(ConditionDTO conditionDTO) {
        Condition condition = new ConditionImpl();
        condition.setItem(conditionDTO.item);
        condition.setCondition(conditionDTO.condition);
        condition.setValue(conditionDTO.value);
        return condition;
    }
}
