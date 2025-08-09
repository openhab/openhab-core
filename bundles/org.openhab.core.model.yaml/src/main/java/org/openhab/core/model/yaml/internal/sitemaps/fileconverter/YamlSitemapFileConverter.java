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
package org.openhab.core.model.yaml.internal.sitemaps.fileconverter;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.sitemap.fileconverter.AbstractSitemapFileGenerator;
import org.openhab.core.model.sitemap.fileconverter.SitemapFileGenerator;
import org.openhab.core.model.sitemap.sitemap.ButtonDefinitionList;
import org.openhab.core.model.sitemap.sitemap.Buttongrid;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.ColorArrayList;
import org.openhab.core.model.sitemap.sitemap.Condition;
import org.openhab.core.model.sitemap.sitemap.IconRule;
import org.openhab.core.model.sitemap.sitemap.IconRuleList;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.MappingList;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.SitemapPackage;
import org.openhab.core.model.sitemap.sitemap.VisibilityRule;
import org.openhab.core.model.sitemap.sitemap.VisibilityRuleList;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.model.sitemap.sitemap.impl.ButtonImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ButtongridImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ChartImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ColortemperaturepickerImpl;
import org.openhab.core.model.sitemap.sitemap.impl.DefaultImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ImageImpl;
import org.openhab.core.model.sitemap.sitemap.impl.InputImpl;
import org.openhab.core.model.sitemap.sitemap.impl.MapviewImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SelectionImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SetpointImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SliderImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SwitchImpl;
import org.openhab.core.model.sitemap.sitemap.impl.VideoImpl;
import org.openhab.core.model.sitemap.sitemap.impl.WebviewImpl;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.sitemaps.YamlMappingDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlRuleDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlSitemapDTO;
import org.openhab.core.model.yaml.internal.sitemaps.YamlWidgetDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlSitemapFileConverter} is the YAML file converter for {@link Sitemap} objects.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = SitemapFileGenerator.class)
public class YamlSitemapFileConverter extends AbstractSitemapFileGenerator {

    private final YamlModelRepository modelRepository;

    @Activate
    public YamlSitemapFileConverter(final @Reference YamlModelRepository modelRepository) {
        super();
        this.modelRepository = modelRepository;
    }

    @Override
    public String getFileFormatGenerator() {
        return "YAML";
    }

    @Override
    public synchronized void generateFileFormat(OutputStream out, List<Sitemap> sitemaps) {
        List<YamlElement> elements = new ArrayList<>();
        sitemaps.forEach(sitemap -> {
            elements.add(buildSitemapDTO(sitemap));
        });
        modelRepository.generateSyntaxFromElements(out, elements);
    }

    private YamlSitemapDTO buildSitemapDTO(Sitemap sitemap) {
        YamlSitemapDTO dto = new YamlSitemapDTO();
        dto.uid = sitemap.getName();
        dto.label = sitemap.getLabel();
        dto.icon = sitemap.getIcon();
        EList<Widget> children = sitemap.getChildren();
        if (children != null && !children.isEmpty()) {
            dto.widgets = children.stream().map(w -> buildWidgetDTO(w)).toList();
        }
        return dto;
    }

    private Map.Entry<String, YamlWidgetDTO> buildWidgetDTO(Widget widget) {
        YamlWidgetDTO dto = new YamlWidgetDTO();
        if (!(widget instanceof ButtongridImpl)) {
            // We will convert Buttongrid with buttons definition to explicit Button widget, so don't set item
            dto.item = widget.getItem();
        }
        dto.label = widget.getLabel();

        dto.icon = widget.getStaticIcon() != null ? widget.getStaticIcon()
                : (widget.getIconRules() == null ? widget.getIcon() : null);
        IconRuleList iconRuleList = widget.getIconRules();
        if (iconRuleList != null && iconRuleList.getElements() != null && iconRuleList.getElements().size() > 0) {
            dto.iconRules = iconRuleList.getElements().stream().map(e -> buildRuleDTO(e)).toList();
        }
        dto.staticIcon = widget.getStaticIcon() != null ? true : null;

        switch (widget) {
            case ImageImpl imageWidget: {
                dto.url = imageWidget.getUrl();
                dto.refresh = imageWidget.getRefresh();
                break;
            }
            case VideoImpl videoWidget: {
                dto.url = videoWidget.getUrl();
                dto.encoding = videoWidget.getEncoding();
                break;
            }
            case ChartImpl chartWidget: {
                dto.service = chartWidget.getService();
                dto.refresh = chartWidget.getRefresh();
                dto.period = chartWidget.getPeriod();
                dto.legend = chartWidget.getLegend();
                dto.forceAsItem = chartWidget.getForceAsItem() ? true : null;
                dto.yAxisDecimalPattern = chartWidget.getYAxisDecimalPattern();
                dto.interpolation = chartWidget.getInterpolation();
                break;
            }
            case WebviewImpl webviewWidget: {
                dto.height = webviewWidget.getHeight();
                dto.url = webviewWidget.getUrl();
                break;
            }
            case SwitchImpl switchWidget: {
                MappingList mappingList = switchWidget.getMappings();
                if (mappingList != null && mappingList.getElements() != null && mappingList.getElements().size() > 0) {
                    dto.mappings = mappingList.getElements().stream().map(e -> buildMappingDTO(e))
                            .filter(Objects::nonNull).toList();
                }
                break;
            }
            case MapviewImpl mapviewWidget: {
                dto.height = mapviewWidget.getHeight();
                break;
            }
            case SliderImpl sliderWidget: {
                dto.switchEnabled = sliderWidget.isSwitchEnabled() ? true : null;
                dto.releaseOnly = sliderWidget.isReleaseOnly() ? true : null;
                dto.minValue = sliderWidget.getMinValue();
                dto.maxValue = sliderWidget.getMaxValue();
                dto.step = sliderWidget.getStep();
                break;
            }
            case SelectionImpl selectionWidget: {
                MappingList mappingList = selectionWidget.getMappings();
                if (mappingList != null && mappingList.getElements() != null && mappingList.getElements().size() > 0) {
                    dto.mappings = mappingList.getElements().stream().map(e -> buildMappingDTO(e))
                            .filter(Objects::nonNull).toList();
                }
                break;
            }
            case SetpointImpl setpointWidget: {
                dto.minValue = setpointWidget.getMinValue();
                dto.maxValue = setpointWidget.getMaxValue();
                dto.step = setpointWidget.getStep();
                break;
            }
            case ColortemperaturepickerImpl colortemperaturepickerWidget: {
                dto.minValue = colortemperaturepickerWidget.getMinValue();
                dto.maxValue = colortemperaturepickerWidget.getMaxValue();
                break;
            }
            case InputImpl inputWidget: {
                dto.inputHint = inputWidget.getInputHint();
                break;
            }
            case ButtongridImpl buttongridWidget: {
                List<Map.Entry<String, YamlWidgetDTO>> buttons = convertToButtonWidgets(buttongridWidget);
                if (buttons != null) {
                    dto.widgets = buttons;
                }
                break;
            }
            case ButtonImpl buttonWidget: {
                dto.row = buttonWidget.getRow();
                dto.column = buttonWidget.getColumn();
                dto.stateless = buttonWidget.isStateless() ? true : null;
                dto.cmd = buttonWidget.getCmd();
                dto.releaseCmd = buttonWidget.getReleaseCmd();
                break;
            }
            case DefaultImpl defaultWidget: {
                dto.height = defaultWidget.getHeight();
                break;
            }
            default:
                break;
        }

        ColorArrayList labelColorList = widget.getLabelColor();
        if (labelColorList != null && labelColorList.getElements() != null && labelColorList.getElements().size() > 0) {
            dto.labelColor = labelColorList.getElements().stream().map(e -> buildRuleDTO(e)).toList();
        }
        ColorArrayList valueColorList = widget.getValueColor();
        if (valueColorList != null && valueColorList.getElements() != null && valueColorList.getElements().size() > 0) {
            dto.valueColor = valueColorList.getElements().stream().map(e -> buildRuleDTO(e)).toList();
        }
        ColorArrayList iconColorList = widget.getIconColor();
        if (iconColorList != null && iconColorList.getElements() != null && iconColorList.getElements().size() > 0) {
            dto.iconColor = iconColorList.getElements().stream().map(e -> buildRuleDTO(e)).toList();
        }
        VisibilityRuleList visiblityRuleList = widget.getVisibility();
        if (visiblityRuleList != null && visiblityRuleList.getElements() != null
                && visiblityRuleList.getElements().size() > 0) {
            dto.visibility = visiblityRuleList.getElements().stream().map(e -> buildRuleDTO(e)).toList();
        }

        if (widget instanceof LinkableWidget linkableWidget) {
            EList<Widget> children = linkableWidget.getChildren();
            if (children != null && !children.isEmpty()) {
                dto.widgets = children.stream().map(w -> buildWidgetDTO(w)).toList();
            }
        }

        String widgetType = widget.getClass().getInterfaces()[0].getSimpleName();
        return Map.entry(widgetType, dto);
    }

    private <T> @Nullable YamlRuleDTO buildRuleDTO(T rule) {
        EList<Condition> conditions = null;
        String argument = null;
        switch (rule) {
            case IconRule iconRule: {
                conditions = iconRule.getConditions();
                argument = iconRule.getArg();
                break;
            }
            case ColorArray colorArray: {
                conditions = colorArray.getConditions();
                argument = colorArray.getArg();
                break;
            }
            case VisibilityRule visiblityRule: {
                conditions = visiblityRule.getConditions();
                break;
            }
            default:
                break;
        }
        if (conditions == null) {
            return null;
        }
        YamlRuleDTO dto = new YamlRuleDTO();
        dto.conditions = conditions.stream().map(c -> {
            YamlRuleDTO.Condition condition = dto.new Condition();
            condition.item = c.getItem();
            condition.condition = c.getCondition();
            condition.value = (c.getSign() != null ? c.getSign() : "") + c.getState();
            return condition;
        }).toList();
        dto.argument = argument;
        return dto;
    }

    private YamlMappingDTO buildMappingDTO(Mapping mapping) {
        YamlMappingDTO dto = new YamlMappingDTO();
        dto.cmd = mapping.getCmd();
        dto.releaseCmd = mapping.getReleaseCmd();
        dto.label = mapping.getLabel();
        dto.icon = mapping.getIcon();
        return dto;
    }

    private @Nullable List<Map.Entry<String, YamlWidgetDTO>> convertToButtonWidgets(Buttongrid widget) {
        String item = widget.getItem();
        ButtonDefinitionList buttons = widget.getButtons();
        if (buttons == null || buttons.getElements() == null) {
            return null;
        }
        return buttons.getElements().stream().map(b -> {
            ButtonImpl buttonWidget = (ButtonImpl) SitemapFactory.eINSTANCE.createButton();
            buttonWidget.eSet(SitemapPackage.BUTTON__ITEM, item);
            buttonWidget.eSet(SitemapPackage.BUTTON__ROW, b.getRow());
            buttonWidget.eSet(SitemapPackage.BUTTON__COLUMN, b.getColumn());
            buttonWidget.eSet(SitemapPackage.BUTTON__CMD, b.getCmd());
            buttonWidget.eSet(SitemapPackage.BUTTON__LABEL, b.getLabel());
            buttonWidget.eSet(SitemapPackage.BUTTON__ICON, b.getIcon());
            return buildWidgetDTO(buttonWidget);
        }).toList();
    }
}
