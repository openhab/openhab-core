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
package org.openhab.core.model.sitemap.internal.fileconverter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.sitemap.internal.DslSitemapProvider;
import org.openhab.core.model.sitemap.sitemap.ModelButton;
import org.openhab.core.model.sitemap.sitemap.ModelButtongrid;
import org.openhab.core.model.sitemap.sitemap.ModelChart;
import org.openhab.core.model.sitemap.sitemap.ModelColorArray;
import org.openhab.core.model.sitemap.sitemap.ModelColorArrayList;
import org.openhab.core.model.sitemap.sitemap.ModelColorpicker;
import org.openhab.core.model.sitemap.sitemap.ModelColortemperaturepicker;
import org.openhab.core.model.sitemap.sitemap.ModelCondition;
import org.openhab.core.model.sitemap.sitemap.ModelDefault;
import org.openhab.core.model.sitemap.sitemap.ModelFrame;
import org.openhab.core.model.sitemap.sitemap.ModelGroup;
import org.openhab.core.model.sitemap.sitemap.ModelIconRule;
import org.openhab.core.model.sitemap.sitemap.ModelIconRuleList;
import org.openhab.core.model.sitemap.sitemap.ModelImage;
import org.openhab.core.model.sitemap.sitemap.ModelInput;
import org.openhab.core.model.sitemap.sitemap.ModelLinkableWidget;
import org.openhab.core.model.sitemap.sitemap.ModelMapping;
import org.openhab.core.model.sitemap.sitemap.ModelMappingList;
import org.openhab.core.model.sitemap.sitemap.ModelMapview;
import org.openhab.core.model.sitemap.sitemap.ModelNestedSitemap;
import org.openhab.core.model.sitemap.sitemap.ModelSelection;
import org.openhab.core.model.sitemap.sitemap.ModelSetpoint;
import org.openhab.core.model.sitemap.sitemap.ModelSitemap;
import org.openhab.core.model.sitemap.sitemap.ModelSlider;
import org.openhab.core.model.sitemap.sitemap.ModelSwitch;
import org.openhab.core.model.sitemap.sitemap.ModelText;
import org.openhab.core.model.sitemap.sitemap.ModelVideo;
import org.openhab.core.model.sitemap.sitemap.ModelVisibilityRule;
import org.openhab.core.model.sitemap.sitemap.ModelVisibilityRuleList;
import org.openhab.core.model.sitemap.sitemap.ModelWebview;
import org.openhab.core.model.sitemap.sitemap.ModelWidget;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.SitemapModel;
import org.openhab.core.sitemap.Button;
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
import org.openhab.core.sitemap.NestedSitemap;
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
import org.openhab.core.sitemap.fileconverter.SitemapParser;
import org.openhab.core.sitemap.fileconverter.SitemapSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SitemapSerializer} is the DSL file converter for {@link Sitemap} object
 * with the capabilities of parsing and generating file.
 *
 * @author Mark Herwege - Initial contribution
 * @author Mark Herwege - Add support for nested sitemaps
 */
@NonNullByDefault
@Component(immediate = true, service = { SitemapSerializer.class, SitemapParser.class })
public class DslSitemapConverter implements SitemapSerializer, SitemapParser {

    private final Logger logger = LoggerFactory.getLogger(DslSitemapConverter.class);

    private final ModelRepository modelRepository;
    private final DslSitemapProvider sitemapProvider;

    private final Map<String, SitemapModel> elementsToGenerate = new ConcurrentHashMap<>();

    @Activate
    public DslSitemapConverter(final @Reference ModelRepository modelRepository,
            final @Reference DslSitemapProvider sitemapProvider) {
        this.modelRepository = modelRepository;
        this.sitemapProvider = sitemapProvider;
    }

    @Override
    public String getGeneratedFormat() {
        return "DSL";
    }

    @Override
    public void setSitemapsToBeSerialized(String id, List<Sitemap> sitemaps) {
        if (sitemaps.isEmpty()) {
            return;
        }
        SitemapModel model = SitemapFactory.eINSTANCE.createSitemapModel();
        for (Sitemap sitemap : sitemaps) {
            model.getSitemaps().add(buildModelSitemap(sitemap));
        }
        elementsToGenerate.put(id, model);
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        SitemapModel model = elementsToGenerate.remove(id);
        if (model != null) {
            modelRepository.generateFileFormat(out, "sitemaps", model);
        }
    }

    private ModelSitemap buildModelSitemap(Sitemap sitemap) {
        ModelSitemap model = SitemapFactory.eINSTANCE.createModelSitemap();
        model.setName(sitemap.getName());
        model.setLabel(sitemap.getLabel());
        model.setIcon(sitemap.getIcon());
        EList<ModelWidget> childModelWidgets = model.getChildren();
        sitemap.getWidgets().forEach(childWidget -> {
            ModelWidget childModelWidget = buildModelWidget(childWidget);
            childModelWidgets.add(childModelWidget);
        });
        return model;
    }

    private ModelWidget buildModelWidget(Widget widget) {
        ModelWidget modelWidget;
        switch (widget) {
            case Frame frameWidget -> {
                ModelFrame modelFrame = SitemapFactory.eINSTANCE.createModelFrame();
                modelWidget = modelFrame;
            }
            case Text textWidget -> {
                ModelText modelText = SitemapFactory.eINSTANCE.createModelText();
                modelWidget = modelText;
            }
            case Group groupWidget -> {
                ModelGroup modelGroup = SitemapFactory.eINSTANCE.createModelGroup();
                modelWidget = modelGroup;
            }
            case Switch switchWidget -> {
                ModelSwitch modelSwitch = SitemapFactory.eINSTANCE.createModelSwitch();
                List<Mapping> mappings = switchWidget.getMappings();
                if (!mappings.isEmpty()) {
                    modelSwitch.setMappings(modelMappings(mappings));
                }
                modelWidget = modelSwitch;
            }
            case Buttongrid buttongridWidget -> {
                ModelButtongrid modelButtongrid = SitemapFactory.eINSTANCE.createModelButtongrid();
                modelWidget = modelButtongrid;
            }
            case Button buttonWidget -> {
                ModelButton modelButton = SitemapFactory.eINSTANCE.createModelButton();
                modelButton.setRow(buttonWidget.getRow());
                modelButton.setColumn(buttonWidget.getColumn());
                modelButton.setCmd(buttonWidget.getCmd());
                modelButton.setReleaseCmd(buttonWidget.getReleaseCmd());
                modelButton.setStateless(buttonWidget.isStateless());
                modelWidget = modelButton;
            }
            case Selection selectionWidget -> {
                ModelSelection modelSelection = SitemapFactory.eINSTANCE.createModelSelection();
                List<Mapping> mappings = selectionWidget.getMappings();
                if (!mappings.isEmpty()) {
                    modelSelection.setMappings(modelMappings(mappings));
                }
                modelWidget = modelSelection;
            }
            case Setpoint setpointWidget -> {
                ModelSetpoint modelSetpoint = SitemapFactory.eINSTANCE.createModelSetpoint();
                modelSetpoint.setMinValue(setpointWidget.getMinValue());
                modelSetpoint.setMaxValue(setpointWidget.getMaxValue());
                modelSetpoint.setStep(setpointWidget.getStep());
                modelWidget = modelSetpoint;
            }
            case Slider sliderWidget -> {
                ModelSlider modelSlider = SitemapFactory.eINSTANCE.createModelSlider();
                modelSlider.setMinValue(sliderWidget.getMinValue());
                modelSlider.setMaxValue(sliderWidget.getMaxValue());
                modelSlider.setStep(sliderWidget.getStep());
                modelSlider.setSwitchEnabled(sliderWidget.isSwitchEnabled());
                modelSlider.setReleaseOnly(sliderWidget.isReleaseOnly());
                modelWidget = modelSlider;
            }
            case Colorpicker colorpickerWidget -> {
                ModelColorpicker modelColorpicker = SitemapFactory.eINSTANCE.createModelColorpicker();
                modelWidget = modelColorpicker;
            }
            case Colortemperaturepicker colortemperaturepickerWidget -> {
                ModelColortemperaturepicker modelColortemperaturepicker = SitemapFactory.eINSTANCE
                        .createModelColortemperaturepicker();
                modelColortemperaturepicker.setMinValue(colortemperaturepickerWidget.getMinValue());
                modelColortemperaturepicker.setMaxValue(colortemperaturepickerWidget.getMaxValue());
                modelWidget = modelColortemperaturepicker;
            }
            case Input inputWidget -> {
                ModelInput modelInput = SitemapFactory.eINSTANCE.createModelInput();
                modelInput.setInputHint(inputWidget.getInputHint());
                modelWidget = modelInput;
            }
            case Webview webviewWidget -> {
                ModelWebview modelWebview = SitemapFactory.eINSTANCE.createModelWebview();
                modelWebview.setUrl(webviewWidget.getUrl());
                modelWebview.setHeight(webviewWidget.getHeight());
                modelWidget = modelWebview;
            }
            case Mapview mapviewWidget -> {
                ModelMapview modelMapview = SitemapFactory.eINSTANCE.createModelMapview();
                modelMapview.setHeight(mapviewWidget.getHeight());
                modelWidget = modelMapview;
            }
            case Image imageWidget -> {
                ModelImage modelImage = SitemapFactory.eINSTANCE.createModelImage();
                modelImage.setUrl(imageWidget.getUrl());
                modelImage.setRefresh(imageWidget.getRefresh());
                modelWidget = modelImage;
            }
            case Video videoWidget -> {
                ModelVideo modelVideo = SitemapFactory.eINSTANCE.createModelVideo();
                modelVideo.setUrl(videoWidget.getUrl());
                modelVideo.setEncoding(videoWidget.getEncoding());
                modelWidget = modelVideo;
            }
            case Chart chartWidget -> {
                ModelChart modelChart = SitemapFactory.eINSTANCE.createModelChart();
                modelChart.setRefresh(chartWidget.getRefresh());
                modelChart.setPeriod(chartWidget.getPeriod());
                modelChart.setService(chartWidget.getService());
                modelChart.setLegend(chartWidget.hasLegend());
                if (chartWidget.forceAsItem()) {
                    modelChart.setForceAsItem(chartWidget.forceAsItem());
                }
                modelChart.setYAxisDecimalPattern(chartWidget.getYAxisDecimalPattern());
                modelChart.setInterpolation(chartWidget.getInterpolation());
                modelWidget = modelChart;
            }
            case NestedSitemap nestedSitemapWidget -> {
                ModelNestedSitemap modelNestedSitemap = SitemapFactory.eINSTANCE.createModelNestedSitemap();
                modelNestedSitemap.setSitemapName(nestedSitemapWidget.getSitemapName());
                modelWidget = modelNestedSitemap;
            }
            default -> {
                ModelDefault modelDefault = SitemapFactory.eINSTANCE.createModelDefault();
                if (widget instanceof Default defaultWidget) {
                    modelDefault.setHeight(defaultWidget.getHeight());
                }
                modelWidget = modelDefault;
            }
        }

        modelWidget.setItem(widget.getItem());
        modelWidget.setLabel(widget.getLabel());
        String icon = widget.getIcon();
        if (widget.isStaticIcon()) {
            modelWidget.setStaticIcon(icon);
        } else {
            modelWidget.setIcon(icon);
        }

        List<Rule> iconRules = widget.getIconRules();
        if (!iconRules.isEmpty()) {
            modelWidget.setIconRules(modelIconRules(iconRules));
        }
        List<Rule> visibilityRules = widget.getVisibility();
        if (!visibilityRules.isEmpty()) {
            modelWidget.setVisibility(modelVisibilityRules(visibilityRules));
        }
        List<Rule> labelColorRules = widget.getLabelColor();
        if (!labelColorRules.isEmpty()) {
            modelWidget.setLabelColor(modelColorRules(labelColorRules));
        }
        List<Rule> valueColorRules = widget.getValueColor();
        if (!valueColorRules.isEmpty()) {
            modelWidget.setValueColor(modelColorRules(valueColorRules));
        }
        List<Rule> iconColorRules = widget.getIconColor();
        if (!iconColorRules.isEmpty()) {
            modelWidget.setIconColor(modelColorRules(iconColorRules));
        }

        if (widget instanceof LinkableWidget linkableWidget
                && modelWidget instanceof ModelLinkableWidget modelLinkableWidget) {
            EList<ModelWidget> childModelWidgets = modelLinkableWidget.getChildren();
            linkableWidget.getWidgets().forEach(childWidget -> {
                ModelWidget childModelWidget = buildModelWidget(childWidget);
                childModelWidgets.add(childModelWidget);
            });
        }
        return modelWidget;
    }

    private ModelMappingList modelMappings(List<Mapping> mappings) {
        ModelMappingList modelMappingList = SitemapFactory.eINSTANCE.createModelMappingList();
        EList<ModelMapping> modelMappings = modelMappingList.getElements();
        mappings.forEach(mapping -> {
            ModelMapping modelMapping = SitemapFactory.eINSTANCE.createModelMapping();
            modelMapping.setCmd(mapping.getCmd());
            modelMapping.setReleaseCmd(mapping.getReleaseCmd());
            modelMapping.setLabel(mapping.getLabel());
            modelMapping.setIcon(mapping.getIcon());
            modelMappings.add(modelMapping);
        });
        return modelMappingList;
    }

    private ModelIconRuleList modelIconRules(List<Rule> rules) {
        ModelIconRuleList modelRuleList = SitemapFactory.eINSTANCE.createModelIconRuleList();
        EList<ModelIconRule> modelRules = modelRuleList.getElements();
        rules.forEach(rule -> {
            ModelIconRule modelRule = SitemapFactory.eINSTANCE.createModelIconRule();
            EList<ModelCondition> modelConditions = modelRule.getConditions();
            List<Condition> conditions = rule.getConditions();
            setModelConditions(modelConditions, conditions);
            modelRule.setArg(rule.getArgument());
            modelRules.add(modelRule);
        });
        return modelRuleList;
    }

    private ModelVisibilityRuleList modelVisibilityRules(List<Rule> rules) {
        ModelVisibilityRuleList modelRuleList = SitemapFactory.eINSTANCE.createModelVisibilityRuleList();
        EList<ModelVisibilityRule> modelRules = modelRuleList.getElements();
        rules.forEach(rule -> {
            ModelVisibilityRule modelRule = SitemapFactory.eINSTANCE.createModelVisibilityRule();
            EList<ModelCondition> modelConditions = modelRule.getConditions();
            List<Condition> conditions = rule.getConditions();
            setModelConditions(modelConditions, conditions);
            modelRules.add(modelRule);
        });
        return modelRuleList;
    }

    private ModelColorArrayList modelColorRules(List<Rule> rules) {
        ModelColorArrayList modelRuleList = SitemapFactory.eINSTANCE.createModelColorArrayList();
        EList<ModelColorArray> modelRules = modelRuleList.getElements();
        rules.forEach(rule -> {
            ModelColorArray modelRule = SitemapFactory.eINSTANCE.createModelColorArray();
            EList<ModelCondition> modelConditions = modelRule.getConditions();
            List<Condition> conditions = rule.getConditions();
            setModelConditions(modelConditions, conditions);
            modelRule.setArg(rule.getArgument());
            modelRules.add(modelRule);
        });
        return modelRuleList;
    }

    private void setModelConditions(EList<ModelCondition> modelConditions, List<Condition> conditions) {
        conditions.forEach(condition -> {
            ModelCondition modelCondition = SitemapFactory.eINSTANCE.createModelCondition();
            modelCondition.setItem(condition.getItem());
            modelCondition.setCondition(condition.getCondition());
            String value = condition.getValue();
            if (value.length() > 1 && (value.startsWith("-") || value.startsWith("+"))) {
                modelCondition.setSign(value.substring(0, 1));
                value = value.substring(1);
            }
            modelCondition.setState(value);
            modelConditions.add(modelCondition);
        });
    }

    @Override
    public String getParserFormat() {
        return "DSL";
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel("sitemaps", inputStream, errors, warnings);
    }

    @Override
    public Collection<Sitemap> getParsedObjects(String modelName) {
        return sitemapProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeModel(modelName);
    }
}
