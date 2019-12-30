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
package org.openhab.core.ui.items;

import javax.measure.Unit;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.model.sitemap.sitemap.LinkableWidget;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;

/**
 * This interface is used by a service which combines the core item registry
 * with an aggregation of item ui providers; it can be therefore widely used for
 * all UI related information requests regarding items.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 */
@NonNullByDefault
public interface ItemUIRegistry extends ItemRegistry, ItemUIProvider {

    /**
     * Retrieves the label for a widget.
     *
     * This first checks, if there is a label defined in the sitemap. If not, it
     * checks all item UI providers for a label. If no label can be found, it is
     * set to an empty string.
     *
     * If the label contains a "[%format]" section, i.e.
     * "[%s]" for a string or "[%.3f]" for a decimal, this is replaced by the
     * current value of the item and padded by a "<span>" element.
     *
     * @param w the widget to retrieve the label for
     * @return the label to use for the widget
     */
    @Nullable
    String getLabel(Widget w);

    /**
     * Retrieves the category for a widget.
     *
     * This first checks, if there is a category defined in the sitemap. If not, it
     * checks all item UI providers for a category. If no category can be found, the
     * default category is the widget type name, e.g. "switch".
     *
     * @param w the widget to retrieve the category for
     * @return the category to use for the widget
     */
    @Nullable
    String getCategory(Widget w);

    /**
     * Retrieves the current state of the item of a widget or <code>UnDefType.UNDEF</code>.
     *
     * @param w the widget to retrieve the item state for
     * @return the item state of the widget
     */
    @Nullable
    State getState(Widget w);

    /**
     * Retrieves the widget for a given id on a given sitemap.
     *
     * @param sitemap the sitemap to look for the widget
     * @param id the id of the widget to look for
     * @return the widget for the given id
     */
    @Nullable
    Widget getWidget(Sitemap sitemap, String id);

    /**
     * Provides an id for a widget.
     *
     * This constructs a string out of the position of the sitemap, so if this
     * widget is the third child of a page linked from the fifth widget on the
     * home screen, its id would be "0503". If the widget is dynamically created
     * and not available in the sitemap, the name of its associated item is used
     * instead.
     *
     * @param w the widget to get the id for
     * @return an id for this widget
     */
    String getWidgetId(Widget w);

    /**
     * this should be used instead of Sitemap.getChildren() as the default
     * widgets have to be resolved to a concrete widget type.
     *
     * @param w the sitemap to retrieve the children for
     * @return the children of the sitemap
     */
    EList<Widget> getChildren(Sitemap sitemap);

    /**
     * this should be used instead of LinkableWidget.getChildren() as there
     * might be no children defined on the widget, but they should be
     * dynamically determined by looking at the members of the underlying item.
     *
     * @param w the widget to retrieve the children for
     * @return the (dynamically or statically defined) children of the widget
     */
    EList<Widget> getChildren(LinkableWidget w);

    /**
     * this should be used instead of Widget.eContainer() as as the concrete
     * widgets created from default widgets have no parent.
     *
     * @param w the widget to retrieve the parent for
     * @return the parent of the widget
     */
    @Nullable
    EObject getParent(Widget w);

    /**
     * Gets the label color for the widget. Checks conditional statements to
     * find the color based on the item value
     *
     * @param w Widget
     * @return String with the color
     */
    @Nullable
    String getLabelColor(Widget w);

    /**
     * Gets the value color for the widget. Checks conditional statements to
     * find the color based on the item value
     *
     * @param w Widget
     * @return String with the color
     */
    @Nullable
    String getValueColor(Widget w);

    /**
     * Gets the widget visibility based on the item state
     *
     * @param w Widget
     * @return true if the item is visible
     */
    boolean getVisiblity(Widget w);

    /**
     * Gets the item state
     *
     * @param itemName item name
     * @return State of the item
     */
    @Nullable
    State getItemState(String itemName);

    /**
     * Provide a widget specific String representation of a {@link Unit}.
     *
     * @param widget
     * @return a widget specific String representation of a {@link Unit}.
     */
    @Nullable
    String getUnitForWidget(Widget widget);

    /**
     * Convert the given state to the unit found in label. The label must be in the format "<value> <unit>" with unit
     * being a valid unit symbol.
     *
     * @param state the state to be converted.
     * @param label the label containing the target unit.
     * @return the converted state.
     */
    @Nullable
    State convertStateToLabelUnit(QuantityType<?> state, String label);

    /**
     * Convert the given state into
     *
     * @param widget Widget
     * @param item item
     * @param state state
     * @return the state converted to a type accepted by the item or the given state if the conversion was not possible
     */
    @Nullable
    State convertState(Widget widget, Item item, State state);

}
