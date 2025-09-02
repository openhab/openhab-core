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
package org.openhab.core.model.sitemap.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.sitemap.sitemap.ModelButton;
import org.openhab.core.model.sitemap.sitemap.ModelButtonDefinition;
import org.openhab.core.model.sitemap.sitemap.ModelButtonDefinitionList;
import org.openhab.core.model.sitemap.sitemap.ModelButtongrid;
import org.openhab.core.model.sitemap.sitemap.ModelChart;
import org.openhab.core.model.sitemap.sitemap.ModelColorArray;
import org.openhab.core.model.sitemap.sitemap.ModelColorArrayList;
import org.openhab.core.model.sitemap.sitemap.ModelColortemperaturepicker;
import org.openhab.core.model.sitemap.sitemap.ModelCondition;
import org.openhab.core.model.sitemap.sitemap.ModelDefault;
import org.openhab.core.model.sitemap.sitemap.ModelIconRule;
import org.openhab.core.model.sitemap.sitemap.ModelIconRuleList;
import org.openhab.core.model.sitemap.sitemap.ModelImage;
import org.openhab.core.model.sitemap.sitemap.ModelInput;
import org.openhab.core.model.sitemap.sitemap.ModelLinkableWidget;
import org.openhab.core.model.sitemap.sitemap.ModelMapping;
import org.openhab.core.model.sitemap.sitemap.ModelMappingList;
import org.openhab.core.model.sitemap.sitemap.ModelMapview;
import org.openhab.core.model.sitemap.sitemap.ModelSelection;
import org.openhab.core.model.sitemap.sitemap.ModelSetpoint;
import org.openhab.core.model.sitemap.sitemap.ModelSitemap;
import org.openhab.core.model.sitemap.sitemap.ModelSlider;
import org.openhab.core.model.sitemap.sitemap.ModelSwitch;
import org.openhab.core.model.sitemap.sitemap.ModelVideo;
import org.openhab.core.model.sitemap.sitemap.ModelVisibilityRule;
import org.openhab.core.model.sitemap.sitemap.ModelVisibilityRuleList;
import org.openhab.core.model.sitemap.sitemap.ModelWebview;
import org.openhab.core.model.sitemap.sitemap.ModelWidget;
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
import org.openhab.core.sitemap.registry.SitemapProvider;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides access to the sitemap model files.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - Separate registry from model
 */
@NonNullByDefault
@Component(service = SitemapProvider.class, immediate = true)
public class SitemapProviderImpl extends AbstractProvider<Sitemap>
        implements SitemapProvider, ModelRepositoryChangeListener {

    private static final String SITEMAP_MODEL_NAME = "sitemap";
    protected static final String SITEMAP_FILEEXT = "." + SITEMAP_MODEL_NAME;
    protected static final String MODEL_TYPE_PREFIX = "Model";

    private final Logger logger = LoggerFactory.getLogger(SitemapProviderImpl.class);

    private final ModelRepository modelRepo;
    private final SitemapRegistry sitemapRegistry;
    private final SitemapFactory sitemapFactory;

    private final Map<String, Sitemap> sitemapCache = new ConcurrentHashMap<>();

    @Activate
    public SitemapProviderImpl(final @Reference ModelRepository modelRepo,
            final @Reference SitemapRegistry sitemapRegistry, final @Reference SitemapFactory sitemapFactory) {
        this.modelRepo = modelRepo;
        this.sitemapRegistry = sitemapRegistry;
        this.sitemapFactory = sitemapFactory;
        refreshSitemapModels();
        sitemapRegistry.addSitemapProvider(this);
        modelRepo.addModelRepositoryChangeListener(this);
    }

    @Deactivate
    protected void deactivate() {
        modelRepo.removeModelRepositoryChangeListener(this);
        sitemapRegistry.removeSitemapProvider(this);
        sitemapCache.clear();
    }

    @Override
    public @Nullable Sitemap getSitemap(String sitemapName) {
        Sitemap sitemap = sitemapCache.get(sitemapName);
        if (sitemap == null) {
            logger.trace("Sitemap {} cannot be found", sitemapName);
        }
        return sitemap;
    }

    private void refreshSitemapModels() {
        sitemapCache.clear();
        Iterable<String> sitemapFilenames = modelRepo.getAllModelNamesOfType(SITEMAP_MODEL_NAME);
        for (String filename : sitemapFilenames) {
            ModelSitemap modelSitemap = (ModelSitemap) modelRepo.getModel(filename);
            if (modelSitemap != null) {
                String sitemapName = filename.substring(0, filename.length() - SITEMAP_FILEEXT.length());
                if (!modelSitemap.getName().equals(sitemapName)) {
                    logger.warn(
                            "Filename `{}` does not match the name `{}` of the sitemap - please fix this as you might see unexpected behavior otherwise.",
                            filename, modelSitemap.getName());
                }
                Sitemap sitemap = parseModelSitemap(modelSitemap);
                sitemapCache.put(sitemapName, sitemap);
            }
        }
    }

    private Sitemap parseModelSitemap(ModelSitemap modelSitemap) {
        Sitemap sitemap = sitemapFactory.createSitemap(modelSitemap.getName());
        sitemap.setLabel(modelSitemap.getLabel());
        sitemap.setIcon(modelSitemap.getIcon());
        List<Widget> widgets = sitemap.getWidgets();
        modelSitemap.getChildren().forEach(child -> addWidget(widgets, child, sitemap));
        return sitemap;
    }

    private void addWidget(List<Widget> widgets, ModelWidget modelWidget, Parent parent) {
        String widgetType = getWidgetType(modelWidget);
        Widget widget = sitemapFactory.createWidget(widgetType, parent);
        if (widget != null) {
            switch (widget) {
                case Image imageWidget:
                    ModelImage modelImage = (ModelImage) modelWidget;
                    imageWidget.setUrl(modelImage.getUrl());
                    imageWidget.setRefresh(modelImage.getRefresh());
                    break;
                case Video videoWidget:
                    ModelVideo modelVideo = (ModelVideo) modelWidget;
                    videoWidget.setUrl(modelVideo.getUrl());
                    videoWidget.setEncoding(modelVideo.getEncoding());
                    break;
                case Chart chartWidget:
                    ModelChart modelChart = (ModelChart) modelWidget;
                    chartWidget.setService(modelChart.getService());
                    chartWidget.setRefresh(modelChart.getRefresh());
                    chartWidget.setPeriod(modelChart.getPeriod());
                    chartWidget.setLegend(modelChart.getLegend());
                    chartWidget.setForceAsItem(modelChart.getForceAsItem());
                    chartWidget.setYAxisDecimalPattern(modelChart.getYAxisDecimalPattern());
                    chartWidget.setInterpolation(modelChart.getInterpolation());
                    break;
                case Webview webviewWidget:
                    ModelWebview modelWebview = (ModelWebview) modelWidget;
                    webviewWidget.setHeight(modelWebview.getHeight());
                    webviewWidget.setUrl(modelWebview.getUrl());
                    break;
                case Switch switchWidget:
                    ModelSwitch modelSwitch = (ModelSwitch) modelWidget;
                    addWidgetMappings(switchWidget.getMappings(), modelSwitch.getMappings());
                    break;
                case Mapview mapviewWidget:
                    ModelMapview modelMapview = (ModelMapview) modelWidget;
                    mapviewWidget.setHeight(modelMapview.getHeight());
                    break;
                case Slider sliderWidget:
                    ModelSlider modelSlider = (ModelSlider) modelWidget;
                    sliderWidget.setMinValue(modelSlider.getMinValue());
                    sliderWidget.setMaxValue(modelSlider.getMaxValue());
                    sliderWidget.setStep(modelSlider.getStep());
                    sliderWidget.setSwitchEnabled(modelSlider.isSwitchEnabled());
                    sliderWidget.setReleaseOnly(modelSlider.isReleaseOnly());
                    break;
                case Selection selectionWidget:
                    ModelSelection modelSelection = (ModelSelection) modelWidget;
                    addWidgetMappings(selectionWidget.getMappings(), modelSelection.getMappings());
                    break;
                case Input inputWidget:
                    ModelInput modelInput = (ModelInput) modelWidget;
                    inputWidget.setInputHint(modelInput.getInputHint());
                    break;
                case Setpoint setpointWidget:
                    ModelSetpoint modelSetpoint = (ModelSetpoint) modelWidget;
                    setpointWidget.setMinValue(modelSetpoint.getMinValue());
                    setpointWidget.setMaxValue(modelSetpoint.getMaxValue());
                    setpointWidget.setStep(modelSetpoint.getStep());
                    break;
                case Colortemperaturepicker colortemperaturepickerWidget:
                    ModelColortemperaturepicker modelColortemperaturepicker = (ModelColortemperaturepicker) modelWidget;
                    colortemperaturepickerWidget.setMinValue(modelColortemperaturepicker.getMinValue());
                    colortemperaturepickerWidget.setMaxValue(modelColortemperaturepicker.getMaxValue());
                    break;
                case Buttongrid buttongridWidget:
                    ModelButtongrid modelButtongrid = (ModelButtongrid) modelWidget;
                    addWidgetButtons(buttongridWidget.getButtons(), modelButtongrid.getButtons());
                    break;
                case Button buttonWidget:
                    ModelButton modelButton = (ModelButton) modelWidget;
                    buttonWidget.setRow(modelButton.getRow());
                    buttonWidget.setColumn(modelButton.getColumn());
                    buttonWidget.setStateless(modelButton.isStateless());
                    buttonWidget.setCmd(modelButton.getCmd());
                    buttonWidget.setReleaseCmd(modelButton.getReleaseCmd());
                    break;
                case Default defaultWidget:
                    ModelDefault modelDefault = (ModelDefault) modelWidget;
                    defaultWidget.setHeight(modelDefault.getHeight());
                    break;
                default:
                    break;
            }

            widget.setItem(modelWidget.getItem());
            widget.setLabel(modelWidget.getLabel());
            String staticIcon = modelWidget.getStaticIcon();
            if (staticIcon != null && !staticIcon.isEmpty()) {
                widget.setIcon(staticIcon);
                widget.setStaticIcon(true);
            } else {
                widget.setIcon(modelWidget.getIcon());
            }

            if (modelWidget instanceof ModelLinkableWidget modelLinkableWidget) {
                LinkableWidget linkableWidget = (LinkableWidget) widget;
                List<Widget> childWidgets = linkableWidget.getWidgets();
                modelLinkableWidget.getChildren()
                        .forEach(childModelWidget -> addWidget(childWidgets, childModelWidget, linkableWidget));
            }

            addWidgetVisibilityRules(widget.getVisibility(), modelWidget.getVisibility());
            addWidgetColorRules(widget.getLabelColor(), modelWidget.getLabelColor());
            addWidgetColorRules(widget.getValueColor(), modelWidget.getValueColor());
            addWidgetColorRules(widget.getIconColor(), modelWidget.getIconColor());
            addWidgetIconRules(widget.getIconRules(), modelWidget.getIconRules());

            widgets.add(widget);
        }
    }

    private String getWidgetType(ModelWidget modelWidget) {
        String instanceTypeName = modelWidget.eClass().getInstanceTypeName();
        String widgetType = instanceTypeName
                .substring(instanceTypeName.lastIndexOf("." + MODEL_TYPE_PREFIX) + MODEL_TYPE_PREFIX.length() + 1);
        return widgetType;
    }

    private void addWidgetMappings(List<Mapping> mappings, @Nullable ModelMappingList modelMappingList) {
        if (modelMappingList != null) {
            EList<ModelMapping> modelMappings = modelMappingList.getElements();
            modelMappings.forEach(modelMapping -> {
                Mapping mapping = sitemapFactory.createMapping();
                mapping.setCmd(modelMapping.getCmd());
                mapping.setReleaseCmd(modelMapping.getReleaseCmd());
                mapping.setLabel(modelMapping.getLabel());
                mapping.setIcon(modelMapping.getIcon());
                mappings.add(mapping);
            });
        }
    }

    private void addWidgetButtons(List<ButtonDefinition> buttons, @Nullable ModelButtonDefinitionList modelButtonList) {
        if (modelButtonList != null) {
            EList<ModelButtonDefinition> modelButtons = modelButtonList.getElements();
            modelButtons.forEach(modelButton -> {
                ButtonDefinition button = sitemapFactory.createButtonDefinition();
                button.setRow(modelButton.getRow());
                button.setColumn(modelButton.getColumn());
                button.setCmd(modelButton.getCmd());
                button.setLabel(modelButton.getLabel());
                button.setIcon(modelButton.getIcon());
                buttons.add(button);
            });
        }
    }

    private void addWidgetVisibilityRules(List<Rule> visibilityRules,
            @Nullable ModelVisibilityRuleList modelVisibilityRuleList) {
        if (modelVisibilityRuleList != null) {
            EList<ModelVisibilityRule> modelVisibilityRules = modelVisibilityRuleList.getElements();
            modelVisibilityRules.forEach(modelVisibilityRule -> {
                Rule visibilityRule = sitemapFactory.createRule();
                addRuleConditions(visibilityRule.getConditions(), modelVisibilityRule.getConditions());
                visibilityRules.add(visibilityRule);
            });
        }
    }

    private void addWidgetColorRules(List<Rule> colorRules, @Nullable ModelColorArrayList modelColorRuleList) {
        if (modelColorRuleList != null) {
            EList<ModelColorArray> modelColorRules = modelColorRuleList.getElements();
            modelColorRules.forEach(modelColorRule -> {
                Rule colorRule = sitemapFactory.createRule();
                addRuleConditions(colorRule.getConditions(), modelColorRule.getConditions());
                colorRules.add(colorRule);
            });
        }
    }

    private void addWidgetIconRules(List<Rule> iconRules, @Nullable ModelIconRuleList modelIconRuleList) {
        if (modelIconRuleList != null) {
            EList<ModelIconRule> modelIconRules = modelIconRuleList.getElements();
            modelIconRules.forEach(modelIconRule -> {
                Rule iconRule = sitemapFactory.createRule();
                iconRule.setArgument(modelIconRule.getArg());
                addRuleConditions(iconRule.getConditions(), modelIconRule.getConditions());
                iconRules.add(iconRule);
            });
        }
    }

    private void addRuleConditions(List<Condition> conditions, EList<ModelCondition> modelConditions) {
        modelConditions.forEach(modelCondition -> {
            Condition condition = sitemapFactory.createCondition();
            condition.setItem(modelCondition.getItem());
            condition.setCondition(modelCondition.getCondition());
            String sign = modelCondition.getSign();
            String value = (sign != null ? sign : "") + modelCondition.getState();
            condition.setValue(value);
            conditions.add(condition);
        });
    }

    @Override
    public Set<String> getSitemapNames() {
        return sitemapCache.keySet();
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (!modelName.endsWith(SITEMAP_FILEEXT)) {
            return;
        }

        Sitemap sitemap = null;
        String sitemapName = modelName.substring(0, modelName.length() - SITEMAP_FILEEXT.length());
        Sitemap oldSitemap = sitemapRegistry.get(sitemapName);

        if (type == EventType.REMOVED) {
            sitemapCache.remove(sitemapName);
        } else {
            EObject modelSitemapObject = modelRepo.getModel(modelName);
            // if the sitemap file is empty it will not be in the repo and thus there is no need to cache it here
            if (modelSitemapObject instanceof ModelSitemap modelSitemap) {
                sitemap = parseModelSitemap(modelSitemap);
                sitemapCache.put(sitemapName, sitemap);
            }
        }

        switch (type) {
            case EventType.ADDED:
                if (sitemap != null) {
                    notifyListenersAboutAddedElement(sitemap);
                }
                break;
            case EventType.REMOVED:
                if (oldSitemap != null) {
                    notifyListenersAboutRemovedElement(oldSitemap);
                }
                break;
            case EventType.MODIFIED:
                if (sitemap != null && oldSitemap != null) {
                    notifyListenersAboutUpdatedElement(oldSitemap, sitemap);
                }
                break;
        }
    }

    @Override
    public Collection<Sitemap> getAll() {
        return sitemapCache.values();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Sitemap> listener) {
        super.addProviderChangeListener(listener);
        getAll().forEach(sitemap -> {
            notifyListenersAboutAddedElement(sitemap);
        });
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Sitemap> listener) {
        super.removeProviderChangeListener(listener);
        getAll().forEach(sitemap -> {
            notifyListenersAboutRemovedElement(sitemap);
        });
    }
}
