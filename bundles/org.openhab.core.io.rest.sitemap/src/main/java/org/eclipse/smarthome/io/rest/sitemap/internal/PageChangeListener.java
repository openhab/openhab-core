/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.sitemap.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.EList;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTOMapper;
import org.eclipse.smarthome.io.rest.sitemap.SitemapSubscriptionService.SitemapSubscriptionCallback;
import org.eclipse.smarthome.model.sitemap.Chart;
import org.eclipse.smarthome.model.sitemap.ColorArray;
import org.eclipse.smarthome.model.sitemap.Frame;
import org.eclipse.smarthome.model.sitemap.VisibilityRule;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;

/**
 * This is a class that listens on item state change events and creates sitemap events for a dedicated sitemap page.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class PageChangeListener implements StateChangeListener {

    private static final int REVERT_INTERVAL = 300;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    private final String sitemapName;
    private final String pageId;
    private final ItemUIRegistry itemUIRegistry;
    private EList<Widget> widgets;
    private Set<Item> items;
    private final List<SitemapSubscriptionCallback> callbacks = Collections
            .synchronizedList(new ArrayList<SitemapSubscriptionCallback>());
    private Set<SitemapSubscriptionCallback> distinctCallbacks = Collections.emptySet();

    /**
     * Creates a new instance.
     *
     * @param sitemapName the sitemap name of the page
     * @param pageId the id of the page for which events are created
     * @param itemUIRegistry the ItemUIRegistry which is needed for the functionality
     * @param widgets the list of widgets that are part of the page.
     */
    public PageChangeListener(String sitemapName, String pageId, ItemUIRegistry itemUIRegistry, EList<Widget> widgets) {
        this.sitemapName = sitemapName;
        this.pageId = pageId;
        this.itemUIRegistry = itemUIRegistry;

        updateItemsAndWidgets(widgets);
    }

    private void updateItemsAndWidgets(EList<Widget> widgets) {
        if (this.widgets != null) {
            // cleanup statechange listeners in case widgets were removed
            items = getAllItems(this.widgets);
            for (Item item : items) {
                if (item instanceof GenericItem) {
                    ((GenericItem) item).removeStateChangeListener(this);
                }
            }
        }

        this.widgets = widgets;
        items = getAllItems(widgets);
        for (Item item : items) {
            if (item instanceof GenericItem) {
                ((GenericItem) item).addStateChangeListener(this);
            }
        }
    }

    public String getSitemapName() {
        return sitemapName;
    }

    public String getPageId() {
        return pageId;
    }

    public void addCallback(SitemapSubscriptionCallback callback) {
        callbacks.add(callback);
        // we transform the list of callbacks to a set in order to remove duplicates
        distinctCallbacks = new HashSet<>(callbacks);
    }

    public void removeCallback(SitemapSubscriptionCallback callback) {
        callbacks.remove(callback);
        distinctCallbacks = new HashSet<>(callbacks);
    }

    /**
     * Disposes this instance and releases all resources.
     */
    public void dispose() {
        for (Item item : items) {
            if (item instanceof GenericItem) {
                ((GenericItem) item).removeStateChangeListener(this);
            } else if (item instanceof GroupItem) {
                ((GroupItem) item).removeStateChangeListener(this);
            }
        }
    }

    /**
     * Collects all items that are represented by a given list of widgets
     *
     * @param widgets
     *            the widget list to get the items for added to all bundles containing REST resources
     * @return all items that are represented by the list of widgets
     */
    private Set<Item> getAllItems(EList<Widget> widgets) {
        Set<Item> items = new HashSet<Item>();
        if (itemUIRegistry != null) {
            for (Widget widget : widgets) {
                addItemWithName(items, widget.getItem());
                if (widget instanceof Frame) {
                    items.addAll(getAllItems(((Frame) widget).getChildren()));
                }
                // now scan visibility rules
                for (VisibilityRule rule : widget.getVisibility()) {
                    addItemWithName(items, rule.getItem());
                }
                // now scan label color rules
                for (ColorArray rule : widget.getLabelColor()) {
                    addItemWithName(items, rule.getItem());
                }
                // now scan value color rules
                for (ColorArray rule : widget.getValueColor()) {
                    addItemWithName(items, rule.getItem());
                }
            }
        }
        return items;
    }

    private void addItemWithName(Set<Item> items, String itemName) {
        if (itemName != null) {
            try {
                Item item = itemUIRegistry.getItem(itemName);
                items.add(item);
            } catch (ItemNotFoundException e) {
                // ignore
            }
        }
    }

    private void constructAndSendEvents(Item item, State newState) {
        Set<SitemapEvent> events = constructSitemapEvents(item, newState, widgets);
        for (SitemapEvent event : events) {
            for (SitemapSubscriptionCallback callback : distinctCallbacks) {
                callback.onEvent(event);
            }
        }
    }

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        // For all items except group, send an event only when the event state is changed.
        if (item instanceof GroupItem) {
            return;
        }
        constructAndSendEvents(item, newState);
    }

    @Override
    public void stateUpdated(Item item, State state) {
        // For group item only, send an event each time the event state is updated.
        // It allows updating the group label while the group state is unchanged,
        // for example the count in label for Group:Switch:OR
        if (!(item instanceof GroupItem)) {
            return;
        }
        constructAndSendEvents(item, state);
    }

    public void keepCurrentState(Item item) {
        scheduler.schedule(() -> {
            constructAndSendEvents(item, item.getState());
        }, REVERT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void changeStateTo(Item item, State state) {
        constructAndSendEvents(item, state);
    }

    private Set<SitemapEvent> constructSitemapEvents(Item item, State state, List<Widget> widgets) {
        Set<SitemapEvent> events = new HashSet<>();
        for (Widget w : widgets) {
            if (w instanceof Frame) {
                events.addAll(constructSitemapEvents(item, state, itemUIRegistry.getChildren((Frame) w)));
            }

            boolean skipWidget = (w.getItem() == null) || !w.getItem().equals(item.getName());
            // We skip the chart widgets having a refresh argument
            if (!skipWidget && w instanceof Chart) {
                Chart chartWidget = (Chart) w;
                skipWidget = chartWidget.getRefresh() > 0;
            }
            if (!skipWidget || definesVisibilityOrColor(w, item.getName())) {
                SitemapWidgetEvent event = new SitemapWidgetEvent();
                event.sitemapName = sitemapName;
                event.pageId = pageId;
                event.label = itemUIRegistry.getLabel(w);
                event.labelcolor = itemUIRegistry.getLabelColor(w);
                event.valuecolor = itemUIRegistry.getValueColor(w);
                event.widgetId = itemUIRegistry.getWidgetId(w);
                event.visibility = itemUIRegistry.getVisiblity(w);
                // event.item contains data from the item including its state (in event.item.state)
                String widgetTypeName = w.eClass().getInstanceTypeName()
                        .substring(w.eClass().getInstanceTypeName().lastIndexOf(".") + 1);
                boolean drillDown = "mapview".equalsIgnoreCase(widgetTypeName);
                Predicate<Item> itemFilter = (i -> i.getType().equals(CoreItemFactory.LOCATION));
                event.item = EnrichedItemDTOMapper.map(item, drillDown, itemFilter, null, null);

                // event.state is an adjustment of the item state to the widget type.
                event.state = itemUIRegistry.convertState(w, item, state).toFullString();
                // In case this state is identical to the item state, its value is set to null.
                if (event.state != null && event.state.equals(event.item.state)) {
                    event.state = null;
                }

                events.add(event);
            }
        }
        return events;
    }

    private boolean definesVisibilityOrColor(Widget w, String name) {
        for (VisibilityRule rule : w.getVisibility()) {
            if (name.equals(rule.getItem())) {
                return true;
            }
        }
        for (ColorArray rule : w.getLabelColor()) {
            if (name.equals(rule.getItem())) {
                return true;
            }
        }
        for (ColorArray rule : w.getValueColor()) {
            if (name.equals(rule.getItem())) {
                return true;
            }
        }
        return false;
    }

    public void sitemapContentChanged(EList<Widget> widgets) {
        updateItemsAndWidgets(widgets);

        SitemapChangedEvent changeEvent = new SitemapChangedEvent();
        changeEvent.pageId = pageId;
        changeEvent.sitemapName = sitemapName;
        for (SitemapSubscriptionCallback callback : distinctCallbacks) {
            callback.onEvent(changeEvent);
        }
    }

    public void sendAliveEvent() {
        ServerAliveEvent aliveEvent = new ServerAliveEvent();
        aliveEvent.pageId = pageId;
        aliveEvent.sitemapName = sitemapName;
        for (SitemapSubscriptionCallback callback : distinctCallbacks) {
            callback.onEvent(aliveEvent);
        }
    }

}
