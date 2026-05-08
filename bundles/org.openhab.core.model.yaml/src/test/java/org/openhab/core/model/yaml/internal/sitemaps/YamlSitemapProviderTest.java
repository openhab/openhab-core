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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.model.yaml.YamlModelUtils;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.service.WatchService;
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
import org.openhab.core.sitemap.internal.NestedSitemapImpl;
import org.openhab.core.sitemap.internal.RuleImpl;
import org.openhab.core.sitemap.internal.SelectionImpl;
import org.openhab.core.sitemap.internal.SetpointImpl;
import org.openhab.core.sitemap.internal.SitemapImpl;
import org.openhab.core.sitemap.internal.SliderImpl;
import org.openhab.core.sitemap.internal.SwitchImpl;
import org.openhab.core.sitemap.internal.TextImpl;
import org.openhab.core.sitemap.internal.VideoImpl;
import org.openhab.core.sitemap.internal.WebviewImpl;
import org.openhab.core.sitemap.registry.SitemapFactory;

/**
 * The {@link YamlSitemapProviderTest} contains tests for the {@link YamlSitemapProvider} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlSitemapProviderTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model/sitemaps");
    private static final String MODEL_NAME = "model.yaml";
    private static final Path MODEL_PATH = Path.of(MODEL_NAME);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;

    private @Mock @NonNullByDefault({}) SitemapFactory sitemapFactory;

    private @NonNullByDefault({}) YamlModelRepositoryImpl modelRepository;
    private @NonNullByDefault({}) YamlSitemapProvider sitemapProvider;
    private @NonNullByDefault({}) TestSitemapChangeListener sitemapListener;

    @BeforeEach
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        when(sitemapFactory.createSitemap(anyString())).thenAnswer(i -> {
            return new SitemapImpl(i.getArgument(0));
        });
        when(sitemapFactory.createWidget(anyString(), any())).thenAnswer(i -> {
            return switch ((String) i.getArgument(0)) {
                case "Frame" -> {
                    yield new FrameImpl(i.getArgument(1));
                }
                case "Text" -> {
                    yield new TextImpl(i.getArgument(1));
                }
                case "Group" -> {
                    yield new GroupImpl(i.getArgument(1));
                }
                case "Switch" -> {
                    yield new SwitchImpl(i.getArgument(1));
                }
                case "Selection" -> {
                    yield new SelectionImpl(i.getArgument(1));
                }
                case "Setpoint" -> {
                    yield new SetpointImpl(i.getArgument(1));
                }
                case "Input" -> {
                    yield new InputImpl(i.getArgument(1));
                }
                case "Slider" -> {
                    yield new SliderImpl(i.getArgument(1));
                }
                case "Colorpicker" -> {
                    yield new ColorpickerImpl(i.getArgument(1));
                }
                case "Colortemperaturepicker" -> {
                    yield new ColortemperaturepickerImpl(i.getArgument(1));
                }
                case "Image" -> {
                    yield new ImageImpl(i.getArgument(1));
                }
                case "Chart" -> {
                    yield new ChartImpl(i.getArgument(1));
                }
                case "Video" -> {
                    yield new VideoImpl(i.getArgument(1));
                }
                case "Mapview" -> {
                    yield new MapviewImpl(i.getArgument(1));
                }
                case "Webview" -> {
                    yield new WebviewImpl(i.getArgument(1));
                }
                case "Buttongrid" -> {
                    yield new ButtongridImpl(i.getArgument(1));
                }
                case "Button" -> {
                    yield new ButtonImpl(i.getArgument(1));
                }
                case "Sitemap" -> {
                    yield new NestedSitemapImpl(i.getArgument(1));
                }
                case "Default" -> {
                    yield new DefaultImpl(i.getArgument(1));
                }
                default -> {
                    yield null;
                }
            };
        });
        when(sitemapFactory.createMapping()).thenAnswer(i -> {
            return new MappingImpl();
        });
        when(sitemapFactory.createRule()).thenAnswer(i -> {
            return new RuleImpl();
        });
        when(sitemapFactory.createCondition()).thenAnswer(i -> {
            return new ConditionImpl();
        });

        sitemapProvider = new YamlSitemapProvider(sitemapFactory);

        sitemapListener = new TestSitemapChangeListener();
        sitemapProvider.addProviderChangeListener(sitemapListener);

        modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(sitemapProvider);
    }

    @Test
    public void testLoadModelWithSitemap() throws IOException {
        Files.copy(SOURCE_PATH.resolve("bigSitemap.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        assertFalse(YamlModelUtils.isIsolatedModel(MODEL_NAME));
        assertThat(sitemapListener.sitemaps, is(aMapWithSize(1)));
        assertThat(sitemapListener.sitemaps, hasKey("demo"));
        assertThat(sitemapProvider.getAllFromModel(MODEL_NAME), hasSize(1));
        Collection<Sitemap> sitemaps = sitemapProvider.getAll();
        assertThat(sitemaps, hasSize(1));
        Sitemap sitemap = sitemaps.iterator().next();
        assertEquals("demo", sitemap.getName());
        assertEquals("Demo Sitemap", sitemap.getLabel());
        assertNull(sitemap.getIcon());
        List<Widget> widgets = sitemap.getWidgets();
        assertThat(widgets, hasSize(1));

        Widget widget = widgets.getFirst();
        assertEquals("Frame", widget.getWidgetType());
        assertNull(widget.getItem());
        assertEquals("Demo Widgets", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Frame);
        widgets = ((Frame) widget).getWidgets();
        assertThat(widgets, hasSize(19));

        widget = widgets.getFirst();
        assertEquals("Switch", widget.getWidgetType());
        assertEquals("DemoSwitch", widget.getItem());
        assertNull(widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Switch);
        Switch switchWidget = (Switch) widget;
        assertThat(switchWidget.getMappings(), hasSize(0));

        widget = widgets.get(1);
        assertEquals("Switch", widget.getWidgetType());
        assertEquals("DemoSwitch", widget.getItem());
        assertEquals("Button with release", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Switch);
        switchWidget = (Switch) widget;
        assertThat(switchWidget.getMappings(), hasSize(1));
        Mapping mapping = switchWidget.getMappings().getFirst();
        assertEquals("Switch ON", mapping.getLabel());
        assertEquals("switch-on", mapping.getIcon());
        assertEquals("ON", mapping.getCmd());
        assertEquals("OFF", mapping.getReleaseCmd());

        widget = widgets.get(2);
        assertEquals("Selection", widget.getWidgetType());
        assertEquals("DemoSwitch", widget.getItem());
        assertEquals("Test Selection", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertEquals("switch", widget.getIcon());
        assertFalse(widget.isStaticIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Selection);
        Selection selectionWidget = (Selection) widget;
        assertThat(selectionWidget.getMappings(), hasSize(2));
        mapping = selectionWidget.getMappings().getFirst();
        assertEquals("Switch ON", mapping.getLabel());
        assertNull(mapping.getIcon());
        assertEquals("ON", mapping.getCmd());
        assertNull(mapping.getReleaseCmd());
        mapping = selectionWidget.getMappings().get(1);
        assertEquals("Switch OFF", mapping.getLabel());
        assertNull(mapping.getIcon());
        assertEquals("OFF", mapping.getCmd());
        assertNull(mapping.getReleaseCmd());

        widget = widgets.get(3);
        assertEquals("Text", widget.getWidgetType());
        assertEquals("DemoContact", widget.getItem());
        assertEquals("Contact [MAP(en.map):%s]", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(1));
        Rule rule = widget.getLabelColor().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        Condition condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertNull(condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertEquals("blue", rule.getArgument());
        assertThat(widget.getValueColor(), hasSize(1));
        rule = widget.getValueColor().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertNull(condition.getCondition());
        assertEquals("OFF", condition.getValue());
        assertEquals("green", rule.getArgument());
        assertEquals("material:home", widget.getIcon());
        assertFalse(widget.isStaticIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(1));
        rule = widget.getIconColor().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertNull(condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertEquals("red", rule.getArgument());
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Text);
        assertThat(((Text) widget).getWidgets(), hasSize(0));

        widget = widgets.get(4);
        assertEquals("Input", widget.getWidgetType());
        assertEquals("DemoNumber", widget.getItem());
        assertEquals("Test Input", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Input);
        Input inputWidget = (Input) widget;
        assertEquals("number", inputWidget.getInputHint());

        widget = widgets.get(5);
        assertEquals("Text", widget.getWidgetType());
        assertEquals("DemoNumber", widget.getItem());
        assertEquals("Test Text [%d]", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(3));
        rule = widget.getLabelColor().getFirst();
        assertThat(rule.getConditions(), hasSize(2));
        condition = rule.getConditions().getFirst();
        assertNull(condition.getItem());
        assertEquals("<", condition.getCondition());
        assertEquals("50", condition.getValue());
        condition = rule.getConditions().get(1);
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("==", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertEquals("green", rule.getArgument());
        rule = widget.getLabelColor().get(1);
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertNull(condition.getItem());
        assertEquals("<=", condition.getCondition());
        assertEquals("75", condition.getValue());
        assertEquals("orange", rule.getArgument());
        rule = widget.getLabelColor().get(2);
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("blue", rule.getArgument());
        assertThat(widget.getValueColor(), hasSize(3));
        rule = widget.getValueColor().getFirst();
        assertThat(rule.getConditions(), hasSize(2));
        condition = rule.getConditions().getFirst();
        assertNull(condition.getItem());
        assertEquals("<", condition.getCondition());
        assertEquals("50", condition.getValue());
        condition = rule.getConditions().get(1);
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("==", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertEquals("blue", rule.getArgument());
        rule = widget.getValueColor().get(1);
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertNull(condition.getItem());
        assertEquals("<=", condition.getCondition());
        assertEquals("75", condition.getValue());
        assertEquals("red", rule.getArgument());
        rule = widget.getValueColor().get(2);
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("green", rule.getArgument());
        assertEquals("material:settings", widget.getIcon());
        assertTrue(widget.isStaticIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(3));
        rule = widget.getIconColor().getFirst();
        condition = rule.getConditions().getFirst();
        assertNull(condition.getItem());
        assertEquals("<", condition.getCondition());
        assertEquals("50", condition.getValue());
        condition = rule.getConditions().get(1);
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("==", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertEquals("red", rule.getArgument());
        rule = widget.getIconColor().get(1);
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertNull(condition.getItem());
        assertEquals("<=", condition.getCondition());
        assertEquals("75", condition.getValue());
        assertEquals("green", rule.getArgument());
        rule = widget.getIconColor().get(2);
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("violet", rule.getArgument());
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Text);
        assertThat(((Text) widget).getWidgets(), hasSize(0));

        widget = widgets.get(6);
        assertEquals("Slider", widget.getWidgetType());
        assertEquals("DemoNumber", widget.getItem());
        assertEquals("Test Slider [%.0f]", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(1));
        rule = widget.getLabelColor().getFirst();
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("green", rule.getArgument());
        assertThat(widget.getValueColor(), hasSize(1));
        rule = widget.getValueColor().getFirst();
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("red", rule.getArgument());
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(2));
        rule = widget.getIconRules().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("==", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertEquals("material:home", rule.getArgument());
        rule = widget.getIconRules().get(1);
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("material:favorite", rule.getArgument());
        assertThat(widget.getIconColor(), hasSize(1));
        rule = widget.getIconColor().getFirst();
        assertThat(rule.getConditions(), hasSize(0));
        assertEquals("blue", rule.getArgument());
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Slider);
        Slider sliderWidget = (Slider) widget;
        assertEquals(BigDecimal.valueOf(25), sliderWidget.getMinValue());
        assertEquals(BigDecimal.valueOf(75), sliderWidget.getMaxValue());
        assertEquals(BigDecimal.valueOf(5), sliderWidget.getStep());
        assertFalse(sliderWidget.isSwitchEnabled());
        assertTrue(sliderWidget.isReleaseOnly());

        widget = widgets.get(7);
        assertEquals("Setpoint", widget.getWidgetType());
        assertEquals("DemoNumber", widget.getItem());
        assertEquals("Test Setpoint", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Setpoint);
        Setpoint setpointWidget = (Setpoint) widget;
        assertEquals(BigDecimal.valueOf(12.5), setpointWidget.getMinValue());
        assertEquals(BigDecimal.valueOf(22.5), setpointWidget.getMaxValue());
        assertEquals(BigDecimal.valueOf(0.5), setpointWidget.getStep());

        widget = widgets.get(8);
        assertEquals("Colorpicker", widget.getWidgetType());
        assertEquals("DemoColor", widget.getItem());
        assertEquals("Test Colorpicker", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Colorpicker);

        widget = widgets.get(9);
        assertEquals("Colortemperaturepicker", widget.getWidgetType());
        assertEquals("DemoColorTemp", widget.getItem());
        assertEquals("Test Colortemperaturepicker", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Colortemperaturepicker);
        Colortemperaturepicker pickerWidget = (Colortemperaturepicker) widget;
        assertEquals(BigDecimal.valueOf(3000), pickerWidget.getMinValue());
        assertEquals(BigDecimal.valueOf(5000), pickerWidget.getMaxValue());

        widget = widgets.get(10);
        assertEquals("Image", widget.getWidgetType());
        assertEquals("DemoImage", widget.getItem());
        assertEquals("Test Image", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Image);
        Image imageWidget = (Image) widget;
        assertEquals(600000, imageWidget.getRefresh());
        assertEquals("https://raw.githubusercontent.com/wiki/openhab/openhab/images/features.png",
                imageWidget.getUrl());
        assertThat(imageWidget.getWidgets(), hasSize(0));

        widget = widgets.get(11);
        assertEquals("Chart", widget.getWidgetType());
        assertEquals("DemoNumber", widget.getItem());
        assertEquals("Test Chart", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Chart);
        Chart chartWidget = (Chart) widget;
        assertEquals("D", chartWidget.getPeriod());
        assertEquals(10000, chartWidget.getRefresh());
        assertEquals("rrd4j", chartWidget.getService());
        assertNotNull(chartWidget.hasLegend());
        assertFalse(chartWidget.hasLegend());
        assertTrue(chartWidget.forceAsItem());
        assertEquals("linear", chartWidget.getInterpolation());
        assertEquals("#.##E0", chartWidget.getYAxisDecimalPattern());

        widget = widgets.get(12);
        assertEquals("Video", widget.getWidgetType());
        assertNull(widget.getItem());
        assertEquals("Test Video", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Video);
        Video videoWidget = (Video) widget;
        assertEquals("https://demo.openhab.org/Hue.m4v", videoWidget.getUrl());
        assertEquals("hls", videoWidget.getEncoding());

        widget = widgets.get(13);
        assertEquals("Buttongrid", widget.getWidgetType());
        assertNull(widget.getItem());
        assertNull(widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Buttongrid);
        List<Widget> subWidgets = ((Buttongrid) widget).getWidgets();
        assertThat(subWidgets, hasSize(2));

        widget = subWidgets.getFirst();
        assertEquals("Button", widget.getWidgetType());
        assertEquals("DemoSwitch", widget.getItem());
        assertEquals("Off", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertEquals("switch-off", widget.getIcon());
        assertFalse(widget.isStaticIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(1));
        rule = widget.getVisibility().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("!=", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertNull(rule.getArgument());
        assertTrue(widget instanceof Button);
        Button buttonWidget = (Button) widget;
        assertEquals(1, buttonWidget.getRow());
        assertEquals(1, buttonWidget.getColumn());
        assertEquals("ON", buttonWidget.getCmd());
        assertNull(buttonWidget.getReleaseCmd());
        assertTrue(buttonWidget.isStateless());

        widget = subWidgets.get(1);
        assertEquals("Button", widget.getWidgetType());
        assertEquals("DemoSwitch", widget.getItem());
        assertEquals("On", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertEquals("switch-on", widget.getIcon());
        assertFalse(widget.isStaticIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(1));
        rule = widget.getVisibility().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("==", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertNull(rule.getArgument());
        assertTrue(widget instanceof Button);
        buttonWidget = (Button) widget;
        assertEquals(1, buttonWidget.getRow());
        assertEquals(1, buttonWidget.getColumn());
        assertEquals("OFF", buttonWidget.getCmd());
        assertNull(buttonWidget.getReleaseCmd());
        assertFalse(buttonWidget.isStateless());

        widget = widgets.get(14);
        assertEquals("Mapview", widget.getWidgetType());
        assertEquals("DemoLocation", widget.getItem());
        assertEquals("Test Mapview", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(1));
        rule = widget.getVisibility().getFirst();
        assertThat(rule.getConditions(), hasSize(2));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("!=", condition.getCondition());
        assertEquals("ON", condition.getValue());
        condition = rule.getConditions().get(1);
        assertEquals("DemoNumber", condition.getItem());
        assertEquals("<", condition.getCondition());
        assertEquals("100", condition.getValue());
        assertNull(rule.getArgument());
        assertTrue(widget instanceof Mapview);
        Mapview mapviewWidget = (Mapview) widget;
        assertEquals(10, mapviewWidget.getHeight());

        widget = widgets.get(15);
        assertEquals("Webview", widget.getWidgetType());
        assertNull(widget.getItem());
        assertEquals("Test Webview", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(2));
        rule = widget.getVisibility().getFirst();
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoSwitch", condition.getItem());
        assertEquals("!=", condition.getCondition());
        assertEquals("ON", condition.getValue());
        assertNull(rule.getArgument());
        rule = widget.getVisibility().get(1);
        assertThat(rule.getConditions(), hasSize(1));
        condition = rule.getConditions().getFirst();
        assertEquals("DemoNumber", condition.getItem());
        assertEquals("<", condition.getCondition());
        assertEquals("100", condition.getValue());
        assertNull(rule.getArgument());
        assertTrue(widget instanceof Webview);
        Webview webviewWidget = (Webview) widget;
        assertEquals("https://www.openhab.org/", webviewWidget.getUrl());
        assertEquals(15, webviewWidget.getHeight());

        widget = widgets.get(16);
        assertEquals("Group", widget.getWidgetType());
        assertEquals("DemoSwitchGroup", widget.getItem());
        assertNull(widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Group);
        assertThat(((Group) widget).getWidgets(), hasSize(0));

        widget = widgets.get(17);
        assertEquals("Sitemap", widget.getWidgetType());
        assertNull(widget.getItem());
        assertEquals("Test Nested Sitemap", widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof NestedSitemap);
        NestedSitemap nestedSitemapWidget = (NestedSitemap) widget;
        assertEquals("sitemap", nestedSitemapWidget.getSitemapName());

        widget = widgets.get(18);
        assertEquals("Default", widget.getWidgetType());
        assertEquals("DemoLocation", widget.getItem());
        assertNull(widget.getLabel());
        assertThat(widget.getLabelColor(), hasSize(0));
        assertThat(widget.getValueColor(), hasSize(0));
        assertNull(widget.getIcon());
        assertThat(widget.getIconRules(), hasSize(0));
        assertThat(widget.getIconColor(), hasSize(0));
        assertThat(widget.getVisibility(), hasSize(0));
        assertTrue(widget instanceof Default);
        Default defaultWidget = (Default) widget;
        assertEquals(0, defaultWidget.getHeight());
    }

    @Test
    public void testCreateIsolatedModelWithSitemap() throws IOException {
        Files.copy(SOURCE_PATH.resolve("basicSitemap.yaml"), fullModelPath);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(fullModelPath.toFile())) {
            String name = modelRepository.createIsolatedModel(inputStream, errors, warnings);
            assertNotNull(name);
            assertEquals(0, errors.size());
            assertEquals(0, warnings.size());

            assertTrue(YamlModelUtils.isIsolatedModel(name));
            assertThat(sitemapListener.sitemaps, is(aMapWithSize(0)));
            assertThat(sitemapProvider.getAll(), hasSize(0)); // No sitemap for the registry
            Collection<Sitemap> sitemaps = sitemapProvider.getAllFromModel(name);
            assertThat(sitemaps, hasSize(1));
            Sitemap sitemap = sitemaps.iterator().next();
            assertEquals("demo", sitemap.getName());
            assertEquals("Demo Sitemap", sitemap.getLabel());
            assertNull(sitemap.getIcon());
            List<Widget> widgets = sitemap.getWidgets();
            assertThat(widgets, hasSize(1));

            Widget widget = widgets.getFirst();
            assertEquals("Text", widget.getWidgetType());
            assertEquals("DemoContact", widget.getItem());
            assertEquals("Contact [MAP(en.map):%s]", widget.getLabel());
            assertThat(widget.getLabelColor(), hasSize(0));
            assertThat(widget.getValueColor(), hasSize(0));
            assertNull(widget.getIcon());
            assertThat(widget.getIconRules(), hasSize(0));
            assertThat(widget.getIconColor(), hasSize(0));
            assertThat(widget.getVisibility(), hasSize(0));
            assertTrue(widget instanceof Text);
            assertThat(((Text) widget).getWidgets(), hasSize(0));
        }
    }

    private static class TestSitemapChangeListener implements ProviderChangeListener<Sitemap> {

        public final Map<String, Sitemap> sitemaps = new HashMap<>();

        @Override
        public void added(Provider<Sitemap> provider, Sitemap element) {
            sitemaps.put(element.getName(), element);
        }

        @Override
        public void removed(Provider<Sitemap> provider, Sitemap element) {
            sitemaps.remove(element.getName());
        }

        @Override
        public void updated(Provider<Sitemap> provider, Sitemap oldelement, Sitemap element) {
            sitemaps.put(element.getName(), element);
        }
    }
}
