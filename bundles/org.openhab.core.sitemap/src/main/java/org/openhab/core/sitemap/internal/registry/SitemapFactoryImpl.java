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
package org.openhab.core.sitemap.internal.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.ButtonDefinition;
import org.openhab.core.sitemap.Condition;
import org.openhab.core.sitemap.Mapping;
import org.openhab.core.sitemap.Parent;
import org.openhab.core.sitemap.Rule;
import org.openhab.core.sitemap.Sitemap;
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
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link SitemapFactoryImpl} implements the {@link SitemapRegistry}
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
@Component(service = SitemapFactory.class, immediate = true)
public class SitemapFactoryImpl implements SitemapFactory {

    // Sitemap widget types
    public static final String BUTTON = "Button";
    public static final String BUTTON_GRID = "Buttongrid";
    public static final String CHART = "Chart";
    public static final String COLOR_PICKER = "Colorpicker";
    public static final String COLOR_TEMPERATURE_PICKER = "Colortemperaturepicker";
    public static final String DEFAULT = "Default";
    public static final String FRAME = "Frame";
    public static final String GROUP = "Group";
    public static final String IMAGE = "Image";
    public static final String INPUT = "Input";
    public static final String MAPVIEW = "Mapview";
    public static final String SELECTION = "Selection";
    public static final String SETPOINT = "Setpoint";
    public static final String SLIDER = "Slider";
    public static final String SWITCH = "Switch";
    public static final String TEXT = "Text";
    public static final String VIDEO = "Video";
    public static final String WEBVIEW = "Webview";

    private static final String[] WIDGET_TYPES = { BUTTON, BUTTON_GRID, CHART, COLOR_PICKER, COLOR_TEMPERATURE_PICKER,
            DEFAULT, FRAME, GROUP, IMAGE, INPUT, MAPVIEW, SELECTION, SETPOINT, SLIDER, SWITCH, TEXT, VIDEO, WEBVIEW };

    @Override
    public Sitemap createSitemap(String sitemapName) {
        return new SitemapImpl(sitemapName);
    }

    @Override
    public @Nullable Widget createWidget(String widgetTypeName) {
        return switch (widgetTypeName) {
            case BUTTON -> new ButtonImpl();
            case BUTTON_GRID -> new ButtongridImpl();
            case CHART -> new ChartImpl();
            case COLOR_PICKER -> new ColorpickerImpl();
            case COLOR_TEMPERATURE_PICKER -> new ColortemperaturepickerImpl();
            case DEFAULT -> new DefaultImpl();
            case FRAME -> new FrameImpl();
            case GROUP -> new GroupImpl();
            case IMAGE -> new ImageImpl();
            case INPUT -> new InputImpl();
            case MAPVIEW -> new MapviewImpl();
            case SELECTION -> new SelectionImpl();
            case SETPOINT -> new SetpointImpl();
            case SLIDER -> new SliderImpl();
            case SWITCH -> new SwitchImpl();
            case TEXT -> new TextImpl();
            case VIDEO -> new VideoImpl();
            case WEBVIEW -> new WebviewImpl();
            default -> null;
        };
    }

    @Override
    public @Nullable Widget createWidget(String widgetTypeName, Parent parent) {
        return switch (widgetTypeName) {
            case BUTTON -> new ButtonImpl(parent);
            case BUTTON_GRID -> new ButtongridImpl(parent);
            case CHART -> new ChartImpl(parent);
            case COLOR_PICKER -> new ColorpickerImpl(parent);
            case COLOR_TEMPERATURE_PICKER -> new ColortemperaturepickerImpl(parent);
            case DEFAULT -> new DefaultImpl(parent);
            case FRAME -> new FrameImpl(parent);
            case GROUP -> new GroupImpl(parent);
            case IMAGE -> new ImageImpl(parent);
            case INPUT -> new InputImpl(parent);
            case MAPVIEW -> new MapviewImpl(parent);
            case SELECTION -> new SelectionImpl(parent);
            case SETPOINT -> new SetpointImpl(parent);
            case SLIDER -> new SliderImpl(parent);
            case SWITCH -> new SwitchImpl(parent);
            case TEXT -> new TextImpl(parent);
            case VIDEO -> new VideoImpl(parent);
            case WEBVIEW -> new WebviewImpl(parent);
            default -> null;
        };
    }

    @Override
    public ButtonDefinition createButtonDefinition() {
        return new ButtonDefinitionImpl();
    }

    @Override
    public Mapping createMapping() {
        return new MappingImpl();
    }

    @Override
    public Rule createRule() {
        return new RuleImpl();
    }

    @Override
    public Condition createCondition() {
        return new ConditionImpl();
    }

    @Override
    public String[] getSupportedWidgetTypes() {
        return WIDGET_TYPES;
    }
}
