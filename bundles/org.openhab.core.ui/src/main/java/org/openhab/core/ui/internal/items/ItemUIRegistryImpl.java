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
package org.openhab.core.ui.internal.items;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.measure.Unit;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.RegistryHook;
import org.openhab.core.library.items.CallItem;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.ImageItem;
import org.openhab.core.library.items.LocationItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.PlayerItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.Default;
import org.openhab.core.model.sitemap.sitemap.Group;
import org.openhab.core.model.sitemap.sitemap.IconRule;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Mapping;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.SitemapFactory;
import org.openhab.core.model.sitemap.sitemap.Slider;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.VisibilityRule;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.core.ui.internal.UIActivator;
import org.openhab.core.ui.items.ItemUIProvider;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a simple way to ask different item providers by a
 * single method call, i.e. the consumer does not need to iterate over all
 * registered providers as this is done inside this class.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Stefan Triller - Method to convert a state into something a sitemap entity can understand
 * @author Erdoan Hadzhiyusein - Adapted the class to work with the new DateTimeType
 * @author Laurent Garnier - new method getIconColor
 * @author Mark Herwege - new method getFormatPattern(widget), clean pattern
 * @author Laurent Garnier - Support added for multiple AND conditions in labelcolor/valuecolor/visibility
 * @author Laurent Garnier - new icon parameter based on conditional rules
 * @author Danny Baumann - widget label source support
 */
@NonNullByDefault
@Component(immediate = true, configurationPid = "org.openhab.sitemap", //
        property = Constants.SERVICE_PID + "=org.openhab.sitemap")
@ConfigurableService(category = "system", label = "Sitemap", description_uri = ItemUIRegistryImpl.CONFIG_URI)
public class ItemUIRegistryImpl implements ItemUIRegistry {

    protected static final String CONFIG_URI = "system:sitemap";

    /* RegEx to extract and parse a function String <code>'(.*?)\((.*)\):(.*)'</code> */
    protected static final Pattern EXTRACT_TRANSFORM_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\):(.*)");

    /* RegEx to identify format patterns. See java.util.Formatter#formatSpecifier (without the '%' at the very end). */
    protected static final String IDENTIFY_FORMAT_PATTERN_PATTERN = "%(?:(unit%)|(?:(?:\\d+\\$)?(?:[-#+ 0,(<]*)?(?:\\d+)?(?:\\.\\d+)?(?:[tT])?(?:[a-zA-Z])))";
    private static final Pattern FORMAT_PATTERN = Pattern.compile("(?:^|[^%])" + IDENTIFY_FORMAT_PATTERN_PATTERN);

    private static final int MAX_BUTTONS = 4;

    private static final String DEFAULT_SORTING = "NONE";

    private final Logger logger = LoggerFactory.getLogger(ItemUIRegistryImpl.class);

    protected final Set<ItemUIProvider> itemUIProviders = new HashSet<>();

    private final ItemRegistry itemRegistry;

    private final Map<Widget, Widget> defaultWidgets = Collections.synchronizedMap(new WeakHashMap<>());

    private String groupMembersSorting = DEFAULT_SORTING;

    private static class WidgetLabelWithSource {
        public final String label;
        public final WidgetLabelSource source;

        public WidgetLabelWithSource(String l, WidgetLabelSource s) {
            label = l;
            source = s;
        }
    }

    @Activate
    public ItemUIRegistryImpl(@Reference ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addItemUIProvider(ItemUIProvider itemUIProvider) {
        itemUIProviders.add(itemUIProvider);
    }

    public void removeItemUIProvider(ItemUIProvider itemUIProvider) {
        itemUIProviders.remove(itemUIProvider);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        applyConfig(config);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> config) {
        applyConfig(config);
    }

    /**
     * Handle the initial or a changed configuration.
     *
     * @param config the configuration
     */
    private void applyConfig(@Nullable Map<String, Object> config) {
        if (config != null) {
            final String groupMembersSortingString = Objects.toString(config.get("groupMembersSorting"), null);
            if (groupMembersSortingString != null) {
                groupMembersSorting = groupMembersSortingString;
            }
        }
    }

    @Override
    public @Nullable String getCategory(String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            String currentCategory = provider.getCategory(itemName);
            if (currentCategory != null) {
                return currentCategory;
            }
        }

        // use the category, if defined
        String category = getItemCategory(itemName);
        if (category != null) {
            return category.toLowerCase();
        }

        // do some reasonable default
        // try to get the item type from the item name
        Class<? extends Item> itemType = getItemType(itemName);
        if (itemType == null) {
            return null;
        }

        // we handle items here that have no specific widget,
        // e.g. the default widget of a rollerblind is "Switch".
        // We want to provide a dedicated default category for it
        // like "rollerblind".
        if (NumberItem.class.equals(itemType) || ContactItem.class.equals(itemType)
                || RollershutterItem.class.equals(itemType)) {
            return itemType.getSimpleName().replace("Item", "").toLowerCase();
        }
        return null;
    }

    @Override
    public @Nullable String getLabel(String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            String currentLabel = provider.getLabel(itemName);
            if (currentLabel != null) {
                return currentLabel;
            }
        }
        try {
            Item item = getItem(itemName);
            if (item.getLabel() != null) {
                return item.getLabel();
            }
        } catch (ItemNotFoundException ignored) {
        }

        return null;
    }

    @Override
    public @Nullable Widget getWidget(String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            Widget currentWidget = provider.getWidget(itemName);
            if (currentWidget != null) {
                return resolveDefault(currentWidget);
            }
        }
        return null;
    }

    @Override
    public @Nullable Widget getDefaultWidget(@Nullable Class<? extends Item> targetItemType, String itemName) {
        for (ItemUIProvider provider : itemUIProviders) {
            Widget widget = provider.getDefaultWidget(targetItemType, itemName);
            if (widget != null) {
                return widget;
            }
        }

        // do some reasonable default, if no provider had an answer
        // if the itemType is not defined, try to get it from the item name
        Class<? extends Item> itemType = targetItemType;
        if (itemType == null) {
            itemType = getItemType(itemName);
        }
        if (itemType == null) {
            return null;
        }

        if (GroupItem.class.equals(itemType)) {
            return SitemapFactory.eINSTANCE.createGroup();
        } else if (CallItem.class.equals(itemType) //
                || ContactItem.class.equals(itemType) //
                || DateTimeItem.class.equals(itemType)) {
            return SitemapFactory.eINSTANCE.createText();
        } else if (ColorItem.class.equals(itemType)) {
            return SitemapFactory.eINSTANCE.createColorpicker();
        } else if (DimmerItem.class.equals(itemType)) {
            Slider slider = SitemapFactory.eINSTANCE.createSlider();
            slider.setSwitchEnabled(true);
            return slider;
        } else if (ImageItem.class.equals(itemType)) {
            return SitemapFactory.eINSTANCE.createImage();
        } else if (LocationItem.class.equals(itemType)) {
            return SitemapFactory.eINSTANCE.createMapview();
        } else if (NumberItem.class.isAssignableFrom(itemType) //
                || StringItem.class.equals(itemType)) {
            boolean isReadOnly = isReadOnly(itemName);
            int commandOptionsSize = getCommandOptionsSize(itemName);
            if (!isReadOnly && commandOptionsSize > 0) {
                return commandOptionsSize <= MAX_BUTTONS ? SitemapFactory.eINSTANCE.createSwitch()
                        : SitemapFactory.eINSTANCE.createSelection();
            }
            if (!isReadOnly && hasStateOptions(itemName)) {
                return SitemapFactory.eINSTANCE.createSelection();
            }
            if (!isReadOnly && NumberItem.class.isAssignableFrom(itemType) && hasItemTag(itemName, "Setpoint")) {
                return SitemapFactory.eINSTANCE.createSetpoint();
            } else {
                return SitemapFactory.eINSTANCE.createText();
            }
        } else if (PlayerItem.class.equals(itemType)) {
            return createPlayerButtons();
        } else if (RollershutterItem.class.equals(itemType) //
                || SwitchItem.class.equals(itemType)) {
            return SitemapFactory.eINSTANCE.createSwitch();
        }

        return null;
    }

    private Switch createPlayerButtons() {
        final Switch playerItemSwitch = SitemapFactory.eINSTANCE.createSwitch();
        final List<Mapping> mappings = playerItemSwitch.getMappings();
        Mapping commandMapping;
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(NextPreviousType.PREVIOUS.name());
        commandMapping.setLabel("<<");
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(PlayPauseType.PAUSE.name());
        commandMapping.setLabel("||");
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(PlayPauseType.PLAY.name());
        commandMapping.setLabel(">");
        mappings.add(commandMapping = SitemapFactory.eINSTANCE.createMapping());
        commandMapping.setCmd(NextPreviousType.NEXT.name());
        commandMapping.setLabel(">>");
        return playerItemSwitch;
    }

    @Override
    public @Nullable String getLabel(Widget w) {
        String label = getLabelFromWidget(w).label;

        String itemName = w.getItem();
        if (itemName == null || itemName.isBlank()) {
            return transform(label, true, null);
        }

        String labelMappedOption = null;
        State state = null;
        StateDescription stateDescription = null;
        String formatPattern = getFormatPattern(w);

        if (formatPattern != null && label.indexOf("[") < 0) {
            label = label + " [" + formatPattern + "]";
        }

        // now insert the value, if the state is a string or decimal value and there is some formatting pattern defined
        // in the label or state description (i.e. it contains at least a %)
        try {
            final Item item = getItem(itemName);

            // There is a known issue in the implementation of the method getStateDescription() of class Item
            // in the following case:
            // - the item provider returns as expected a state description without pattern but with for
            // example a min value because a min value is set in the item definition but no label with
            // pattern is set.
            // - the channel state description provider returns as expected a state description with a pattern
            // In this case, the result is no display of value by UIs because no pattern is set in the
            // returned StateDescription. What is expected is the display of a value using the pattern
            // provided by the channel state description provider.
            stateDescription = item.getStateDescription();

            if (formatPattern != null) {
                state = item.getState();

                if (formatPattern.contains("%d")) {
                    if (!(state instanceof Number)) {
                        // States which do not provide a Number will be converted to DecimalType.
                        // e.g.: GroupItem can provide a count of items matching the active state
                        // for some group functions.
                        state = item.getStateAs(DecimalType.class);
                    }

                    // for fraction digits in state we don't want to risk format exceptions,
                    // so treat everything as floats:
                    formatPattern = formatPattern.replace("%d", "%.0f");
                }
            }
        } catch (ItemNotFoundException e) {
            logger.warn("Cannot retrieve item '{}' for widget {}", itemName, w.eClass().getInstanceTypeName());
        }

        boolean considerTransform = false;
        if (formatPattern != null) {
            if (formatPattern.isEmpty()) {
                label = label.substring(0, label.indexOf("[")).trim();
            } else {
                if (state == null || state instanceof UnDefType) {
                    formatPattern = formatUndefined(formatPattern);
                    considerTransform = true;
                } else {
                    // if the channel contains options, we build a label with the mapped option value
                    if (stateDescription != null) {
                        for (StateOption option : stateDescription.getOptions()) {
                            if (option.getValue().equals(state.toString()) && option.getLabel() != null) {
                                State stateOption = new StringType(option.getLabel());
                                try {
                                    String formatPatternOption = stateOption.format(formatPattern);
                                    labelMappedOption = label.trim();
                                    labelMappedOption = labelMappedOption.substring(0,
                                            labelMappedOption.indexOf("[") + 1) + formatPatternOption + "]";
                                } catch (IllegalArgumentException e) {
                                    logger.debug(
                                            "Mapping option value '{}' for item {} using format '{}' failed ({}); mapping is ignored",
                                            stateOption, itemName, formatPattern, e.getMessage());
                                    labelMappedOption = null;
                                }
                                break;
                            }
                        }
                    }

                    if (state instanceof DecimalType) {
                        // for DecimalTypes we don't want to risk format exceptions, if pattern contains unit
                        // placeholder
                        if (formatPattern.contains(UnitUtils.UNIT_PLACEHOLDER)) {
                            formatPattern = formatPattern.replaceAll(UnitUtils.UNIT_PLACEHOLDER, "").stripTrailing();
                        }
                    } else if (state instanceof QuantityType quantityState) {
                        // sanity convert current state to the item state description unit in case it was updated in the
                        // meantime. The item state is still in the "original" unit while the state description will
                        // display the new unit:
                        Unit<?> patternUnit = UnitUtils.parseUnit(formatPattern);
                        if (patternUnit != null && !quantityState.getUnit().equals(patternUnit)) {
                            quantityState = quantityState.toInvertibleUnit(patternUnit);
                        }

                        // The widget may define its own unit in the widget label. Convert to this unit:
                        if (quantityState != null) {
                            quantityState = convertStateToWidgetUnit(quantityState, w);
                            state = quantityState;
                        }
                    } else if (state instanceof DateTimeType type) {
                        // Translate a DateTimeType state to the local time zone
                        try {
                            state = type.toLocaleZone();
                        } catch (DateTimeException ignored) {
                        }
                    }

                    // The following exception handling has been added to work around a Java bug with formatting
                    // numbers. See http://bugs.sun.com/view_bug.do?bug_id=6476425
                    // Without this catch, the whole sitemap, or page can not be displayed!
                    // This also handles IllegalFormatConversionException, which is a subclass of IllegalArgument.
                    try {
                        Matcher matcher = EXTRACT_TRANSFORM_FUNCTION_PATTERN.matcher(formatPattern);
                        if (matcher.find()) {
                            considerTransform = true;
                            String type = matcher.group(1);
                            String pattern = matcher.group(2);
                            String value = matcher.group(3);
                            formatPattern = type + "(" + pattern + "):" + state.format(value);
                        } else {
                            formatPattern = state.format(formatPattern);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Exception while formatting value '{}' of item {} with format '{}': {}", state,
                                itemName, formatPattern, e.getMessage());
                        formatPattern = "Err";
                    }
                }

                label = label.trim();
                int index = label.indexOf("[");
                if (index >= 0) {
                    label = label.substring(0, index + 1) + formatPattern + "]";
                }
            }
        }

        return transform(label, considerTransform, labelMappedOption);
    }

    @Override
    public WidgetLabelSource getLabelSource(Widget w) {
        return getLabelFromWidget(w).source;
    }

    private QuantityType<?> convertStateToWidgetUnit(QuantityType<?> quantityState, Widget w) {
        Unit<?> widgetUnit = UnitUtils.parseUnit(getFormatPattern(w));
        if (widgetUnit != null && !widgetUnit.equals(quantityState.getUnit())) {
            return Objects.requireNonNullElse(quantityState.toInvertibleUnit(widgetUnit), quantityState);
        }

        return quantityState;
    }

    @Override
    public @Nullable String getFormatPattern(Widget w) {
        String label = getLabelFromWidget(w).label;
        String pattern = getFormatPattern(label);
        String itemName = w.getItem();
        try {
            Item item = null;
            if (itemName != null && !itemName.isBlank()) {
                item = getItem(itemName);
            }
            if (item != null && pattern == null) {
                StateDescription stateDescription = item.getStateDescription();
                if (stateDescription != null) {
                    pattern = stateDescription.getPattern();
                }
            }

            if (pattern == null) {
                return null;
            }

            // remove last part of pattern, after unit, if it exists, as this is not valid and creates problems with
            // updates
            if (item instanceof NumberItem numberItem && numberItem.getDimension() != null) {
                Matcher m = FORMAT_PATTERN.matcher(pattern);
                int matcherEnd = 0;
                if (m.find() && m.group(1) == null) {
                    matcherEnd = m.end();
                }
                String unit = pattern.substring(matcherEnd).trim();
                String postfix = "";
                int unitEnd = unit.indexOf(" ");
                if (unitEnd > -1) {
                    postfix = unit.substring(unitEnd + 1).trim();
                    unit = unit.substring(0, unitEnd);
                }
                if (!postfix.isBlank()) {
                    logger.warn(
                            "Item '{}' with unit, nothing allowed after unit in label pattern '{}', dropping postfix",
                            itemName, pattern);
                }
                pattern = unit.isBlank() ? pattern.substring(0, matcherEnd)
                        : pattern.substring(0, pattern.indexOf(unit, matcherEnd) + unit.length());
            }
        } catch (ItemNotFoundException e) {
            logger.warn("Cannot retrieve item '{}' for widget {}", itemName, w.eClass().getInstanceTypeName());
        }

        return pattern;
    }

    private @Nullable String getFormatPattern(@Nullable String label) {
        if (label == null) {
            return null;
        }
        String pattern = label.trim();
        int indexOpenBracket = pattern.indexOf("[");
        int indexCloseBracket = pattern.endsWith("]") ? pattern.length() - 1 : -1;

        if ((indexOpenBracket >= 0) && (indexCloseBracket > indexOpenBracket)) {
            return pattern.substring(indexOpenBracket + 1, indexCloseBracket);
        } else {
            return null;
        }
    }

    private WidgetLabelWithSource getLabelFromWidget(Widget w) {
        String label = null;
        WidgetLabelSource source = WidgetLabelSource.NONE;

        if (w.getLabel() != null) {
            // if there is a label defined for the widget, use this
            label = w.getLabel();
            source = WidgetLabelSource.SITEMAP_WIDGET;
        } else {
            String itemName = w.getItem();
            if (itemName != null) {
                // check if any item ui provider provides a label for this item
                label = getLabel(itemName);
                // if there is no item ui provider saying anything, simply use the name as a label
                if (label != null) {
                    source = WidgetLabelSource.ITEM_LABEL;
                } else {
                    label = itemName;
                    source = WidgetLabelSource.ITEM_NAME;
                }
            }
        }
        // use an empty string, if no label could be found
        return new WidgetLabelWithSource(label != null ? label : "", source);
    }

    /**
     * Takes the given <code>formatPattern</code> and replaces it with an analog
     * String-based pattern to replace all value Occurrences with a dash ("-")
     *
     * @param formatPattern the original pattern which will be replaces by a
     *            String pattern.
     * @return a formatted String with dashes ("-") as value replacement
     */
    protected String formatUndefined(String formatPattern) {
        String undefinedFormatPattern = formatPattern.replaceAll(IDENTIFY_FORMAT_PATTERN_PATTERN, "%1\\$s");
        try {
            return String.format(undefinedFormatPattern, "-");
        } catch (Exception e) {
            logger.warn(
                    "Exception while formatting undefined value [sourcePattern={}, targetPattern={}, exceptionMessage={}]",
                    formatPattern, undefinedFormatPattern, e.getMessage());
            return "Err";
        }
    }

    private String insertInLabel(String label, Object o) {
        return label.substring(0, label.indexOf("[") + 1) + o + "]";
    }

    /*
     * check if there is a status value being displayed on the right side of the
     * label (the right side is signified by being enclosed in square brackets [].
     * If so, check if the value starts with the call to a transformation service
     * (e.g. "[MAP(en.map):%s]") and execute the transformation in this case.
     * If the value does not start with the call to a transformation service,
     * we return the label with the mapped option value if provided (not null).
     */
    private String transform(String label, boolean matchTransform, @Nullable String labelMappedOption) {
        String ret = label;
        String formatPattern = getFormatPattern(label);
        if (formatPattern != null) {
            Matcher matcher = EXTRACT_TRANSFORM_FUNCTION_PATTERN.matcher(formatPattern);
            if (matchTransform && matcher.find()) {
                String type = matcher.group(1);
                String pattern = matcher.group(2);
                String value = matcher.group(3);
                TransformationService transformation = TransformationHelper
                        .getTransformationService(UIActivator.getContext(), type);
                if (transformation != null) {
                    try {
                        String transformationResult = transformation.transform(pattern, value);
                        if (transformationResult != null) {
                            ret = insertInLabel(label, transformationResult);
                        } else {
                            logger.warn("transformation of type {} did not return a valid result", type);
                            ret = insertInLabel(label, UnDefType.NULL);
                        }
                    } catch (TransformationException e) {
                        logger.error("transformation throws exception [transformation={}, value={}]", transformation,
                                value, e);
                        ret = insertInLabel(label, value);
                    }
                } else {
                    logger.warn(
                            "couldn't transform value in label because transformationService of type '{}' is unavailable",
                            type);
                    ret = insertInLabel(label, value);
                }
            } else if (labelMappedOption != null) {
                ret = labelMappedOption;
            }
        }
        return ret;
    }

    @Override
    public @Nullable String getCategory(Widget w) {
        String widgetTypeName = w.eClass().getInstanceTypeName()
                .substring(w.eClass().getInstanceTypeName().lastIndexOf(".") + 1);

        // the default is the widget type name, e.g. "switch"
        String category = widgetTypeName.toLowerCase();

        String conditionalIcon = getConditionalIcon(w);
        // if an icon is defined for the widget, use it
        if (w.getIcon() != null) {
            category = w.getIcon();
        } else if (w.getStaticIcon() != null) {
            category = w.getStaticIcon();
        } else if (conditionalIcon != null) {
            category = conditionalIcon;
        } else {
            // otherwise check if any item ui provider provides an icon for this item
            String itemName = w.getItem();
            if (itemName != null) {
                String result = getCategory(itemName);
                if (result != null) {
                    category = result;
                }
            }
        }
        return category;
    }

    @Override
    public @Nullable State getState(Widget w) {
        String itemName = w.getItem();
        if (itemName != null) {
            try {
                Item item = getItem(itemName);
                return convertState(w, item, item.getState());
            } catch (ItemNotFoundException e) {
                logger.warn("Cannot retrieve item '{}' for widget {}", itemName, w.eClass().getInstanceTypeName());
            }
        }
        return UnDefType.UNDEF;
    }

    /**
     * Converts an item state to the type the widget supports (if possible)
     *
     * @param w Widget in sitemap that shows the state
     * @param i item
     * @param state the state
     * @return the converted state or the original if conversion was not possible
     */
    @Override
    public @Nullable State convertState(Widget w, Item i, State state) {
        State returnState = null;

        State itemState = i.getState();
        if (itemState instanceof QuantityType<?> quantityTypeState) {
            itemState = convertStateToWidgetUnit(quantityTypeState, w);
        }

        if (w instanceof Switch && i instanceof RollershutterItem) {
            // RollerShutter are represented as Switch in a Sitemap but need a PercentType state
            returnState = itemState.as(PercentType.class);
        } else if (w instanceof Slider) {
            if (i.getAcceptedDataTypes().contains(PercentType.class)) {
                returnState = itemState.as(PercentType.class);
            } else if (!(itemState instanceof QuantityType<?>)) {
                returnState = itemState.as(DecimalType.class);
            }
        } else if (w instanceof Switch sw) {
            if (sw.getMappings().isEmpty()) {
                returnState = itemState.as(OnOffType.class);
            }
        }

        // we return the original state to not break anything
        return Objects.requireNonNullElse(returnState, itemState);
    }

    @Override
    public @Nullable Widget getWidget(Sitemap sitemap, String id) {
        if (!id.isEmpty()) {
            // see if the id is an itemName and try to get the widget for it
            Widget w = getWidget(id);
            if (w == null) {
                // try to get the default widget instead
                w = getDefaultWidget(null, id);
            }
            if (w != null) {
                w.setItem(id);
            } else {
                try {
                    int widgetID = Integer.parseInt(id.substring(0, 2));
                    if (widgetID < sitemap.getChildren().size()) {
                        w = sitemap.getChildren().get(widgetID);
                        for (int i = 2; i < id.length(); i += 2) {
                            int childWidgetID = Integer.parseInt(id.substring(i, i + 2));
                            if (childWidgetID < ((LinkableWidget) w).getChildren().size()) {
                                w = ((LinkableWidget) w).getChildren().get(childWidgetID);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // no valid number, so the requested page id does not exist
                }
            }
            return resolveDefault(w);
        }
        logger.warn("Cannot find page for id '{}'.", id);
        return null;
    }

    @Override
    public EList<Widget> getChildren(Sitemap sitemap) {
        EList<Widget> widgets = sitemap.getChildren();

        EList<Widget> result = new BasicEList<>();
        widgets.stream().map(this::resolveDefault).filter(Objects::nonNull).map(Objects::requireNonNull)
                .forEach(result::add);
        return result;
    }

    @Override
    public EList<Widget> getChildren(LinkableWidget w) {
        EList<Widget> widgets;
        if (w instanceof Group group && w.getChildren().isEmpty()) {
            widgets = getDynamicGroupChildren(group);
        } else {
            widgets = w.getChildren();
        }

        EList<Widget> result = new BasicEList<>();
        widgets.stream().map(this::resolveDefault).filter(Objects::nonNull).map(Objects::requireNonNull)
                .forEach(result::add);

        return result;
    }

    @Override
    public @Nullable EObject getParent(Widget w) {
        Widget w2 = defaultWidgets.get(w);
        return (w2 == null) ? w.eContainer() : w2.eContainer();
    }

    private @Nullable Widget resolveDefault(@Nullable Widget widget) {
        if (!(widget instanceof Default)) {
            return widget;
        } else {
            String itemName = widget.getItem();
            if (itemName != null) {
                Item item = get(itemName);
                if (item != null) {
                    Widget defaultWidget = getDefaultWidget(item.getClass(), item.getName());
                    if (defaultWidget != null) {
                        copyProperties(widget, defaultWidget);
                        defaultWidgets.put(defaultWidget, widget);
                        return defaultWidget;
                    }
                }
            }
            return null;
        }
    }

    private void copyProperties(Widget source, Widget target) {
        target.setItem(source.getItem());
        target.setIcon(source.getIcon());
        target.setStaticIcon(source.getStaticIcon());
        target.setLabel(source.getLabel());
        target.getVisibility().addAll(EcoreUtil.copyAll(source.getVisibility()));
        target.getLabelColor().addAll(EcoreUtil.copyAll(source.getLabelColor()));
        target.getValueColor().addAll(EcoreUtil.copyAll(source.getValueColor()));
        target.getIconColor().addAll(EcoreUtil.copyAll(source.getIconColor()));
        target.getIconRules().addAll(EcoreUtil.copyAll(source.getIconRules()));
    }

    /**
     * This method creates a list of children for a group dynamically.
     * If there are no explicit children defined in a sitemap, the children
     * can thus be created on the fly by iterating over the members of the group item.
     *
     * @param group The group widget to get children for
     * @return a list of default widgets provided for the member items
     */
    private EList<Widget> getDynamicGroupChildren(Group group) {
        EList<Widget> children = new BasicEList<>();
        String itemName = group.getItem();
        try {
            if (itemName != null) {
                Item item = getItem(itemName);
                if (item instanceof GroupItem groupItem) {
                    List<Item> members = new ArrayList<>(groupItem.getMembers());
                    switch (groupMembersSorting) {
                        case "LABEL":
                            members.sort((u1, u2) -> {
                                String u1Label = u1.getLabel();
                                String u2Label = u2.getLabel();
                                if (u1Label != null && u2Label != null) {
                                    return u1Label.compareTo(u2Label);
                                } else {
                                    return u1.getName().compareTo(u2.getName());
                                }
                            });
                            break;
                        case "NAME":
                            members.sort(Comparator.comparing(Item::getName));
                            break;
                        default:
                            break;
                    }
                    for (Item member : members) {
                        Widget widget = getDefaultWidget(member.getClass(), member.getName());
                        if (widget != null) {
                            widget.setItem(member.getName());
                            children.add(widget);
                        }
                    }
                } else {
                    logger.warn("Item '{}' is not a group.", item.getName());
                }
            } else {
                logger.warn("Dynamic group with label '{}' does not specify an associated item - ignoring it.",
                        group.getLabel());
            }
        } catch (ItemNotFoundException e) {
            logger.warn("Dynamic group with label '{}' will be ignored, because its item '{}' does not exist.",
                    group.getLabel(), itemName);
        }
        return children;
    }

    private boolean isReadOnly(String itemName) {
        try {
            Item item = getItem(itemName);
            StateDescription stateDescription = item.getStateDescription();
            return stateDescription != null && stateDescription.isReadOnly();
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private boolean hasStateOptions(String itemName) {
        try {
            Item item = getItem(itemName);
            StateDescription stateDescription = item.getStateDescription();
            return stateDescription != null && !stateDescription.getOptions().isEmpty();
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private int getCommandOptionsSize(String itemName) {
        try {
            Item item = getItem(itemName);
            CommandDescription commandDescription = item.getCommandDescription();
            return commandDescription != null ? commandDescription.getCommandOptions().size() : 0;
        } catch (ItemNotFoundException e) {
            return 0;
        }
    }

    private @Nullable Class<? extends Item> getItemType(String itemName) {
        try {
            Item item = getItem(itemName);
            return item.getClass();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    @Override
    public @Nullable State getItemState(String itemName) {
        try {
            Item item = getItem(itemName);
            return item.getState();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    public @Nullable String getItemCategory(String itemName) {
        try {
            Item item = getItem(itemName);
            return item.getCategory();
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    private boolean hasItemTag(String itemName, String tag) {
        try {
            Item item = getItem(itemName);
            return item.hasTag(tag);
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    @Override
    public Item getItem(String name) throws ItemNotFoundException {
        return itemRegistry.getItem(name);
    }

    @Override
    public Item getItemByPattern(String name) throws ItemNotFoundException, ItemNotUniqueException {
        return itemRegistry.getItemByPattern(name);
    }

    @Override
    public Collection<Item> getItems() {
        return itemRegistry.getItems();
    }

    @Override
    public Collection<Item> getItemsOfType(String type) {
        return itemRegistry.getItemsOfType(type);
    }

    @Override
    public Collection<Item> getItems(String pattern) {
        return itemRegistry.getItems(pattern);
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<Item> listener) {
        itemRegistry.addRegistryChangeListener(listener);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<Item> listener) {
        itemRegistry.removeRegistryChangeListener(listener);
    }

    @Override
    public Collection<Item> getAll() {
        return itemRegistry.getAll();
    }

    @Override
    public Stream<Item> stream() {
        return itemRegistry.stream();
    }

    @Override
    public String getWidgetId(Widget widget) {
        Widget w2 = defaultWidgets.get(widget);
        if (w2 != null) {
            return getWidgetId(w2);
        }

        String id = "";
        Widget w = widget;
        while (w.eContainer() instanceof Widget) {
            Widget parent = (Widget) w.eContainer();
            String index = String.valueOf(((LinkableWidget) parent).getChildren().indexOf(w));
            if (index.length() == 1) {
                index = "0" + index; // make it two digits
            }
            id = index + id;
            w = parent;
        }
        if (w.eContainer() instanceof Sitemap) {
            Sitemap sitemap = (Sitemap) w.eContainer();
            String index = String.valueOf(sitemap.getChildren().indexOf(w));
            if (index.length() == 1) {
                index = "0" + index; // make it two digits
            }
            id = index + id;
        }

        // if the widget is dynamically created and not available in the sitemap,
        // use the item name as the id
        if (w.eContainer() == null) {
            id = w.getItem();
        }
        return id;
    }

    private boolean matchStateToValue(State state, String value, @Nullable String matchCondition) {
        // Remove quotes - this occurs in some instances where multiple types
        // are defined in the xtext definitions
        String unquotedValue = value;
        if (unquotedValue.startsWith("\"") && unquotedValue.endsWith("\"")) {
            unquotedValue = unquotedValue.substring(1, unquotedValue.length() - 1);
        }

        // Convert the condition string into enum
        Condition condition = Condition.EQUAL;
        if (matchCondition != null) {
            condition = Condition.fromString(matchCondition);
            if (condition == null) {
                logger.warn("matchStateToValue: unknown match condition '{}'", matchCondition);
                return false;
            }
        }

        // Check if the value is equal to the supplied value
        boolean matched = false;
        if (UnDefType.NULL.toString().equals(unquotedValue) || UnDefType.UNDEF.toString().equals(unquotedValue)) {
            switch (condition) {
                case EQUAL:
                    if (unquotedValue.equals(state.toString())) {
                        matched = true;
                    }
                    break;
                case NOT:
                case NOTEQUAL:
                    if (!unquotedValue.equals(state.toString())) {
                        matched = true;
                    }
                    break;
                default:
                    break;
            }
        } else {
            if (state instanceof DecimalType || state instanceof QuantityType<?>) {
                try {
                    double compareDoubleValue = Double.parseDouble(unquotedValue);
                    double stateDoubleValue;
                    if (state instanceof DecimalType type) {
                        stateDoubleValue = type.doubleValue();
                    } else {
                        stateDoubleValue = ((QuantityType<?>) state).doubleValue();
                    }
                    switch (condition) {
                        case EQUAL:
                            if (stateDoubleValue == compareDoubleValue) {
                                matched = true;
                            }
                            break;
                        case LTE:
                            if (stateDoubleValue <= compareDoubleValue) {
                                matched = true;
                            }
                            break;
                        case GTE:
                            if (stateDoubleValue >= compareDoubleValue) {
                                matched = true;
                            }
                            break;
                        case GREATER:
                            if (stateDoubleValue > compareDoubleValue) {
                                matched = true;
                            }
                            break;
                        case LESS:
                            if (stateDoubleValue < compareDoubleValue) {
                                matched = true;
                            }
                            break;
                        case NOT:
                        case NOTEQUAL:
                            if (stateDoubleValue != compareDoubleValue) {
                                matched = true;
                            }
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("matchStateToValue: Decimal format exception: ", e);
                }
            } else if (state instanceof DateTimeType type) {
                ZonedDateTime val = type.getZonedDateTime();
                ZonedDateTime now = ZonedDateTime.now();
                long secsDif = ChronoUnit.SECONDS.between(val, now);

                try {
                    switch (condition) {
                        case EQUAL:
                            if (secsDif == Integer.parseInt(unquotedValue)) {
                                matched = true;
                            }
                            break;
                        case LTE:
                            if (secsDif <= Integer.parseInt(unquotedValue)) {
                                matched = true;
                            }
                            break;
                        case GTE:
                            if (secsDif >= Integer.parseInt(unquotedValue)) {
                                matched = true;
                            }
                            break;
                        case GREATER:
                            if (secsDif > Integer.parseInt(unquotedValue)) {
                                matched = true;
                            }
                            break;
                        case LESS:
                            if (secsDif < Integer.parseInt(unquotedValue)) {
                                matched = true;
                            }
                            break;
                        case NOT:
                        case NOTEQUAL:
                            if (secsDif != Integer.parseInt(unquotedValue)) {
                                matched = true;
                            }
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("matchStateToValue: Decimal format exception: ", e);
                }
            } else {
                // Strings only allow = and !=
                switch (condition) {
                    case NOT:
                    case NOTEQUAL:
                        if (!unquotedValue.equals(state.toString())) {
                            matched = true;
                        }
                        break;
                    default:
                        if (unquotedValue.equals(state.toString())) {
                            matched = true;
                        }
                        break;
                }
            }
        }

        return matched;
    }

    private @Nullable String processColorDefinition(Widget w, @Nullable List<ColorArray> colorList, String colorType) {
        // Sanity check
        if (colorList == null || colorList.isEmpty()) {
            return null;
        }

        logger.debug("Checking {} color for widget '{}'.", colorType, w.getLabel());

        String colorString = null;

        // Loop through all elements looking for the definition associated
        // with the supplied value
        for (ColorArray rule : colorList) {
            if (allConditionsOk(rule.getConditions(), w)) {
                // We have the color for this value - break!
                colorString = rule.getArg();
                break;
            }
        }

        if (colorString == null) {
            logger.debug("No {} color found for widget '{}'.", colorType, w.getLabel());
            return null;
        }

        // Remove quotes off the colour - if they exist
        if (colorString.startsWith("\"") && colorString.endsWith("\"")) {
            colorString = colorString.substring(1, colorString.length() - 1);
        }
        logger.debug("{} color for widget '{}' is '{}'.", colorType, w.getLabel(), colorString);

        return colorString;
    }

    @Override
    public @Nullable String getLabelColor(Widget w) {
        return processColorDefinition(w, w.getLabelColor(), "label");
    }

    @Override
    public @Nullable String getValueColor(Widget w) {
        return processColorDefinition(w, w.getValueColor(), "value");
    }

    @Override
    public @Nullable String getIconColor(Widget w) {
        return processColorDefinition(w, w.getIconColor(), "icon");
    }

    @Override
    public boolean getVisiblity(Widget w) {
        // Default to visible if parameters not set
        List<VisibilityRule> ruleList = w.getVisibility();
        if (ruleList == null || ruleList.isEmpty()) {
            return true;
        }

        logger.debug("Checking visiblity for widget '{}'.", w.getLabel());

        for (VisibilityRule rule : ruleList) {
            if (allConditionsOk(rule.getConditions(), w)) {
                return true;
            }
        }

        logger.debug("Widget {} is not visible.", w.getLabel());

        return false;
    }

    @Override
    public @Nullable String getConditionalIcon(Widget w) {
        List<IconRule> ruleList = w.getIconRules();
        // Sanity check
        if (ruleList == null || ruleList.isEmpty()) {
            return null;
        }

        logger.debug("Checking icon for widget '{}'.", w.getLabel());

        String icon = null;

        // Loop through all elements looking for the definition associated
        // with the supplied value
        for (IconRule rule : ruleList) {
            if (allConditionsOk(rule.getConditions(), w)) {
                // We have the icon for this value - break!
                icon = rule.getArg();
                break;
            }
        }

        if (icon == null) {
            logger.debug("No icon found for widget '{}'.", w.getLabel());
            return null;
        }

        // Remove quotes off the icon - if they exist
        if (icon.startsWith("\"") && icon.endsWith("\"")) {
            icon = icon.substring(1, icon.length() - 1);
        }
        logger.debug("icon for widget '{}' is '{}'.", w.getLabel(), icon);

        return icon;
    }

    private boolean allConditionsOk(@Nullable List<org.openhab.core.model.sitemap.sitemap.Condition> conditions,
            Widget w) {
        boolean allConditionsOk = true;
        if (conditions != null) {
            State defaultState = getState(w);

            // Go through all AND conditions
            for (org.openhab.core.model.sitemap.sitemap.Condition condition : conditions) {
                // Use a local state variable in case it gets overridden below
                State state = defaultState;

                // If there's an item defined here, get its state
                String itemName = condition.getItem();
                if (itemName != null) {
                    // Try and find the item to test.
                    Item item;
                    try {
                        item = itemRegistry.getItem(itemName);

                        // Get the item state
                        state = item.getState();
                    } catch (ItemNotFoundException e) {
                        logger.warn("Cannot retrieve item {} for widget {}", itemName,
                                w.eClass().getInstanceTypeName());
                    }
                }

                // Handle the sign
                String value;
                if (condition.getSign() != null) {
                    value = condition.getSign() + condition.getState();
                } else {
                    value = condition.getState();
                }

                if (state == null || !matchStateToValue(state, value, condition.getCondition())) {
                    allConditionsOk = false;
                    break;
                }
            }
        }
        return allConditionsOk;
    }

    enum Condition {
        EQUAL("=="),
        GTE(">="),
        LTE("<="),
        NOTEQUAL("!="),
        GREATER(">"),
        LESS("<"),
        NOT("!");

        private final String value;

        Condition(String value) {
            this.value = value;
        }

        public static @Nullable Condition fromString(String text) {
            for (Condition c : Condition.values()) {
                if (text.equalsIgnoreCase(c.value)) {
                    return c;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    @Override
    public Collection<Item> getItemsByTag(String... tags) {
        return itemRegistry.getItemsByTag(tags);
    }

    @Override
    public Collection<Item> getItemsByTagAndType(String type, String... tags) {
        return itemRegistry.getItemsByTagAndType(type, tags);
    }

    @Override
    public <T extends Item> Collection<T> getItemsByTag(Class<T> typeFilter, String... tags) {
        return itemRegistry.getItemsByTag(typeFilter, tags);
    }

    @Override
    public Item add(Item element) {
        return itemRegistry.add(element);
    }

    @Override
    public @Nullable Item update(Item element) {
        return itemRegistry.update(element);
    }

    @Override
    public @Nullable Item remove(String key) {
        return itemRegistry.remove(key);
    }

    @Override
    public @Nullable Item get(String key) {
        return itemRegistry.get(key);
    }

    @Override
    public @Nullable Item remove(String itemName, boolean recursive) {
        return itemRegistry.remove(itemName, recursive);
    }

    @Override
    public void addRegistryHook(RegistryHook<Item> hook) {
        itemRegistry.addRegistryHook(hook);
    }

    @Override
    public void removeRegistryHook(RegistryHook<Item> hook) {
        itemRegistry.removeRegistryHook(hook);
    }

    @Override
    public @Nullable String getUnitForWidget(Widget w) {
        String itemName = w.getItem();
        if (itemName != null) {
            try {
                Item item = getItem(itemName);

                // we require the item to define a dimension, otherwise no unit will be reported to the UIs.
                if (item instanceof NumberItem numberItem && numberItem.getDimension() != null) {
                    String pattern = getFormatPattern(w);
                    if (pattern == null || pattern.isBlank()) {
                        // if no Label was assigned to the Widget we fallback to the items unit
                        return numberItem.getUnitSymbol();
                    }

                    String unit = getUnitFromPattern(pattern);
                    if (!UnitUtils.UNIT_PLACEHOLDER.equals(unit)) {
                        return unit;
                    }

                    return numberItem.getUnitSymbol();
                }
            } catch (ItemNotFoundException e) {
                logger.warn("Failed to retrieve item during widget rendering, item does not exist: {}", e.getMessage());
            }
        }

        return "";
    }

    @Override
    public @Nullable State convertStateToLabelUnit(QuantityType<?> state, String label) {
        int index = label.lastIndexOf(" ");
        String labelUnit = index > 0 ? label.substring(index) : null;
        if (labelUnit != null && !state.getUnit().toString().equals(labelUnit)) {
            return state.toInvertibleUnit(labelUnit);
        }
        return state;
    }

    private @Nullable String getUnitFromPattern(@Nullable String format) {
        if (format == null || format.isBlank()) {
            return null;
        }
        int index = format.lastIndexOf(" ");
        String unit = index > 0 ? format.substring(index + 1) : null;
        unit = UnitUtils.UNIT_PERCENT_FORMAT_STRING.equals(unit) ? "%" : unit;
        return unit;
    }
}
