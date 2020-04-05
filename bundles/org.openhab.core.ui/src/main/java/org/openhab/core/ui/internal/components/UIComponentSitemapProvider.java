/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.ui.internal.components;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.SitemapPackage;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.model.sitemap.sitemap.impl.ChartImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ColorpickerImpl;
import org.openhab.core.model.sitemap.sitemap.impl.DefaultImpl;
import org.openhab.core.model.sitemap.sitemap.impl.FrameImpl;
import org.openhab.core.model.sitemap.sitemap.impl.GroupImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ImageImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ListImpl;
import org.openhab.core.model.sitemap.sitemap.impl.MappingImpl;
import org.openhab.core.model.sitemap.sitemap.impl.MapviewImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SelectionImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SetpointImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SitemapImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SliderImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SwitchImpl;
import org.openhab.core.model.sitemap.sitemap.impl.TextImpl;
import org.openhab.core.model.sitemap.sitemap.impl.VideoImpl;
import org.openhab.core.model.sitemap.sitemap.impl.WebviewImpl;
import org.openhab.core.model.sitemap.sitemap.impl.WidgetImpl;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link SitemapProvider} provides sitemaps from all well-formed {@link RootUIComponent} found in a specific
 * "system:sitemap" namespace.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
@Component(service = SitemapProvider.class)
public class UIComponentSitemapProvider implements SitemapProvider, RegistryChangeListener<RootUIComponent> {
    private final Logger logger = LoggerFactory.getLogger(UIComponentSitemapProvider.class);

    public static final String SITEMAP_NAMESPACE = "system:sitemap";

    private static final String SITEMAP_PREFIX = "uicomponents_";
    private static final String SITEMAP_SUFFIX = ".sitemap";

    private Map<String, Sitemap> sitemaps = new HashMap<>();
    private @Nullable UIComponentRegistryFactory componentRegistryFactory;
    private @Nullable UIComponentRegistry sitemapComponentRegistry;

    private final Set<ModelRepositoryChangeListener> modelChangeListeners = new CopyOnWriteArraySet<>();

    @Override
    public @Nullable Sitemap getSitemap(String sitemapName) {
        buildSitemap(sitemapName.replaceFirst(SITEMAP_PREFIX, ""));
        return sitemaps.get(sitemapName);
    }

    @Override
    public Set<String> getSitemapNames() {
        UIComponentRegistry registry = sitemapComponentRegistry;
        if (registry == null) {
            return Collections.emptySet();
        }

        sitemaps.clear();
        Collection<RootUIComponent> rootComponents = registry.getAll();
        // try building all sitemaps to leave the invalid ones out
        for (RootUIComponent rootComponent : rootComponents) {
            try {
                Sitemap sitemap = buildSitemap(rootComponent);
                sitemaps.put(sitemap.getName(), sitemap);
            } catch (Exception e) {
                logger.error("Cannot build sitemap {}", rootComponent.getUID(), e);
            }
        }

        return sitemaps.keySet();
    }

    protected @Nullable Sitemap buildSitemap(String sitemapName) {
        UIComponentRegistry registry = sitemapComponentRegistry;
        if (registry == null) {
            return null;
        }

        RootUIComponent rootComponent = registry.get(sitemapName);
        if (rootComponent != null) {
            try {
                Sitemap sitemap = buildSitemap(rootComponent);
                sitemaps.put(sitemap.getName(), sitemap);
                return null;
            } catch (Exception e) {
                logger.error("Cannot build sitemap {}", rootComponent.getUID(), e);
            }
        }

        return null;
    }

    protected Sitemap buildSitemap(RootUIComponent rootComponent) {
        if (!"Sitemap".equals(rootComponent.getType())) {
            throw new IllegalArgumentException("Root component type is not Sitemap");
        }

        SitemapImpl sitemap = (SitemapImpl) SitemapFactory.eINSTANCE.createSitemap();
        sitemap.setName(SITEMAP_PREFIX + rootComponent.getUID());
        sitemap.setLabel(rootComponent.getConfig().get("label").toString());

        if (rootComponent.getSlots() != null && rootComponent.getSlots().containsKey("widgets")) {
            for (UIComponent component : rootComponent.getSlot("widgets")) {
                Widget widget = buildWidget(component);
                if (widget != null) {
                    sitemap.getChildren().add(widget);
                }
            }
        }

        return sitemap;
    }

    protected @Nullable Widget buildWidget(UIComponent component) {
        Widget widget = null;

        switch (component.getType()) {
            case "Frame":
                FrameImpl frameWidget = (FrameImpl) SitemapFactory.eINSTANCE.createFrame();
                widget = frameWidget;
                break;
            case "Text":
                TextImpl textWidget = (TextImpl) SitemapFactory.eINSTANCE.createText();
                widget = textWidget;
                break;
            case "Group":
                GroupImpl groupWidget = (GroupImpl) SitemapFactory.eINSTANCE.createGroup();
                widget = groupWidget;
                break;
            case "Image":
                ImageImpl imageWidget = (ImageImpl) SitemapFactory.eINSTANCE.createImage();
                widget = imageWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "url", SitemapPackage.IMAGE__URL);
                setWidgetPropertyFromComponentConfig(widget, component, "refresh", SitemapPackage.IMAGE__REFRESH);
                break;
            case "Video":
                VideoImpl videoWidget = (VideoImpl) SitemapFactory.eINSTANCE.createVideo();
                widget = videoWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "url", SitemapPackage.IMAGE__URL);
                setWidgetPropertyFromComponentConfig(widget, component, "encoding", SitemapPackage.VIDEO__ENCODING);
                break;
            case "Chart":
                ChartImpl chartWidget = (ChartImpl) SitemapFactory.eINSTANCE.createChart();
                widget = chartWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "service", SitemapPackage.CHART__SERVICE);
                setWidgetPropertyFromComponentConfig(widget, component, "refresh", SitemapPackage.CHART__REFRESH);
                setWidgetPropertyFromComponentConfig(widget, component, "period", SitemapPackage.CHART__PERIOD);
                setWidgetPropertyFromComponentConfig(widget, component, "legend", SitemapPackage.CHART__LEGEND);
                break;
            case "Webview":
                WebviewImpl webviewWidget = (WebviewImpl) SitemapFactory.eINSTANCE.createWebview();
                widget = webviewWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "height", SitemapPackage.WEBVIEW__HEIGHT);
                setWidgetPropertyFromComponentConfig(widget, component, "url", SitemapPackage.WEBVIEW__URL);
                break;
            case "Switch":
                SwitchImpl switchWidget = (SwitchImpl) SitemapFactory.eINSTANCE.createSwitch();
                addWidgetMappings(switchWidget.getMappings(), component);
                widget = switchWidget;
                break;
            case "Mapview":
                MapviewImpl mapviewWidget = (MapviewImpl) SitemapFactory.eINSTANCE.createMapview();
                widget = mapviewWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "height", SitemapPackage.WEBVIEW__HEIGHT);
                break;
            case "Slider":
                SliderImpl sliderWidget = (SliderImpl) SitemapFactory.eINSTANCE.createSlider();
                widget = sliderWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "minValue", SitemapPackage.SLIDER__MIN_VALUE);
                setWidgetPropertyFromComponentConfig(widget, component, "maxValue", SitemapPackage.SLIDER__MAX_VALUE);
                setWidgetPropertyFromComponentConfig(widget, component, "step", SitemapPackage.SLIDER__STEP);
                setWidgetPropertyFromComponentConfig(widget, component, "switchEnabled",
                        SitemapPackage.SLIDER__SWITCH_ENABLED);
                setWidgetPropertyFromComponentConfig(widget, component, "sendFrequency",
                        SitemapPackage.SLIDER__FREQUENCY);
                break;
            case "Selection":
                SelectionImpl selectionWidget = (SelectionImpl) SitemapFactory.eINSTANCE.createSelection();
                addWidgetMappings(selectionWidget.getMappings(), component);
                widget = selectionWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "height", SitemapPackage.WEBVIEW__HEIGHT);
                break;
            case "List":
                ListImpl listWidget = (ListImpl) SitemapFactory.eINSTANCE.createList();
                widget = listWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "separator", SitemapPackage.LIST__SEPARATOR);
                break;
            case "Setpoint":
                SetpointImpl setpointWidget = (SetpointImpl) SitemapFactory.eINSTANCE.createSetpoint();
                widget = setpointWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "minValue", SitemapPackage.SETPOINT__MIN_VALUE);
                setWidgetPropertyFromComponentConfig(widget, component, "maxValue", SitemapPackage.SETPOINT__MAX_VALUE);
                setWidgetPropertyFromComponentConfig(widget, component, "step", SitemapPackage.SETPOINT__STEP);
                break;
            case "Colorpicker":
                ColorpickerImpl colorpickerWidget = (ColorpickerImpl) SitemapFactory.eINSTANCE.createColorpicker();
                widget = colorpickerWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "frequency",
                        SitemapPackage.COLORPICKER__FREQUENCY);
                break;
            case "Default":
                DefaultImpl defaultWidget = (DefaultImpl) SitemapFactory.eINSTANCE.createDefault();
                widget = defaultWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "height", SitemapPackage.DEFAULT__HEIGHT);
                break;
            default:
                logger.warn("Unknown sitemap component type {}", component.getType());
                break;
        }

        if (widget != null) {
            setWidgetPropertyFromComponentConfig(widget, component, "label", SitemapPackage.WIDGET__LABEL);
            setWidgetPropertyFromComponentConfig(widget, component, "icon", SitemapPackage.WIDGET__ICON);
            setWidgetPropertyFromComponentConfig(widget, component, "item", SitemapPackage.WIDGET__ITEM);

            if (widget instanceof LinkableWidget) {
                LinkableWidget linkableWidget = (LinkableWidget) widget;
                if (component.getSlots() != null && component.getSlots().containsKey("widgets")) {
                    for (UIComponent childComponent : component.getSlot("widgets")) {
                        Widget childWidget = buildWidget(childComponent);
                        if (childWidget != null) {
                            linkableWidget.getChildren().add(childWidget);
                        }
                    }
                }
            }

            // TODO: process visibility & color rules
        }

        return widget;
    }

    private void setWidgetPropertyFromComponentConfig(Widget widget, @Nullable UIComponent component,
            String configParamName, int feature) {
        if (component == null || component.getConfig() == null) {
            return;
        }
        Object value = component.getConfig().get(configParamName);
        if (value == null) {
            return;
        }
        WidgetImpl widgetImpl = (WidgetImpl) widget;
        widgetImpl.eSet(feature, ConfigUtil.normalizeType(value));
    }

    private void addWidgetMappings(EList<Mapping> mappings, UIComponent component) {
        if (component.getConfig() != null && component.getConfig().containsKey("mappings")) {
            if (component.getConfig().get("mappings") instanceof Collection<?>) {
                for (Object sourceMapping : (Collection<?>) component.getConfig().get("mappings")) {
                    if (sourceMapping instanceof String) {
                        String cmd = sourceMapping.toString().split("=")[0].trim();
                        String label = sourceMapping.toString().split("=")[1].trim();
                        MappingImpl mapping = (MappingImpl) SitemapFactory.eINSTANCE.createMapping();
                        mapping.setCmd(cmd);
                        mapping.setLabel(label);
                        mappings.add(mapping);
                    }
                }
            }
        }
    }

    @Override
    public void addModelChangeListener(ModelRepositoryChangeListener listener) {
        modelChangeListeners.add(listener);
    }

    @Override
    public void removeModelChangeListener(ModelRepositoryChangeListener listener) {
        modelChangeListeners.remove(listener);
    }

    @Override
    public void added(RootUIComponent element) {
        for (ModelRepositoryChangeListener listener : modelChangeListeners) {
            listener.modelChanged(SITEMAP_PREFIX + element.getUID() + SITEMAP_SUFFIX, EventType.ADDED);
        }
    }

    @Override
    public void removed(RootUIComponent element) {
        for (ModelRepositoryChangeListener listener : modelChangeListeners) {
            listener.modelChanged(SITEMAP_PREFIX + element.getUID() + SITEMAP_SUFFIX, EventType.REMOVED);
        }
    }

    @Override
    public void updated(RootUIComponent oldElement, RootUIComponent element) {
        for (ModelRepositoryChangeListener listener : modelChangeListeners) {
            listener.modelChanged(SITEMAP_PREFIX + element.getUID() + SITEMAP_SUFFIX, EventType.MODIFIED);
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setComponentRegistryFactory(UIComponentRegistryFactory componentRegistryFactory) {
        this.componentRegistryFactory = componentRegistryFactory;
        this.sitemapComponentRegistry = this.componentRegistryFactory.getRegistry(SITEMAP_NAMESPACE);
        this.sitemapComponentRegistry.addRegistryChangeListener(this);
    }

    protected void unsetComponentRegistryFactory(UIComponentRegistryFactory componentRegistryFactory) {
        UIComponentRegistry registry = this.sitemapComponentRegistry;
        if (registry != null) {
            registry.removeRegistryChangeListener(this);
        }

        this.componentRegistryFactory = null;
        this.sitemapComponentRegistry = null;
    }
}
