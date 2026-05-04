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
package org.openhab.core.model.yaml.internal.sitemaps;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.model.yaml.YamlModelListener;
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
import org.openhab.core.sitemap.registry.SitemapProvider;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlSitemapProvider} is an OSGi service, that allows to define UI sitemaps in YAML configuration files.
 * Files can be added, updated or removed at runtime.
 * These sitemaps are automatically exposed to the {@link SitemapRegistry}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { SitemapProvider.class, YamlSitemapProvider.class, YamlModelListener.class })
public class YamlSitemapProvider extends AbstractProvider<Sitemap>
        implements SitemapProvider, YamlModelListener<YamlSitemapDTO> {

    private final Logger logger = LoggerFactory.getLogger(YamlSitemapProvider.class);

    private SitemapFactory sitemapFactory;

    private final Map<String, Collection<Sitemap>> sitemapsMap = new ConcurrentHashMap<>();

    @Activate
    public YamlSitemapProvider(final @Reference SitemapFactory sitemapFactory) {
        this.sitemapFactory = sitemapFactory;
    }

    @Deactivate
    public void deactivate() {
        sitemapsMap.clear();
    }

    @Override
    public Collection<Sitemap> getAll() {
        // Ignore isolated models
        return sitemapsMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> sitemapsMap.getOrDefault(name, List.of())).flatMap(list -> list.stream()).toList();
    }

    public Collection<Sitemap> getAllFromModel(String modelName) {
        return sitemapsMap.getOrDefault(modelName, List.of());
    }

    @Override
    public Class<YamlSitemapDTO> getElementClass() {
        return YamlSitemapDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlSitemapDTO> elements) {
        Map<Sitemap, YamlSitemapDTO> added = new LinkedHashMap<>();
        elements.forEach(elt -> {
            Sitemap sitemap = mapSitemap(elt);
            if (sitemap != null) {
                added.put(sitemap, elt);
            }
        });

        Collection<Sitemap> modelSitemaps = Objects
                .requireNonNull(sitemapsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelSitemaps.addAll(added.keySet());

        added.forEach((sitemap, sitemapDTO) -> {
            String name = sitemap.getName();
            logger.debug("model {} added sitemap {}", modelName, name);
            if (!isIsolatedModel(modelName)) {
                notifyListenersAboutAddedElement(sitemap);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlSitemapDTO> elements) {
        Map<Sitemap, YamlSitemapDTO> updated = new LinkedHashMap<>();
        elements.forEach(elt -> {
            Sitemap sitemap = mapSitemap(elt);
            if (sitemap != null) {
                updated.put(sitemap, elt);
            }
        });

        Collection<Sitemap> modelSitemaps = Objects
                .requireNonNull(sitemapsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach((sitemap, sitemapDTO) -> {
            String name = sitemap.getName();
            modelSitemaps.stream().filter(s -> s.getName().equals(name)).findFirst().ifPresentOrElse(oldSitemap -> {
                modelSitemaps.remove(oldSitemap);
                modelSitemaps.add(sitemap);
                logger.debug("model {} updated sitemap {}", modelName, name);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutUpdatedElement(oldSitemap, sitemap);
                }
            }, () -> {
                modelSitemaps.add(sitemap);
                logger.debug("model {} added sitemap {}", modelName, name);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutAddedElement(sitemap);
                }
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlSitemapDTO> elements) {
        Collection<Sitemap> modelSitemaps = sitemapsMap.getOrDefault(modelName, List.of());
        elements.stream().map(elt -> elt.name).forEach(name -> {
            modelSitemaps.stream().filter(s -> s.getName().equals(name)).findFirst().ifPresentOrElse(oldSitemap -> {
                modelSitemaps.remove(oldSitemap);
                logger.debug("model {} removed sitemap {}", modelName, name);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutRemovedElement(oldSitemap);
                }
            }, () -> logger.debug("model {} sitemap {} not found", modelName, name));
        });

        if (modelSitemaps.isEmpty()) {
            sitemapsMap.remove(modelName);
        }
    }

    private @Nullable Sitemap mapSitemap(YamlSitemapDTO sitemapDTO) {
        Sitemap sitemap = sitemapFactory.createSitemap(sitemapDTO.name);
        sitemap.setLabel(sitemapDTO.label);
        sitemap.setIcon(sitemapDTO.icon);
        List<Widget> widgets = sitemap.getWidgets();
        List<YamlWidgetDTO> widgetsDTO = sitemapDTO.widgets;
        if (widgetsDTO != null) {
            widgetsDTO.forEach(dto -> {
                Widget w = mapWidget(dto, sitemap);
                if (w != null) {
                    widgets.add(w);
                }
            });
        }
        return sitemap;
    }

    private @Nullable Widget mapWidget(YamlWidgetDTO widgetDTO, Parent parent) {
        Widget widget = sitemapFactory.createWidget(widgetDTO.type, parent);
        if (widget != null) {
            widget.setItem(widgetDTO.item);

            String label = null;
            if (widgetDTO.label instanceof YamlWidgetLabelDTO widgetLabel) {
                if (widgetLabel.format != null) {
                    if (widgetLabel.label == null) {
                        label = "[%s]".formatted(widgetLabel.format);
                    } else {
                        label = "%s [%s]".formatted(widgetLabel.label, widgetLabel.format);
                    }
                } else if (widgetLabel.label != null) {
                    label = widgetLabel.label;
                }
                addWidgetRules(widget.getLabelColor(), widgetLabel.labelColor, false);
                addWidgetRules(widget.getValueColor(), widgetLabel.valueColor, false);
            }
            widget.setLabel(label);

            String icon = null;
            Boolean staticIcon = null;
            if (widgetDTO.icon instanceof YamlWidgetIconDTO widgetIcon) {
                if (widgetIcon.name instanceof String name) {
                    icon = name;
                    staticIcon = widgetIcon.staticIcon;
                } else {
                    addWidgetRules(widget.getIconRules(), widgetIcon.name, false);
                }
                addWidgetRules(widget.getIconColor(), widgetIcon.color, false);
            }
            widget.setIcon(icon);
            widget.setStaticIcon(staticIcon);

            addWidgetRules(widget.getVisibility(), widgetDTO.visibility, true);

            switch (widget) {
                case Image imageWidget:
                    imageWidget.setUrl(widgetDTO.url);
                    imageWidget.setRefresh(widgetDTO.refresh);
                    break;
                case Video videoWidget:
                    videoWidget.setUrl(widgetDTO.url);
                    videoWidget.setEncoding(widgetDTO.encoding);
                    break;
                case Chart chartWidget:
                    chartWidget.setService(widgetDTO.service);
                    chartWidget.setRefresh(widgetDTO.refresh);
                    chartWidget.setPeriod(widgetDTO.period);
                    chartWidget.setLegend(widgetDTO.legend);
                    chartWidget.setForceAsItem(widgetDTO.forceAsItem);
                    chartWidget.setYAxisDecimalPattern(widgetDTO.yAxisDecimalPattern);
                    chartWidget.setInterpolation(widgetDTO.interpolation);
                    break;
                case Webview webviewWidget:
                    webviewWidget.setHeight(widgetDTO.height);
                    webviewWidget.setUrl(widgetDTO.url);
                    break;
                case Switch switchWidget:
                    addWidgetMappings(switchWidget.getMappings(), widgetDTO.mappings);
                    break;
                case Mapview mapviewWidget:
                    mapviewWidget.setHeight(widgetDTO.height);
                    break;
                case Slider sliderWidget:
                    sliderWidget.setMinValue(widgetDTO.min);
                    sliderWidget.setMaxValue(widgetDTO.max);
                    sliderWidget.setStep(widgetDTO.step);
                    sliderWidget.setSwitchEnabled(widgetDTO.switchSupport);
                    sliderWidget.setReleaseOnly(widgetDTO.releaseOnly);
                    break;
                case Selection selectionWidget:
                    addWidgetMappings(selectionWidget.getMappings(), widgetDTO.mappings);
                    break;
                case Input inputWidget:
                    inputWidget.setInputHint(widgetDTO.hint);
                    break;
                case Setpoint setpointWidget:
                    setpointWidget.setMinValue(widgetDTO.min);
                    setpointWidget.setMaxValue(widgetDTO.max);
                    setpointWidget.setStep(widgetDTO.step);
                    break;
                case Colortemperaturepicker colortemperaturepickerWidget:
                    colortemperaturepickerWidget.setMinValue(widgetDTO.min);
                    colortemperaturepickerWidget.setMaxValue(widgetDTO.max);
                    break;
                case Button buttonWidget:
                    buttonWidget.setRow(widgetDTO.row);
                    buttonWidget.setColumn(widgetDTO.column);
                    buttonWidget.setStateless(widgetDTO.stateless);
                    buttonWidget.setCmd(widgetDTO.command);
                    buttonWidget.setReleaseCmd(widgetDTO.releaseCommand);
                    break;
                case Default defaultWidget:
                    defaultWidget.setHeight(widgetDTO.height);
                    break;
                default:
                    break;
            }

            if (widgetDTO.widgets != null && widget instanceof LinkableWidget linkableWidget) {
                List<Widget> widgets = linkableWidget.getWidgets();
                widgetDTO.widgets.forEach(dto -> {
                    Widget w = mapWidget(dto, linkableWidget);
                    if (w != null) {
                        widgets.add(w);
                    }
                });
            }
        }
        return widget;
    }

    private void addWidgetMappings(List<Mapping> mappings, @Nullable List<YamlMappingDTO> mappingsDTO) {
        if (mappingsDTO != null) {
            mappingsDTO.forEach(dto -> {
                Mapping mapping = sitemapFactory.createMapping();
                mapping.setCmd(dto.command);
                mapping.setReleaseCmd(dto.releaseCommand);
                mapping.setLabel(dto.label);
                mapping.setIcon(dto.icon);
                mappings.add(mapping);
            });
        }
    }

    private void addWidgetRules(List<Rule> rules, @Nullable Object rulesDTO, boolean ignoreValue) {
        if (rulesDTO instanceof String value) {
            Rule rule = sitemapFactory.createRule();
            if (!ignoreValue) {
                rule.setArgument(value);
            }
            rules.add(rule);
        } else if (rulesDTO instanceof YamlRuleWithAndConditionsDTO ruleDTO) {
            Rule rule = sitemapFactory.createRule();
            addRuleConditions(rule.getConditions(), ruleDTO.and);
            if (!ignoreValue) {
                rule.setArgument(ruleDTO.value);
            }
            rules.add(rule);
        } else if (rulesDTO instanceof YamlRuleWithUniqueConditionDTO ruleDTO) {
            Rule rule = sitemapFactory.createRule();
            if (ruleDTO.argument != null) {
                addRuleConditions(rule.getConditions(), List.of(ruleDTO));
            }
            if (!ignoreValue) {
                rule.setArgument(ruleDTO.value);
            }
            rules.add(rule);
        } else if (rulesDTO instanceof List<?> dtos) {
            for (Object dto : dtos) {
                if (dto instanceof YamlRuleWithAndConditionsDTO ruleDTO) {
                    Rule rule = sitemapFactory.createRule();
                    addRuleConditions(rule.getConditions(), ruleDTO.and);
                    if (!ignoreValue) {
                        rule.setArgument(ruleDTO.value);
                    }
                    rules.add(rule);
                } else if (dto instanceof YamlRuleWithUniqueConditionDTO ruleDTO) {
                    Rule rule = sitemapFactory.createRule();
                    if (ruleDTO.argument != null) {
                        addRuleConditions(rule.getConditions(), List.of(ruleDTO));
                    }
                    if (!ignoreValue) {
                        rule.setArgument(ruleDTO.value);
                    }
                    rules.add(rule);
                }
            }
        }
    }

    private void addRuleConditions(List<Condition> conditions, @Nullable List<YamlConditionDTO> conditionsDTO) {
        if (conditionsDTO != null) {
            conditionsDTO.forEach(dto -> {
                Condition condition = sitemapFactory.createCondition();
                condition.setItem(dto.item);
                condition.setCondition(dto.operator);
                condition.setValue(dto.argument);
                conditions.add(condition);
            });
        }
    }

    @Override
    public @Nullable Sitemap getSitemap(String sitemapName) {
        Sitemap sitemap = sitemapsMap.entrySet().stream().filter(e -> !isIsolatedModel(e.getKey()))
                .flatMap(e -> e.getValue().stream()).filter(s -> sitemapName.equals(s.getName())).findAny()
                .orElse(null);
        if (sitemap == null) {
            logger.trace("Sitemap {} cannot be found", sitemapName);
        }
        return sitemap;
    }

    @Override
    public Set<String> getSitemapNames() {
        return getAll().stream().map(s -> s.getName()).collect(Collectors.toSet());
    }
}
