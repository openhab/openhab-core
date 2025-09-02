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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import org.openhab.core.model.sitemap.sitemap.ModelButtonDefinition;
import org.openhab.core.model.sitemap.sitemap.ModelButtonDefinitionList;
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
import org.openhab.core.sitemap.ButtonDefinition;
import org.openhab.core.sitemap.Buttongrid;
import org.openhab.core.sitemap.Chart;
import org.openhab.core.sitemap.Colorpicker;
import org.openhab.core.sitemap.Colortemperaturepicker;
import org.openhab.core.sitemap.Condition;
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
import org.openhab.core.sitemap.fileconverter.AbstractSitemapSerializer;
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
 */
@NonNullByDefault
@Component(immediate = true, service = { SitemapSerializer.class, SitemapParser.class })
public class DslSitemapConverter extends AbstractSitemapSerializer implements SitemapParser {

    private final Logger logger = LoggerFactory.getLogger(DslSitemapConverter.class);

    private final ModelRepository modelRepository;
    private final DslSitemapProvider sitemapProvider;

    private final Map<String, SitemapModel> elementsToGenerate = new ConcurrentHashMap<>();

    // private final ISerializer serializer;

    @Activate
    public DslSitemapConverter(final @Reference ModelRepository modelRepository,
            final @Reference DslSitemapProvider sitemapProvider) {
        this.modelRepository = modelRepository;
        this.sitemapProvider = sitemapProvider;
        // this.serializer = SitemapStandaloneSetup.doSetup().getInstance(ISerializer.class);
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
        if (sitemaps.size() > 1) {
            logger.warn("Only one sitemap at a time can be serialized to DSL");
        }
        SitemapModel model = buildModelSitemap(sitemaps.getFirst());
        elementsToGenerate.put(id, model);
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        SitemapModel model = elementsToGenerate.remove(id);
        if (model != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            modelRepository.generateFileFormat(outputStream, "sitemap", model);
            String syntax = new String(outputStream.toByteArray());
            try {
                out.write(syntax.getBytes());
            } catch (IOException e) {
                logger.warn("Exception when writing the generated syntax {}", e.getMessage());
            }
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
            case Frame frame -> {
                ModelFrame modelFrame = SitemapFactory.eINSTANCE.createModelFrame();
                modelWidget = modelFrame;
            }
            case Text text -> {
                ModelText modelText = SitemapFactory.eINSTANCE.createModelText();
                modelWidget = modelText;
            }
            case Group group -> {
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
            case Buttongrid buttongrid -> {
                ModelButtongrid modelButtongrid = SitemapFactory.eINSTANCE.createModelButtongrid();
                List<ButtonDefinition> buttons = buttongrid.getButtons();
                if (!buttons.isEmpty()) {
                    modelButtongrid.setButtons(modelButtons(buttons));
                }
                modelWidget = modelButtongrid;
            }
            case Button button -> {
                ModelButton modelButton = SitemapFactory.eINSTANCE.createModelButton();
                modelButton.setRow(button.getRow());
                modelButton.setColumn(button.getColumn());
                modelButton.setCmd(button.getCmd());
                String releaseCmd = button.getReleaseCmd();
                if (releaseCmd != null) {
                    modelButton.setReleaseCmd(releaseCmd);
                }
                if (button.isStateless()) {
                    modelButton.setStateless(true);
                }
                modelWidget = modelButton;
            }
            case Selection selection -> {
                ModelSelection modelSelection = SitemapFactory.eINSTANCE.createModelSelection();
                List<Mapping> mappings = selection.getMappings();
                if (!mappings.isEmpty()) {
                    modelSelection.setMappings(modelMappings(mappings));
                }
                modelWidget = modelSelection;
            }
            case Setpoint setpoint -> {
                ModelSetpoint modelSetpoint = SitemapFactory.eINSTANCE.createModelSetpoint();
                BigDecimal minValue = setpoint.getMinValue();
                if (minValue != null) {
                    modelSetpoint.setMinValue(minValue);
                }
                BigDecimal maxValue = setpoint.getMaxValue();
                if (maxValue != null) {
                    modelSetpoint.setMaxValue(maxValue);
                }
                BigDecimal step = setpoint.getStep();
                if (step != null) {
                    modelSetpoint.setStep(step);
                }
                modelWidget = modelSetpoint;
            }
            case Slider slider -> {
                ModelSlider modelSlider = SitemapFactory.eINSTANCE.createModelSlider();
                BigDecimal minValue = slider.getMinValue();
                if (minValue != null) {
                    modelSlider.setMinValue(minValue);
                }
                BigDecimal maxValue = slider.getMaxValue();
                if (maxValue != null) {
                    modelSlider.setMaxValue(maxValue);
                }
                BigDecimal step = slider.getStep();
                if (step != null) {
                    modelSlider.setStep(step);
                }
                if (slider.isSwitchEnabled()) {
                    modelSlider.setSwitchEnabled(true);
                }
                if (slider.isReleaseOnly()) {
                    modelSlider.setReleaseOnly(true);
                }
                modelWidget = modelSlider;
            }
            case Colorpicker colorpicker -> {
                ModelColorpicker modelColorpicker = SitemapFactory.eINSTANCE.createModelColorpicker();
                modelWidget = modelColorpicker;
            }
            case Colortemperaturepicker colortemperaturepicker -> {
                ModelColortemperaturepicker modelColortemperaturepicker = SitemapFactory.eINSTANCE
                        .createModelColortemperaturepicker();
                BigDecimal minValue = colortemperaturepicker.getMinValue();
                if (minValue != null) {
                    modelColortemperaturepicker.setMinValue(minValue);
                }
                BigDecimal maxValue = colortemperaturepicker.getMaxValue();
                if (maxValue != null) {
                    modelColortemperaturepicker.setMaxValue(maxValue);
                }
                modelWidget = modelColortemperaturepicker;
            }
            case Input input -> {
                ModelInput modelInput = SitemapFactory.eINSTANCE.createModelInput();
                String inputHint = input.getInputHint();
                if (inputHint != null) {
                    modelInput.setInputHint(inputHint);
                }
                modelWidget = modelInput;
            }
            case Webview webview -> {
                ModelWebview modelWebview = SitemapFactory.eINSTANCE.createModelWebview();
                modelWebview.setUrl(webview.getUrl());
                int height = webview.getHeight();
                if (height != 0) {
                    modelWebview.setHeight(height);
                }
                modelWidget = modelWebview;
            }
            case Mapview mapview -> {
                ModelMapview modelMapview = SitemapFactory.eINSTANCE.createModelMapview();
                int height = mapview.getHeight();
                if (height != 0) {
                    modelMapview.setHeight(height);
                }
                modelWidget = modelMapview;
            }
            case Image image -> {
                ModelImage modelImage = SitemapFactory.eINSTANCE.createModelImage();
                String url = image.getUrl();
                if (url != null) {
                    modelImage.setUrl(url);
                }
                int refresh = image.getRefresh();
                if (refresh != 0) {
                    modelImage.setRefresh(refresh);
                }
                modelWidget = modelImage;
            }
            case Video video -> {
                ModelVideo modelVideo = SitemapFactory.eINSTANCE.createModelVideo();
                modelVideo.setUrl(video.getUrl());
                String encoding = video.getEncoding();
                if (encoding != null) {
                    modelVideo.setEncoding(encoding);
                }
                modelWidget = modelVideo;
            }
            case Chart chart -> {
                ModelChart modelChart = SitemapFactory.eINSTANCE.createModelChart();
                int refresh = chart.getRefresh();
                if (refresh != 0) {
                    modelChart.setRefresh(refresh);
                }
                modelChart.setPeriod(chart.getPeriod());
                String service = chart.getService();
                if (service != null) {
                    modelChart.setService(service);
                }
                Boolean legend = chart.hasLegend();
                if (legend != null) {
                    modelChart.setLegend(legend);
                }
                if (chart.forceAsItem()) {
                    modelChart.setForceAsItem(true);
                }
                String yAxisDecimalPattern = chart.getYAxisDecimalPattern();
                if (yAxisDecimalPattern != null) {
                    modelChart.setYAxisDecimalPattern(yAxisDecimalPattern);
                }
                String interpolation = chart.getInterpolation();
                if (interpolation != null) {
                    modelChart.setInterpolation(interpolation);
                }
                modelWidget = modelChart;
            }
            default -> {
                ModelDefault modelDefault = SitemapFactory.eINSTANCE.createModelDefault();
                modelWidget = modelDefault;
            }
        }

        String item = widget.getItem();
        if (item != null) {
            modelWidget.setItem(item);
        }
        String label = widget.getLabel();
        if (label != null) {
            modelWidget.setLabel(label);
        }
        String icon = widget.getIcon();
        if (icon != null) {
            if (widget.isStaticIcon()) {
                modelWidget.setStaticIcon(icon);
            } else {
                modelWidget.setIcon(icon);
            }
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
            String releaseCmd = mapping.getReleaseCmd();
            if (releaseCmd != null) {
                modelMapping.setReleaseCmd(releaseCmd);
            }
            modelMapping.setLabel(mapping.getLabel());
            String icon = mapping.getIcon();
            if (icon != null) {
                modelMapping.setIcon(icon);
            }
            modelMappings.add(modelMapping);
        });
        return modelMappingList;
    }

    private ModelButtonDefinitionList modelButtons(List<ButtonDefinition> buttons) {
        ModelButtonDefinitionList modelButtonList = SitemapFactory.eINSTANCE.createModelButtonDefinitionList();
        EList<ModelButtonDefinition> modelButtons = modelButtonList.getElements();
        buttons.forEach(button -> {
            ModelButtonDefinition modelButton = SitemapFactory.eINSTANCE.createModelButtonDefinition();
            modelButton.setRow(button.getRow());
            modelButton.setColumn(button.getColumn());
            modelButton.setCmd(button.getCmd());
            modelButton.setLabel(button.getLabel());
            String icon = button.getIcon();
            if (icon != null) {
                modelButton.setIcon(icon);
            }
            modelButtons.add(modelButton);
        });
        return modelButtonList;
    }

    private ModelIconRuleList modelIconRules(List<Rule> rules) {
        ModelIconRuleList modelRuleList = SitemapFactory.eINSTANCE.createModelIconRuleList();
        EList<ModelIconRule> modelRules = modelRuleList.getElements();
        rules.forEach(rule -> {
            ModelIconRule modelRule = SitemapFactory.eINSTANCE.createModelIconRule();
            EList<ModelCondition> modelConditions = modelRule.getConditions();
            List<Condition> conditions = rule.getConditions();
            setModelConditions(modelConditions, conditions);
            String argument = rule.getArgument();
            if (argument != null) {
                modelRule.setArg(argument);
            }
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
            String argument = rule.getArgument();
            if (argument != null) {
                modelRule.setArg(argument);
            }
            modelRules.add(modelRule);
        });
        return modelRuleList;
    }

    private void setModelConditions(EList<ModelCondition> modelConditions, List<Condition> conditions) {
        conditions.forEach(condition -> {
            ModelCondition modelCondition = SitemapFactory.eINSTANCE.createModelCondition();
            String item = condition.getItem();
            if (item != null) {
                modelCondition.setItem(item);
            }
            String operator = condition.getCondition();
            if (operator != null) {
                modelCondition.setCondition(operator);
            }
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
        return modelRepository.createIsolatedModel("sitemap", inputStream, errors, warnings);
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
