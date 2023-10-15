/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.io.rest.sitemap.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.io.rest.core.item.EnrichedItemDTOMapper;
import org.openhab.core.io.rest.sitemap.SitemapSubscriptionService.SitemapSubscriptionCallback;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.events.GroupStateUpdatedEvent;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.model.sitemap.sitemap.Chart;
import org.openhab.core.model.sitemap.sitemap.ColorArray;
import org.openhab.core.model.sitemap.sitemap.Condition;
import org.openhab.core.model.sitemap.sitemap.Frame;
import org.openhab.core.model.sitemap.sitemap.IconRule;
import org.openhab.core.model.sitemap.sitemap.VisibilityRule;
import org.openhab.core.model.sitemap.sitemap.Widget;
import org.openhab.core.types.State;
import org.openhab.core.ui.items.ItemUIRegistry;

/**
 * This is a class that listens on item state change events and creates sitemap events for a dedicated sitemap page.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Added support for icon color
 * @author Laurent Garnier - Support added for multiple AND conditions in labelcolor/valuecolor/visibility
 * @author Laurent Garnier - New widget icon parameter based on conditional rules
 */
public class PageChangeListener implements EventSubscriber {

    private static final int REVERT_INTERVAL = 300;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    private final String sitemapName;
    private final String pageId;
    private final ItemUIRegistry itemUIRegistry;
    private EList<Widget> widgets;
    private Set<Item> items;
    private final HashSet<String> filterItems = new HashSet<>();
    private final List<SitemapSubscriptionCallback> callbacks = Collections.synchronizedList(new ArrayList<>());
    private Set<SitemapSubscriptionCallback> distinctCallbacks = Set.of();

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
        this.widgets = widgets;
        items = getAllItems(widgets);
        filterItems.clear();
        filterItems.addAll(items.stream().map(Item::getName).collect(Collectors.toSet()));
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
     * Collects all items that are represented by a given list of widgets
     *
     * @param widgets
     *            the widget list to get the items for added to all bundles containing REST resources
     * @return all items that are represented by the list of widgets
     */
    private Set<Item> getAllItems(EList<Widget> widgets) {
        Set<Item> items = new HashSet<>();
        if (itemUIRegistry != null) {
            for (Widget widget : widgets) {
                addItemWithName(items, widget.getItem());
                if (widget instanceof Frame frame) {
                    items.addAll(getAllItems(frame.getChildren()));
                }
                // now scan icon rules
                for (IconRule rule : widget.getIconRules()) {
                    addItemsFromConditions(items, rule.getConditions());
                }
                // now scan visibility rules
                for (VisibilityRule rule : widget.getVisibility()) {
                    addItemsFromConditions(items, rule.getConditions());
                }
                // now scan label color rules
                for (ColorArray rule : widget.getLabelColor()) {
                    addItemsFromConditions(items, rule.getConditions());
                }
                // now scan value color rules
                for (ColorArray rule : widget.getValueColor()) {
                    addItemsFromConditions(items, rule.getConditions());
                }
                // now scan icon color rules
                for (ColorArray rule : widget.getIconColor()) {
                    addItemsFromConditions(items, rule.getConditions());
                }
            }
        }
        return items;
    }

    private void addItemsFromConditions(Set<Item> items, @Nullable EList<Condition> conditions) {
        if (conditions != null) {
            for (Condition condition : conditions) {
                addItemWithName(items, condition.getItem());
            }
        }
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
            if (w instanceof Frame frame) {
                events.addAll(constructSitemapEvents(item, state, itemUIRegistry.getChildren(frame)));
            }

            boolean itemBelongsToWidget = w.getItem() != null && w.getItem().equals(item.getName());
            boolean skipWidget = !itemBelongsToWidget;
            // We skip the chart widgets having a refresh argument
            if (!skipWidget && w instanceof Chart chartWidget) {
                skipWidget = chartWidget.getRefresh() > 0;
            }
            if (!skipWidget || definesVisibilityOrColorOrIcon(w, item.getName())) {
                SitemapWidgetEvent event = constructSitemapEventForWidget(item, state, w);
                events.add(event);
            }
        }
        return events;
    }

    private SitemapWidgetEvent constructSitemapEventForWidget(Item item, State state, Widget widget) {
        SitemapWidgetEvent event = new SitemapWidgetEvent();
        event.sitemapName = sitemapName;
        event.pageId = pageId;
        event.label = itemUIRegistry.getLabel(widget);
        event.widgetId = itemUIRegistry.getWidgetId(widget);
        event.icon = itemUIRegistry.getCategory(widget);
        event.reloadIcon = widget.getStaticIcon() == null;
        event.visibility = itemUIRegistry.getVisiblity(widget);
        event.descriptionChanged = false;
        // event.item contains the (potentially changed) data of the item belonging to
        // the widget including its state (in event.item.state)
        boolean itemBelongsToWidget = widget.getItem() != null && widget.getItem().equals(item.getName());
        final Item itemToBeSent = itemBelongsToWidget ? item : getItemForWidget(widget);
        State stateToBeSent = null;
        if (itemToBeSent != null) {
            String widgetTypeName = widget.eClass().getInstanceTypeName()
                    .substring(widget.eClass().getInstanceTypeName().lastIndexOf(".") + 1);
            boolean drillDown = "mapview".equalsIgnoreCase(widgetTypeName);
            Predicate<Item> itemFilter = (i -> CoreItemFactory.LOCATION.equals(i.getType()));
            event.item = EnrichedItemDTOMapper.map(itemToBeSent, drillDown, itemFilter, null, null);

            // event.state is an adjustment of the item state to the widget type.
            stateToBeSent = itemBelongsToWidget ? state : itemToBeSent.getState();
            event.state = itemUIRegistry.convertState(widget, itemToBeSent, stateToBeSent).toFullString();
            // In case this state is identical to the item state, its value is set to null.
            if (event.state != null && event.state.equals(event.item.state)) {
                event.state = null;
            }
        }
        event.labelcolor = SitemapResource.convertItemValueColor(itemUIRegistry.getLabelColor(widget), stateToBeSent);
        event.valuecolor = SitemapResource.convertItemValueColor(itemUIRegistry.getValueColor(widget), stateToBeSent);
        event.iconcolor = SitemapResource.convertItemValueColor(itemUIRegistry.getIconColor(widget), stateToBeSent);
        return event;
    }

    private Item getItemForWidget(Widget w) {
        final String itemName = w.getItem();
        if (itemName != null) {
            try {
                return itemUIRegistry.getItem(itemName);
            } catch (ItemNotFoundException e) {
                // fall through to returning null
            }
        }
        return null;
    }

    private boolean definesVisibilityOrColorOrIcon(Widget w, String name) {
        return w.getVisibility().stream().anyMatch(r -> conditionsDependsOnItem(r.getConditions(), name))
                || w.getLabelColor().stream().anyMatch(r -> conditionsDependsOnItem(r.getConditions(), name))
                || w.getValueColor().stream().anyMatch(r -> conditionsDependsOnItem(r.getConditions(), name))
                || w.getIconColor().stream().anyMatch(r -> conditionsDependsOnItem(r.getConditions(), name))
                || w.getIconRules().stream().anyMatch(r -> conditionsDependsOnItem(r.getConditions(), name));
    }

    private boolean conditionsDependsOnItem(@Nullable EList<Condition> conditions, String name) {
        return conditions != null && conditions.stream().anyMatch(c -> name.equals(c.getItem()));
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

    public void descriptionChanged(String itemName) {
        try {
            Item item = itemUIRegistry.getItem(itemName);

            Set<SitemapEvent> events = constructSitemapEventsForUpdatedDescr(item, widgets);

            for (SitemapEvent event : events) {
                for (SitemapSubscriptionCallback callback : distinctCallbacks) {
                    callback.onEvent(event);
                }
            }
        } catch (ItemNotFoundException e) {
            // ignore
        }
    }

    private Set<SitemapEvent> constructSitemapEventsForUpdatedDescr(Item item, List<Widget> widgets) {
        Set<SitemapEvent> events = new HashSet<>();
        for (Widget w : widgets) {
            if (w instanceof Frame frame) {
                events.addAll(constructSitemapEventsForUpdatedDescr(item, itemUIRegistry.getChildren(frame)));
            }

            boolean itemBelongsToWidget = w.getItem() != null && w.getItem().equals(item.getName());
            if (itemBelongsToWidget) {
                SitemapWidgetEvent event = constructSitemapEventForWidget(item, item.getState(), w);
                event.descriptionChanged = true;
                events.add(event);
            }
        }
        return events;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemStateChangedEvent.TYPE, GroupStateUpdatedEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemEvent itemEvent && filterItems.contains(itemEvent.getItemName())) {
            Item item = itemUIRegistry.get(itemEvent.getItemName());
            if (item == null) {
                return;
            }
            if (event instanceof GroupStateUpdatedEvent groupStateUpdatedEvent) {
                constructAndSendEvents(item, groupStateUpdatedEvent.getItemState());
            } else if (event instanceof ItemStateChangedEvent itemStateChangedEvent) {
                constructAndSendEvents(item, itemStateChangedEvent.getItemState());
            }
        }
    }
}
