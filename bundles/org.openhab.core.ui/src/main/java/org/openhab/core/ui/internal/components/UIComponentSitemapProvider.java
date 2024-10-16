/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.model.sitemap.sitemap.ButtonDefinition;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.IconRule;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.SitemapPackage;
import org.openhab.core.model.sitemap.sitemap.VisibilityRule;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.model.sitemap.sitemap.impl.ButtonDefinitionImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ButtonImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ButtongridImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ChartImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ColorArrayImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ColorpickerImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ConditionImpl;
import org.openhab.core.model.sitemap.sitemap.impl.DefaultImpl;
import org.openhab.core.model.sitemap.sitemap.impl.FrameImpl;
import org.openhab.core.model.sitemap.sitemap.impl.GroupImpl;
import org.openhab.core.model.sitemap.sitemap.impl.IconRuleImpl;
import org.openhab.core.model.sitemap.sitemap.impl.ImageImpl;
import org.openhab.core.model.sitemap.sitemap.impl.InputImpl;
import org.openhab.core.model.sitemap.sitemap.impl.MappingImpl;
import org.openhab.core.model.sitemap.sitemap.impl.MapviewImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SelectionImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SetpointImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SitemapImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SliderImpl;
import org.openhab.core.model.sitemap.sitemap.impl.SwitchImpl;
import org.openhab.core.model.sitemap.sitemap.impl.TextImpl;
import org.openhab.core.model.sitemap.sitemap.impl.VideoImpl;
import org.openhab.core.model.sitemap.sitemap.impl.VisibilityRuleImpl;
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
 * @author Laurent Garnier - icon color support for all widgets
 * @author Laurent Garnier - Added support for new element Buttongrid
 * @author Laurent Garnier - Added icon field for mappings
 * @author Mark Herwege - Make UI provided sitemaps compatible with enhanced syntax in conditions
 * @author Mark Herwege - Add support for Button element
 */
@NonNullByDefault
@Component(service = SitemapProvider.class)
public class UIComponentSitemapProvider implements SitemapProvider, RegistryChangeListener<RootUIComponent> {
    private final Logger logger = LoggerFactory.getLogger(UIComponentSitemapProvider.class);

    public static final String SITEMAP_NAMESPACE = "system:sitemap";

    private static final String SITEMAP_PREFIX = "uicomponents_";
    private static final String SITEMAP_SUFFIX = ".sitemap";

    private static final Pattern CONDITION_PATTERN = Pattern
            .compile("(?<item>[A-Za-z]\\w*)?\\s*(?<condition>==|!=|<=|>=|<|>)?\\s*(?<sign>\\+|-)?(?<state>.+)");
    private static final Pattern COMMANDS_PATTERN = Pattern.compile("^(?<cmd1>\"[^\"]*\"|[^\": ]*):(?<cmd2>.*)$");

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
            return Set.of();
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
        Object label = rootComponent.getConfig().get("label");
        if (label != null) {
            sitemap.setLabel(label.toString());
        }

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
                setWidgetPropertyFromComponentConfig(widget, component, "url", SitemapPackage.VIDEO__URL);
                setWidgetPropertyFromComponentConfig(widget, component, "encoding", SitemapPackage.VIDEO__ENCODING);
                break;
            case "Chart":
                ChartImpl chartWidget = (ChartImpl) SitemapFactory.eINSTANCE.createChart();
                widget = chartWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "service", SitemapPackage.CHART__SERVICE);
                setWidgetPropertyFromComponentConfig(widget, component, "refresh", SitemapPackage.CHART__REFRESH);
                setWidgetPropertyFromComponentConfig(widget, component, "period", SitemapPackage.CHART__PERIOD);
                setWidgetPropertyFromComponentConfig(widget, component, "legend", SitemapPackage.CHART__LEGEND);
                setWidgetPropertyFromComponentConfig(widget, component, "forceAsItem",
                        SitemapPackage.CHART__FORCE_AS_ITEM);
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
                setWidgetPropertyFromComponentConfig(widget, component, "height", SitemapPackage.MAPVIEW__HEIGHT);
                break;
            case "Slider":
                SliderImpl sliderWidget = (SliderImpl) SitemapFactory.eINSTANCE.createSlider();
                widget = sliderWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "minValue", SitemapPackage.SLIDER__MIN_VALUE);
                setWidgetPropertyFromComponentConfig(widget, component, "maxValue", SitemapPackage.SLIDER__MAX_VALUE);
                setWidgetPropertyFromComponentConfig(widget, component, "step", SitemapPackage.SLIDER__STEP);
                setWidgetPropertyFromComponentConfig(widget, component, "switchEnabled",
                        SitemapPackage.SLIDER__SWITCH_ENABLED);
                setWidgetPropertyFromComponentConfig(widget, component, "releaseOnly",
                        SitemapPackage.SLIDER__RELEASE_ONLY);
                break;
            case "Selection":
                SelectionImpl selectionWidget = (SelectionImpl) SitemapFactory.eINSTANCE.createSelection();
                addWidgetMappings(selectionWidget.getMappings(), component);
                widget = selectionWidget;
                break;
            case "Input":
                InputImpl inputWidget = (InputImpl) SitemapFactory.eINSTANCE.createInput();
                widget = inputWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "inputHint", SitemapPackage.INPUT__INPUT_HINT);
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
                break;
            case "Buttongrid":
                ButtongridImpl buttongridWidget = (ButtongridImpl) SitemapFactory.eINSTANCE.createButtongrid();
                addWidgetButtons(buttongridWidget.getButtons(), component);
                widget = buttongridWidget;
                break;
            case "Button":
                ButtonImpl buttonWidget = (ButtonImpl) SitemapFactory.eINSTANCE.createButton();
                widget = buttonWidget;
                setWidgetPropertyFromComponentConfig(widget, component, "row", SitemapPackage.BUTTON__ROW);
                setWidgetPropertyFromComponentConfig(widget, component, "column", SitemapPackage.BUTTON__COLUMN);
                setWidgetPropertyFromComponentConfig(widget, component, "stateless", SitemapPackage.BUTTON__STATELESS);
                setWidgetPropertyFromComponentConfig(widget, component, "cmd", SitemapPackage.BUTTON__CMD);
                setWidgetPropertyFromComponentConfig(widget, component, "releaseCmd",
                        SitemapPackage.BUTTON__RELEASE_CMD);
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
            setWidgetIconPropertyFromComponentConfig(widget, component);
            setWidgetPropertyFromComponentConfig(widget, component, "item", SitemapPackage.WIDGET__ITEM);

            if (widget instanceof LinkableWidget linkableWidget) {
                if (component.getSlots() != null && component.getSlots().containsKey("widgets")) {
                    for (UIComponent childComponent : component.getSlot("widgets")) {
                        Widget childWidget = buildWidget(childComponent);
                        if (childWidget != null) {
                            linkableWidget.getChildren().add(childWidget);
                        }
                    }
                }
            }

            addWidgetVisibility(widget.getVisibility(), component);
            addLabelColor(widget.getLabelColor(), component);
            addValueColor(widget.getValueColor(), component);
            addIconColor(widget.getIconColor(), component);
            addIconRules(widget.getIconRules(), component);
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
        try {
            WidgetImpl widgetImpl = (WidgetImpl) widget;
            Object normalizedValue = ConfigUtil.normalizeType(value);
            if (widgetImpl.eGet(feature, false, false) instanceof Integer) {
                normalizedValue = (normalizedValue instanceof BigDecimal bd) ? bd.intValue()
                        : Integer.parseInt(normalizedValue.toString());
            } else if (widgetImpl.eGet(feature, false, false) instanceof Boolean
                    && !(normalizedValue instanceof Boolean)) {
                normalizedValue = Boolean.valueOf(normalizedValue.toString());
            }
            widgetImpl.eSet(feature, normalizedValue);
        } catch (Exception e) {
            logger.warn("Cannot set {} parameter for {} widget parameter: {}", configParamName, component.getType(),
                    e.getMessage());
        }
    }

    private void setWidgetIconPropertyFromComponentConfig(Widget widget, @Nullable UIComponent component) {
        if (component == null || component.getConfig() == null) {
            return;
        }
        Object staticIcon = component.getConfig().get("staticIcon");
        if (staticIcon != null && Boolean.parseBoolean(ConfigUtil.normalizeType(staticIcon).toString())) {
            setWidgetPropertyFromComponentConfig(widget, component, "icon", SitemapPackage.WIDGET__STATIC_ICON);
            return;
        }

        Object icon = component.getConfig().get("icon");
        if (icon == null) {
            return;
        }
        setWidgetPropertyFromComponentConfig(widget, component, "icon", SitemapPackage.WIDGET__ICON);
    }

    private @Nullable String stripQuotes(@Nullable String input) {
        if ((input != null) && (input.length() >= 2) && (input.charAt(0) == '\"')
                && (input.charAt(input.length() - 1) == '\"')) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    private void addWidgetMappings(EList<Mapping> mappings, UIComponent component) {
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
                        MappingImpl mapping = (MappingImpl) SitemapFactory.eINSTANCE.createMapping();
                        mapping.setCmd(cmd);
                        mapping.setReleaseCmd(releaseCmd);
                        mapping.setLabel(label);
                        mapping.setIcon(icon);
                        mappings.add(mapping);
                    }
                }
            }
        }
    }

    private void addWidgetButtons(EList<ButtonDefinition> buttons, UIComponent component) {
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
                        ButtonDefinitionImpl button = (ButtonDefinitionImpl) SitemapFactory.eINSTANCE
                                .createButtonDefinition();
                        button.setRow(row);
                        button.setColumn(column);
                        button.setCmd(cmd);
                        button.setLabel(label);
                        button.setIcon(icon);
                        buttons.add(button);
                    }
                }
            }
        }
    }

    private void addWidgetVisibility(EList<VisibilityRule> visibility, UIComponent component) {
        if (component.getConfig() != null && component.getConfig().containsKey("visibility")) {
            Object sourceVisibilities = component.getConfig().get("visibility");
            if (sourceVisibilities instanceof Collection<?> sourceVisibilitiesCollection) {
                for (Object sourceVisibility : sourceVisibilitiesCollection) {
                    if (sourceVisibility instanceof String) {
                        List<String> conditionsString = getRuleConditions(sourceVisibility.toString(), null);
                        VisibilityRuleImpl visibilityRule = (VisibilityRuleImpl) SitemapFactory.eINSTANCE
                                .createVisibilityRule();
                        List<ConditionImpl> conditions = getConditions(conditionsString, component, "visibility");
                        visibilityRule.eSet(SitemapPackage.VISIBILITY_RULE__CONDITIONS, conditions);
                        visibility.add(visibilityRule);
                    }
                }
            }
        }
    }

    private void addLabelColor(EList<ColorArray> labelColor, UIComponent component) {
        addColor(labelColor, component, "labelcolor");
    }

    private void addValueColor(EList<ColorArray> valueColor, UIComponent component) {
        addColor(valueColor, component, "valuecolor");
    }

    private void addIconColor(EList<ColorArray> iconColor, UIComponent component) {
        addColor(iconColor, component, "iconcolor");
    }

    private void addColor(EList<ColorArray> color, UIComponent component, String key) {
        if (component.getConfig() != null && component.getConfig().containsKey(key)) {
            Object sourceColors = component.getConfig().get(key);
            if (sourceColors instanceof Collection<?> sourceColorsCollection) {
                for (Object sourceColor : sourceColorsCollection) {
                    if (sourceColor instanceof String) {
                        String argument = getRuleArgument(sourceColor.toString());
                        List<String> conditionsString = getRuleConditions(sourceColor.toString(), argument);
                        ColorArrayImpl colorArray = (ColorArrayImpl) SitemapFactory.eINSTANCE.createColorArray();
                        colorArray.setArg(argument);
                        List<ConditionImpl> conditions = getConditions(conditionsString, component, key);
                        colorArray.eSet(SitemapPackage.COLOR_ARRAY__CONDITIONS, conditions);
                        color.add(colorArray);
                    }
                }
            }
        }
    }

    private void addIconRules(EList<IconRule> icon, UIComponent component) {
        if (component.getConfig() != null && component.getConfig().containsKey("iconrules")) {
            Object sourceIcons = component.getConfig().get("iconrules");
            if (sourceIcons instanceof Collection<?> sourceIconsCollection) {
                for (Object sourceIcon : sourceIconsCollection) {
                    if (sourceIcon instanceof String) {
                        String argument = getRuleArgument(sourceIcon.toString());
                        List<String> conditionsString = getRuleConditions(sourceIcon.toString(), argument);
                        IconRuleImpl iconRule = (IconRuleImpl) SitemapFactory.eINSTANCE.createIconRule();
                        iconRule.setArg(argument);
                        List<ConditionImpl> conditions = getConditions(conditionsString, component, "iconrules");
                        iconRule.eSet(SitemapPackage.ICON_RULE__CONDITIONS, conditions);
                        icon.add(iconRule);
                    }
                }
            }
        }
    }

    private List<ConditionImpl> getConditions(List<String> conditionsString, UIComponent component, String key) {
        List<ConditionImpl> conditions = new ArrayList<>();
        for (String conditionString : conditionsString) {
            Matcher matcher = CONDITION_PATTERN.matcher(conditionString);
            if (matcher.matches()) {
                ConditionImpl condition = (ConditionImpl) SitemapFactory.eINSTANCE.createCondition();
                condition.setItem(matcher.group("item"));
                condition.setCondition(matcher.group("condition"));
                condition.setSign(matcher.group("sign"));
                condition.setState(stripQuotes(matcher.group("state")));
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
