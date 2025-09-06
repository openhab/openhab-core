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
package org.openhab.core.ui.internal.components;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigUtil;
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
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.Webview;
import org.openhab.core.sitemap.Widget;
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.openhab.core.sitemap.registry.SitemapProvider;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
 * @author Laurent Garnier - icon color support for all widgets
 * @author Laurent Garnier - Added support for new element Buttongrid
 * @author Laurent Garnier - Added icon field for mappings
 * @author Mark Herwege - Make UI provided sitemaps compatible with enhanced syntax in conditions
 * @author Mark Herwege - Add support for Button element
 * @author Laurent Garnier - Added support for new sitemap element Colortemperaturepicker
 * @author Mark Herwege - Implement sitemap registry
 */
@NonNullByDefault
@Component(service = SitemapProvider.class, immediate = true)
public class UIComponentSitemapProvider extends AbstractProvider<Sitemap>
        implements SitemapProvider, RegistryChangeListener<RootUIComponent> {
    private final Logger logger = LoggerFactory.getLogger(UIComponentSitemapProvider.class);

    public static final String SITEMAP_NAMESPACE = "system:sitemap";

    private static final String SITEMAP_PREFIX = "uicomponents_";

    private static final Pattern CONDITION_PATTERN = Pattern
            .compile("((?<item>[A-Za-z]\\w*)?\\s*(?<condition>==|!=|<=|>=|<|>))?\\s*(?<value>(\\+|-)?.+)");
    private static final Pattern COMMANDS_PATTERN = Pattern.compile("^(?<cmd1>\"[^\"]*\"|[^\": ]*):(?<cmd2>.*)$");

    private Map<String, Sitemap> sitemaps = new HashMap<>();
    private @Nullable UIComponentRegistryFactory componentRegistryFactory;
    private @Nullable UIComponentRegistry sitemapComponentRegistry;

    private final SitemapRegistry sitemapRegistry;
    private final SitemapFactory sitemapFactory;

    @Activate
    public UIComponentSitemapProvider(final @Reference SitemapRegistry sitemapRegistry,
            final @Reference SitemapFactory sitemapFactory) {
        this.sitemapRegistry = sitemapRegistry;
        this.sitemapFactory = sitemapFactory;
        sitemapRegistry.addSitemapProvider(this);
    }

    @Deactivate
    protected void deactivate() {
        sitemapRegistry.removeSitemapProvider(this);
    }

    @Override
    public @Nullable Sitemap getSitemap(String sitemapName) {
        return sitemaps.get(sitemapName);
    }

    @Override
    public Set<String> getSitemapNames() {
        return sitemaps.keySet();
    }

    protected Sitemap buildSitemap(RootUIComponent rootComponent) {
        if (!"Sitemap".equals(rootComponent.getType())) {
            throw new IllegalArgumentException("Root component type is not Sitemap");
        }

        Sitemap sitemap = sitemapFactory.createSitemap(SITEMAP_PREFIX + rootComponent.getUID());
        Object label = rootComponent.getConfig().get("label");
        if (label != null) {
            sitemap.setLabel(label.toString());
        }

        if (rootComponent.getSlots() != null && rootComponent.getSlots().containsKey("widgets")) {
            for (UIComponent component : rootComponent.getSlot("widgets")) {
                Widget widget = buildWidget(component, sitemap);
                if (widget != null) {
                    sitemap.getWidgets().add(widget);
                }
            }
        }
        sitemaps.put(sitemap.getName(), sitemap);

        return sitemap;
    }

    protected @Nullable Widget buildWidget(UIComponent component, Parent parent) {
        Widget widget = sitemapFactory.createWidget(component.getType(), parent);

        if (widget != null) {
            switch (widget) {
                case Image imageWidget:
                    setWidgetPropertyFromComponentConfig(imageWidget, component, "url");
                    setWidgetPropertyFromComponentConfig(imageWidget, component, "refresh");
                    break;
                case Video videoWidget:
                    setWidgetPropertyFromComponentConfig(videoWidget, component, "url");
                    setWidgetPropertyFromComponentConfig(videoWidget, component, "encoding");
                    break;
                case Chart chartWidget:
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "service");
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "refresh");
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "period");
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "legend");
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "forceAsItem");
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "yAxisDecimalPattern");
                    setWidgetPropertyFromComponentConfig(chartWidget, component, "interpolation");
                    break;
                case Webview webviewWidget:
                    setWidgetPropertyFromComponentConfig(webviewWidget, component, "height");
                    setWidgetPropertyFromComponentConfig(webviewWidget, component, "url");
                    break;
                case Mapview mapviewWidget:
                    setWidgetPropertyFromComponentConfig(mapviewWidget, component, "height");
                    break;
                case Slider sliderWidget:
                    setWidgetPropertyFromComponentConfig(sliderWidget, component, "minValue");
                    setWidgetPropertyFromComponentConfig(sliderWidget, component, "maxValue");
                    setWidgetPropertyFromComponentConfig(sliderWidget, component, "step");
                    setWidgetPropertyFromComponentConfig(sliderWidget, component, "switchEnabled");
                    setWidgetPropertyFromComponentConfig(sliderWidget, component, "releaseOnly");
                    break;
                case Selection selectionWidget:
                    addWidgetMappings(selectionWidget.getMappings(), component);
                    break;
                case Input inputWidget:
                    setWidgetPropertyFromComponentConfig(inputWidget, component, "inputHint");
                    break;
                case Setpoint setpointWidget:
                    setWidgetPropertyFromComponentConfig(setpointWidget, component, "minValue");
                    setWidgetPropertyFromComponentConfig(setpointWidget, component, "maxValue");
                    setWidgetPropertyFromComponentConfig(setpointWidget, component, "step");
                    break;
                case Colortemperaturepicker colortemperaturepickerWidget:
                    setWidgetPropertyFromComponentConfig(colortemperaturepickerWidget, component, "minValue");
                    setWidgetPropertyFromComponentConfig(colortemperaturepickerWidget, component, "maxValue");
                    break;
                case Buttongrid buttongridWidget:
                    addWidgetButtons(buttongridWidget.getButtons(), component);
                    break;
                case Button buttonWidget:
                    setWidgetPropertyFromComponentConfig(buttonWidget, component, "row");
                    setWidgetPropertyFromComponentConfig(buttonWidget, component, "column");
                    setWidgetPropertyFromComponentConfig(buttonWidget, component, "stateless");
                    setWidgetPropertyFromComponentConfig(buttonWidget, component, "cmd");
                    setWidgetPropertyFromComponentConfig(buttonWidget, component, "releaseCmd");
                    break;
                case Default defaultWidget:
                    setWidgetPropertyFromComponentConfig(defaultWidget, component, "height");
                    break;
                default:
                    break;
            }

            setWidgetPropertyFromComponentConfig(widget, component, "item");
            setWidgetPropertyFromComponentConfig(widget, component, "label");
            setWidgetPropertyFromComponentConfig(widget, component, "icon");
            setWidgetPropertyFromComponentConfig(widget, component, "staticIcon");

            if (widget instanceof LinkableWidget linkableWidget) {
                if (component.getSlots() != null && component.getSlots().containsKey("widgets")) {
                    for (UIComponent childComponent : component.getSlot("widgets")) {
                        Widget childWidget = buildWidget(childComponent, linkableWidget);
                        if (childWidget != null) {
                            linkableWidget.getWidgets().add(childWidget);
                        }
                    }
                }
            }

            addWidgetRules(widget.getVisibility(), component, "visibility");
            addWidgetRules(widget.getLabelColor(), component, "labelColor");
            addWidgetRules(widget.getValueColor(), component, "valueColor");
            addWidgetRules(widget.getIconColor(), component, "iconColor");
            addWidgetRules(widget.getIconRules(), component, "iconRules");
        }

        return widget;
    }

    private void setWidgetPropertyFromComponentConfig(Widget widget, @Nullable UIComponent component,
            String configParamName) {
        if (component == null || component.getConfig() == null) {
            return;
        }
        Object value = component.getConfig().get(configParamName);
        if (value == null) {
            return;
        }
        try {
            String setterName = "set" + configParamName.substring(0, 1).toUpperCase() + configParamName.substring(1);
            Object normalizedValue = ConfigUtil.normalizeType(value);
            Class<?> clazz = widget.getClass();
            Method method = List.of(clazz.getMethods()).stream().filter(m -> m.getName().equals(setterName)).findFirst()
                    .get();
            Class<?> argumentType = (method.getParameters()[0].getType());
            if (argumentType.equals(Integer.class) || argumentType.equals(int.class)) {
                normalizedValue = (normalizedValue instanceof BigDecimal bd) ? bd.intValue()
                        : Integer.parseInt(normalizedValue.toString());
            } else if ((argumentType.equals(Boolean.class) || argumentType.equals(boolean.class))
                    && !(normalizedValue instanceof Boolean)) {
                normalizedValue = Boolean.valueOf(normalizedValue.toString());
            }
            method.invoke(widget, normalizedValue);
        } catch (Exception e) {
            logger.warn("Cannot set {} parameter for {} widget parameter: {}", configParamName, component.getType(),
                    e.getMessage());
        }
    }

    private @Nullable String stripQuotes(@Nullable String input) {
        if ((input != null) && (input.length() >= 2) && (input.charAt(0) == '\"')
                && (input.charAt(input.length() - 1) == '\"')) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    private void addWidgetMappings(List<Mapping> mappings, UIComponent component) {
        if (component.getConfig() != null && component.getConfig().containsKey("mappings")) {
            Object sourceMappings = component.getConfig().get("mappings");
            if (sourceMappings instanceof Collection<?> sourceMappingsCollection) {
                for (Object sourceMapping : sourceMappingsCollection) {
                    if (sourceMapping instanceof String) {
                        String[] splitMapping = sourceMapping.toString().split("=");
                        String cmd = splitMapping[0].trim();
                        String releaseCmd = null;
                        Matcher matcher = COMMANDS_PATTERN.matcher(cmd);
                        if (matcher.matches()) {
                            cmd = matcher.group("cmd1");
                            releaseCmd = matcher.group("cmd2");
                        }
                        cmd = stripQuotes(cmd);
                        releaseCmd = stripQuotes(releaseCmd);
                        String label = stripQuotes(splitMapping[1].trim());
                        String icon = splitMapping.length < 3 ? null : stripQuotes(splitMapping[2].trim());
                        Mapping mapping = sitemapFactory.createMapping();
                        mapping.setCmd(cmd != null ? cmd : "");
                        mapping.setReleaseCmd(releaseCmd);
                        mapping.setLabel(label != null ? label : "");
                        mapping.setIcon(icon);
                        mappings.add(mapping);
                    }
                }
            }
        }
    }

    private void addWidgetButtons(List<ButtonDefinition> buttons, UIComponent component) {
        if (component.getConfig() != null && component.getConfig().containsKey("buttons")) {
            Object sourceButtons = component.getConfig().get("buttons");
            if (sourceButtons instanceof Collection<?> sourceButtonsCollection) {
                for (Object sourceButton : sourceButtonsCollection) {
                    if (sourceButton instanceof String) {
                        String[] splitted1 = sourceButton.toString().split(":", 3);
                        int row = Integer.parseInt(splitted1[0].trim());
                        int column = Integer.parseInt(splitted1[1].trim());
                        String[] splitted2 = splitted1[2].trim().split("=");
                        String cmd = stripQuotes(splitted2[0].trim());
                        String label = stripQuotes(splitted2[1].trim());
                        String icon = splitted2.length < 3 ? null : stripQuotes(splitted2[2].trim());
                        ButtonDefinition button = sitemapFactory.createButtonDefinition();
                        button.setRow(row);
                        button.setColumn(column);
                        if (cmd != null) {
                            button.setCmd(cmd);
                        }
                        if (label != null) {
                            button.setLabel(label);
                        }
                        button.setIcon(icon);
                        buttons.add(button);
                    }
                }
            }
        }
    }

    private void addWidgetRules(List<Rule> rules, UIComponent component, String key) {
        if (component.getConfig() != null && component.getConfig().containsKey(key)) {
            Object sourceRules = component.getConfig().get(key);
            if (sourceRules instanceof Collection<?> sourceRulesCollection) {
                for (Object sourceRule : sourceRulesCollection) {
                    if (sourceRule instanceof String) {
                        String argument = !key.equals("visibility") ? getRuleArgument(sourceRule.toString()) : null;
                        List<String> conditionsString = getRuleConditions(sourceRule.toString(), argument);
                        Rule rule = sitemapFactory.createRule();
                        List<Condition> conditions = getConditions(conditionsString, component, key);
                        rule.setConditions(conditions);
                        rules.add(rule);
                    }
                }
            }
        }
    }

    private List<Condition> getConditions(List<String> conditionsString, UIComponent component, String key) {
        List<Condition> conditions = new ArrayList<>();
        for (String conditionString : conditionsString) {
            Matcher matcher = CONDITION_PATTERN.matcher(conditionString);
            String value = null;
            if (matcher.matches()) {
                value = stripQuotes(matcher.group("value"));
            }
            if (matcher.matches() && value != null) {
                Condition condition = sitemapFactory.createCondition();
                condition.setItem(matcher.group("item"));
                condition.setCondition(matcher.group("condition"));
                condition.setValue(value);
                conditions.add(condition);
            } else {
                logger.warn("Syntax error in {} rule condition '{}' for widget {}", key, conditionString,
                        component.getType());
            }
        }
        return conditions;
    }

    private String getRuleArgument(String rule) {
        int argIndex = rule.lastIndexOf("=") + 1;
        String strippedRule = stripQuotes(rule.substring(argIndex).trim());
        return strippedRule != null ? strippedRule : "";
    }

    private List<String> getRuleConditions(String rule, @Nullable String argument) {
        String conditions = rule;
        if (argument != null) {
            conditions = rule.substring(0, rule.lastIndexOf(argument)).trim();
            if (conditions.endsWith("=")) {
                conditions = conditions.substring(0, conditions.length() - 1);
            }
        }
        List<String> conditionsList = List.of(conditions.split(" AND "));
        return conditionsList.stream().filter(Predicate.not(String::isBlank)).map(String::trim).toList();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setComponentRegistryFactory(UIComponentRegistryFactory componentRegistryFactory) {
        this.componentRegistryFactory = componentRegistryFactory;
        UIComponentRegistry sitemapComponentRegistry = this.componentRegistryFactory.getRegistry(SITEMAP_NAMESPACE);
        sitemapComponentRegistry.addRegistryChangeListener(this);
        sitemapComponentRegistry.getAll().forEach(element -> added(element));
        this.sitemapComponentRegistry = sitemapComponentRegistry;
    }

    protected void unsetComponentRegistryFactory(UIComponentRegistryFactory componentRegistryFactory) {
        UIComponentRegistry registry = this.sitemapComponentRegistry;
        if (registry != null) {
            registry.removeRegistryChangeListener(this);
        }

        this.sitemaps = new HashMap<>();
        this.componentRegistryFactory = null;
        this.sitemapComponentRegistry = null;
    }

    @Override
    public Collection<Sitemap> getAll() {
        return sitemaps.values();
    }

    @Override
    public void added(RootUIComponent element) {
        if ("Sitemap".equals(element.getType())) {
            String sitemapName = SITEMAP_PREFIX + element.getUID();
            if (sitemaps.get(sitemapName) == null) {
                Sitemap sitemap = buildSitemap(element);
                sitemaps.put(sitemapName, sitemap);
                notifyListenersAboutAddedElement(sitemap);
            }
        }
    }

    @Override
    public void removed(RootUIComponent element) {
        if ("Sitemap".equals(element.getType())) {
            String sitemapName = SITEMAP_PREFIX + element.getUID();
            Sitemap sitemap = sitemaps.remove(sitemapName);
            if (sitemap != null) {
                notifyListenersAboutRemovedElement(sitemap);
            }
        }
    }

    @Override
    public void updated(RootUIComponent oldElement, RootUIComponent element) {
        if ("Sitemap".equals(oldElement.getType()) && "Sitemap".equals(element.getType())) {
            String oldSitemapName = SITEMAP_PREFIX + oldElement.getUID();
            String sitemapName = SITEMAP_PREFIX + element.getUID();
            Sitemap oldSitemap = sitemaps.get(oldSitemapName);
            Sitemap sitemap = buildSitemap(element);
            if (!oldSitemapName.equals(sitemapName)) {
                sitemaps.remove(oldSitemapName);
            }
            sitemaps.put(sitemapName, sitemap);
            if (oldSitemap != null) {
                notifyListenersAboutUpdatedElement(oldSitemap, sitemap);
            } else {
                notifyListenersAboutAddedElement(sitemap);
            }
        }
    }
}
