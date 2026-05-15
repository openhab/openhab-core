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
package org.openhab.core.model.yaml.internal.sitemaps.fileconverter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.sitemaps.YamlConditionDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlMappingDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlRuleWithAndConditionsDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlRuleWithUniqueConditionDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlSitemapDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlSitemapProvider;
import org.openhab.core.model.yaml.internal.sitemaps.YamlWidgetDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlWidgetIconDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlWidgetLabelDTO;
import org.openhab.core.sitemap.Button;
import org.openhab.core.sitemap.Chart;
import org.openhab.core.sitemap.Colortemperaturepicker;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Default;
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Input;
import org.openhab.core.sitemap.LinkableWidget;
import org.openhab.core.sitemap.Mapping;
import org.openhab.core.sitemap.Mapview;
import org.openhab.core.sitemap.NestedSitemap;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Selection;
import org.openhab.core.sitemap.Setpoint;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Slider;
import org.openhab.core.sitemap.Switch;
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.Webview;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.fileconverter.SitemapParser;
import org.openhab.core.sitemap.fileconverter.SitemapSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlSitemapConverter} is the YAML converter for {@link Sitemap} objects.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Mark Herwege - Add support for nested sitemaps
 */
@NonNullByDefault
@Component(immediate = true, service = { SitemapSerializer.class, SitemapParser.class })
public class YamlSitemapConverter implements SitemapSerializer, SitemapParser {

    private final YamlModelRepository modelRepository;
    private final YamlSitemapProvider sitemapProvider;

    @Activate
    public YamlSitemapConverter(final @Reference YamlModelRepository modelRepository,
            final @Reference YamlSitemapProvider sitemapProvider) {
        this.modelRepository = modelRepository;
        this.sitemapProvider = sitemapProvider;
    }

    @Override
    public String getGeneratedFormat() {
        return "YAML";
    }

    @Override
    public void setSitemapsToBeSerialized(String id, List<Sitemap> sitemaps) {
        List<YamlElement> elements = new ArrayList<>();
        sitemaps.forEach(sitemap -> {
            elements.add(buildSitemapDTO(sitemap));
        });
        modelRepository.addElementsToBeGenerated(id, elements);
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        modelRepository.generateFileFormat(id, out);
    }

    private YamlSitemapDTO buildSitemapDTO(Sitemap sitemap) {
        YamlSitemapDTO dto = new YamlSitemapDTO();
        dto.name = sitemap.getName();
        dto.label = sitemap.getLabel();
        dto.icon = sitemap.getIcon();

        List<YamlWidgetDTO> widgets = new ArrayList<>();
        sitemap.getWidgets().forEach(w -> {
            widgets.add(buildWidgetDTO(w));
        });
        dto.widgets = widgets.isEmpty() ? null : widgets;

        return dto;
    }

    private YamlWidgetDTO buildWidgetDTO(Widget widget) {
        YamlWidgetDTO dto = new YamlWidgetDTO();
        dto.type = widget.getWidgetType();
        dto.item = widget.getItem();

        List<Object> labelColorRules = buildRules(widget.getLabelColor(), false);
        List<Object> valueColorRules = buildRules(widget.getValueColor(), false);
        if (widget.getLabel() != null || !labelColorRules.isEmpty() || !valueColorRules.isEmpty()) {
            String label = widget.getLabel();
            String format = null;
            if (label != null) {
                int idx = label.indexOf("[");
                if (idx >= 0) {
                    format = label.substring(idx + 1, label.length() - 1).trim();
                    label = label.substring(0, idx).trim();
                }
            }
            if (format != null || !labelColorRules.isEmpty() || !valueColorRules.isEmpty()) {
                YamlWidgetLabelDTO widgetLabel = new YamlWidgetLabelDTO();
                widgetLabel.label = label;
                widgetLabel.format = format;
                if (!labelColorRules.isEmpty()) {
                    if (labelColorRules.size() == 1) {
                        Object first = labelColorRules.getFirst();
                        if (first instanceof YamlRuleWithAndConditionsDTO rule
                                && (rule.and == null || rule.and.isEmpty()) && rule.value != null) {
                            widgetLabel.labelColor = new String(rule.value);
                        } else if (first instanceof YamlRuleWithUniqueConditionDTO rule && rule.item == null
                                && rule.operator == null && rule.argument == null && rule.value != null) {
                            widgetLabel.labelColor = new String(rule.value);
                        } else {
                            widgetLabel.labelColor = first;
                        }
                    } else {
                        widgetLabel.labelColor = labelColorRules;
                    }
                }
                if (!valueColorRules.isEmpty()) {
                    if (valueColorRules.size() == 1) {
                        Object first = valueColorRules.getFirst();
                        if (first instanceof YamlRuleWithAndConditionsDTO rule
                                && (rule.and == null || rule.and.isEmpty()) && rule.value != null) {
                            widgetLabel.valueColor = new String(rule.value);
                        } else if (first instanceof YamlRuleWithUniqueConditionDTO rule && rule.item == null
                                && rule.operator == null && rule.argument == null && rule.value != null) {
                            widgetLabel.valueColor = new String(rule.value);
                        } else {
                            widgetLabel.valueColor = first;
                        }
                    } else {
                        widgetLabel.valueColor = valueColorRules;
                    }
                }
                dto.label = widgetLabel;
            } else {
                dto.label = label;
            }
        }

        List<Object> iconRules = buildRules(widget.getIconRules(), false);
        List<Object> iconColorRules = buildRules(widget.getIconColor(), false);
        if (widget.getIcon() != null || !iconRules.isEmpty() || !iconColorRules.isEmpty()) {
            if (widget.isStaticIcon() || !iconRules.isEmpty() || !iconColorRules.isEmpty()) {
                YamlWidgetIconDTO widgetIcon = new YamlWidgetIconDTO();
                widgetIcon.name = widget.getIcon();
                if (widget.isStaticIcon()) {
                    widgetIcon.staticIcon = true;
                }
                if (!iconRules.isEmpty()) {
                    if (iconRules.size() == 1) {
                        Object first = iconRules.getFirst();
                        if (first instanceof YamlRuleWithAndConditionsDTO rule
                                && (rule.and == null || rule.and.isEmpty()) && rule.value != null) {
                            widgetIcon.name = new String(rule.value);
                        } else if (first instanceof YamlRuleWithUniqueConditionDTO rule && rule.item == null
                                && rule.operator == null && rule.argument == null && rule.value != null) {
                            widgetIcon.name = new String(rule.value);
                        } else {
                            widgetIcon.name = first;
                        }
                    } else {
                        widgetIcon.name = iconRules;
                    }
                }
                if (!iconColorRules.isEmpty()) {
                    if (iconColorRules.size() == 1) {
                        Object first = iconColorRules.getFirst();
                        if (first instanceof YamlRuleWithAndConditionsDTO rule
                                && (rule.and == null || rule.and.isEmpty()) && rule.value != null) {
                            widgetIcon.color = new String(rule.value);
                        } else if (first instanceof YamlRuleWithUniqueConditionDTO rule && rule.item == null
                                && rule.operator == null && rule.argument == null && rule.value != null) {
                            widgetIcon.color = new String(rule.value);
                        } else {
                            widgetIcon.color = first;
                        }
                    } else {
                        widgetIcon.color = iconColorRules;
                    }
                }
                dto.icon = widgetIcon;
            } else {
                dto.icon = widget.getIcon();
            }
        }

        List<Object> visibilityRules = buildRules(widget.getVisibility(), true);
        if (!visibilityRules.isEmpty()) {
            if (visibilityRules.size() == 1) {
                dto.visibility = visibilityRules.getFirst();
            } else {
                dto.visibility = visibilityRules;
            }
        }

        switch (widget) {
            case Switch switchWidget -> {
                List<YamlMappingDTO> mappings = buildMappings(switchWidget.getMappings());
                dto.mappings = mappings.isEmpty() ? null : mappings;
            }
            case Button buttonWidget -> {
                dto.row = buttonWidget.getRow();
                dto.column = buttonWidget.getColumn();
                dto.command = buttonWidget.getCmd();
                dto.releaseCommand = buttonWidget.getReleaseCmd();
                if (buttonWidget.isStateless()) {
                    dto.stateless = true;
                }
            }
            case Selection selectionWidget -> {
                List<YamlMappingDTO> mappings = buildMappings(selectionWidget.getMappings());
                dto.mappings = mappings.isEmpty() ? null : mappings;
            }
            case Setpoint setpointWidget -> {
                dto.min = setpointWidget.getMinValue();
                dto.max = setpointWidget.getMaxValue();
                dto.step = setpointWidget.getStep();
            }
            case Slider sliderWidget -> {
                dto.min = sliderWidget.getMinValue();
                dto.max = sliderWidget.getMaxValue();
                dto.step = sliderWidget.getStep();
                if (sliderWidget.isSwitchEnabled()) {
                    dto.switchSupport = true;
                }
                if (sliderWidget.isReleaseOnly()) {
                    dto.releaseOnly = true;
                }
            }
            case Colortemperaturepicker colortemperaturepickerWidget -> {
                dto.min = colortemperaturepickerWidget.getMinValue();
                dto.max = colortemperaturepickerWidget.getMaxValue();
            }
            case Input inputWidget -> {
                dto.hint = inputWidget.getInputHint();
            }
            case Webview webviewWidget -> {
                dto.url = webviewWidget.getUrl();
                if (webviewWidget.getHeight() > 0) {
                    dto.height = webviewWidget.getHeight();
                }
            }
            case Mapview mapviewWidget -> {
                if (mapviewWidget.getHeight() > 0) {
                    dto.height = mapviewWidget.getHeight();
                }
            }
            case Image imageWidget -> {
                dto.url = imageWidget.getUrl();
                if (imageWidget.getRefresh() > 0) {
                    dto.refresh = imageWidget.getRefresh();
                }
            }
            case Video videoWidget -> {
                dto.url = videoWidget.getUrl();
                dto.encoding = videoWidget.getEncoding();
            }
            case Chart chartWidget -> {
                if (chartWidget.getRefresh() > 0) {
                    dto.refresh = chartWidget.getRefresh();
                }
                dto.period = chartWidget.getPeriod();
                dto.service = chartWidget.getService();
                dto.legend = chartWidget.hasLegend();
                if (chartWidget.forceAsItem()) {
                    dto.forceAsItem = true;
                }
                dto.yAxisDecimalPattern = chartWidget.getYAxisDecimalPattern();
                dto.interpolation = chartWidget.getInterpolation();
            }
            case Default defaultWidget -> {
                if (defaultWidget.getHeight() > 0) {
                    dto.height = defaultWidget.getHeight();
                }
            }
            case NestedSitemap nestedSitemapWidget -> {
                dto.name = nestedSitemapWidget.getName();
            }
            default -> {
            }
        }

        if (widget instanceof LinkableWidget linkableWidget) {
            List<YamlWidgetDTO> widgets = new ArrayList<>();
            linkableWidget.getWidgets().forEach(w -> {
                widgets.add(buildWidgetDTO(w));
            });
            dto.widgets = widgets.isEmpty() ? null : widgets;
        }

        return dto;
    }

    private List<YamlMappingDTO> buildMappings(List<Mapping> mappings) {
        List<YamlMappingDTO> dtos = new ArrayList<>();
        mappings.forEach(mapping -> {
            YamlMappingDTO dto = new YamlMappingDTO();
            dto.command = mapping.getCmd();
            dto.releaseCommand = mapping.getReleaseCmd();
            dto.label = mapping.getLabel();
            dto.icon = mapping.getIcon();
            dtos.add(dto);
        });
        return dtos;
    }

    private List<Object> buildRules(List<Rule> rules, boolean ignoreValue) {
        List<Object> dtos = new ArrayList<>();
        rules.forEach(rule -> {
            List<Condition> conditions = rule.getConditions();
            if (!conditions.isEmpty()) {
                if (conditions.size() > 1) {
                    YamlRuleWithAndConditionsDTO dto = new YamlRuleWithAndConditionsDTO();
                    dto.and = buildConditions(conditions);
                    if (!ignoreValue) {
                        dto.value = rule.getArgument();
                    }
                    dtos.add(dto);
                } else {
                    Condition first = conditions.getFirst();
                    YamlRuleWithUniqueConditionDTO dto = new YamlRuleWithUniqueConditionDTO();
                    dto.item = first.getItem();
                    dto.operator = first.getCondition();
                    dto.argument = first.getValue();
                    if (!ignoreValue) {
                        dto.value = rule.getArgument();
                    }
                    dtos.add(dto);
                }
            } else {
                YamlRuleWithUniqueConditionDTO dto = new YamlRuleWithUniqueConditionDTO();
                if (!ignoreValue) {
                    dto.value = rule.getArgument();
                }
                dtos.add(dto);
            }
        });
        return dtos;
    }

    private List<YamlConditionDTO> buildConditions(List<Condition> conditions) {
        List<YamlConditionDTO> dtos = new ArrayList<>();
        conditions.forEach(condition -> {
            YamlConditionDTO dto = new YamlConditionDTO();
            dto.item = condition.getItem();
            dto.operator = condition.getCondition();
            dto.argument = condition.getValue();
            dtos.add(dto);
        });
        return dtos;
    }

    @Override
    public String getParserFormat() {
        return "YAML";
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel(inputStream, errors, warnings);
    }

    @Override
    public Collection<Sitemap> getParsedObjects(String modelName) {
        return sitemapProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
