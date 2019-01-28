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
package org.eclipse.smarthome.io.rest.sitemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.events.ItemStatePredictedEvent;
import org.eclipse.smarthome.io.rest.sitemap.internal.PageChangeListener;
import org.eclipse.smarthome.io.rest.sitemap.internal.SitemapEvent;
import org.eclipse.smarthome.model.core.EventType;
import org.eclipse.smarthome.model.core.ModelRepositoryChangeListener;
import org.eclipse.smarthome.model.sitemap.LinkableWidget;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a service that provides the possibility to manage subscriptions to sitemaps.
 * As such subscriptions are stateful, they need to be created and removed upon disposal.
 * The subscription mechanism makes sure that only events for widgets of the currently active sitemap page are sent as
 * events to the subscriber.
 * For this to work correctly, the subscriber needs to make sure that setPageId is called whenever it switches to a new
 * page.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(service = { SitemapSubscriptionService.class,
        EventSubscriber.class }, configurationPid = "org.eclipse.smarthome.sitemapsubscription")
public class SitemapSubscriptionService implements ModelRepositoryChangeListener, EventSubscriber {

    private static final String SITEMAP_PAGE_SEPARATOR = "#";
    private static final String SITEMAP_SUFFIX = ".sitemap";
    private static final int DEFAULT_MAX_SUBSCRIPTIONS = 50;

    private final Logger logger = LoggerFactory.getLogger(SitemapSubscriptionService.class);

    public interface SitemapSubscriptionCallback {

        void onEvent(SitemapEvent event);

        void onRelease(String subscriptionId);
    }

    private ItemUIRegistry itemUIRegistry;
    private final List<SitemapProvider> sitemapProviders = new ArrayList<>();

    /* subscription id -> sitemap+page */
    private final Map<String, String> pageOfSubscription = new ConcurrentHashMap<>();

    /* subscription id -> callback */
    private final Map<String, SitemapSubscriptionCallback> callbacks = new ConcurrentHashMap<>();

    /* subscription id -> creation date */
    private final Map<String, Long> creationDates = new ConcurrentHashMap<>();

    /* sitemap+page -> listener */
    private final Map<String, PageChangeListener> pageChangeListeners = new ConcurrentHashMap<>();

    /* Max number of subscriptions at the same time */
    private int maxSubscriptions = DEFAULT_MAX_SUBSCRIPTIONS;

    public SitemapSubscriptionService() {
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        applyConfig(config);
    }

    @Deactivate
    protected void deactivate() {
        pageOfSubscription.clear();
        callbacks.clear();
        for (PageChangeListener listener : pageChangeListeners.values()) {
            listener.dispose();
        }
        pageChangeListeners.clear();
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        applyConfig(config);
    }

    private void applyConfig(Map<String, Object> config) {
        if (config == null) {
            return;
        }
        final String max = Objects.toString(config.get("maxSubscriptions"), null);
        if (max != null) {
            try {
                maxSubscriptions = Integer.parseInt(max);
            } catch (NumberFormatException e) {
                logger.debug("Setting 'maxSubscriptions' must be a number; value '{}' ignored.", max);
            }
        }
    }

    @Reference
    protected void setItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = itemUIRegistry;
    }

    protected void unsetItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSitemapProvider(SitemapProvider provider) {
        sitemapProviders.add(provider);
        provider.addModelChangeListener(this);
    }

    protected void removeSitemapProvider(SitemapProvider provider) {
        sitemapProviders.remove(provider);
        provider.removeModelChangeListener(this);
    }

    /**
     * Creates a new subscription with the given id.
     *
     * @param callback an instance that should receive the events
     * @returns a unique id that identifies the subscription or null if the limit of subscriptions is already reached
     */
    public String createSubscription(SitemapSubscriptionCallback callback) {
        if (maxSubscriptions >= 0 && callbacks.size() >= maxSubscriptions) {
            logger.debug("No new subscription delivered as limit ({}) is already reached", maxSubscriptions);
            return null;
        }
        String subscriptionId = UUID.randomUUID().toString();
        callbacks.put(subscriptionId, callback);
        creationDates.put(subscriptionId, System.currentTimeMillis());
        logger.debug("Created new subscription with id {} ({} active subscriptions for a max of {})", subscriptionId,
                callbacks.size(), maxSubscriptions);
        return subscriptionId;
    }

    /**
     * Removes an existing subscription
     *
     * @param subscriptionId the id of the subscription to remove
     */
    public void removeSubscription(String subscriptionId) {
        creationDates.remove(subscriptionId);
        callbacks.remove(subscriptionId);
        String sitemapPage = pageOfSubscription.remove(subscriptionId);
        if (sitemapPage != null && !pageOfSubscription.values().contains(sitemapPage)) {
            // this was the only subscription listening on this page, so we can dispose the listener
            PageChangeListener listener = pageChangeListeners.remove(sitemapPage);
            if (listener != null) {
                listener.dispose();
            }
        }
        logger.debug("Removed subscription with id {} ({} active subscriptions)", subscriptionId, callbacks.size());
    }

    /**
     * Checks whether a subscription with a given id (still) exists.
     *
     * @param subscriptionId the id of the subscription to check
     * @return true, if it exists, false otherwise
     */
    public boolean exists(String subscriptionId) {
        return callbacks.containsKey(subscriptionId);
    }

    /**
     * Retrieves the current page id for a subscription.
     *
     * @param subscriptionId the subscription to get the page id for
     * @return the id of the currently active page or null if no page is currently set for the subscription
     */
    public String getPageId(String subscriptionId) {
        String sitemapWithPageId = pageOfSubscription.get(subscriptionId);
        return (sitemapWithPageId == null) ? null : extractPageId(sitemapWithPageId);
    }

    /**
     * Retrieves the current sitemap name for a subscription.
     *
     * @param subscriptionId the subscription to get the sitemap name for
     * @return the name of the current sitemap or null if no sitemap is currently set for the subscription
     */
    public String getSitemapName(String subscriptionId) {
        String sitemapWithPageId = pageOfSubscription.get(subscriptionId);
        return (sitemapWithPageId == null) ? null : extractSitemapName(sitemapWithPageId);
    }

    private String extractSitemapName(String sitemapWithPageId) {
        return sitemapWithPageId.split(SITEMAP_PAGE_SEPARATOR)[0];
    }

    private String extractPageId(String sitemapWithPageId) {
        return sitemapWithPageId.split(SITEMAP_PAGE_SEPARATOR)[1];
    }

    /**
     * Updates the subscription to send events for the provided page id.
     *
     * @param subscriptionId the subscription to update
     * @param sitemapName the current sitemap name
     * @param pageId the current page id
     */
    public void setPageId(String subscriptionId, String sitemapName, String pageId) {
        SitemapSubscriptionCallback callback = callbacks.get(subscriptionId);
        if (callback != null) {
            String oldSitemapPage = pageOfSubscription.remove(subscriptionId);
            if (oldSitemapPage != null) {
                removeCallbackFromListener(oldSitemapPage, callback);
            }
            addCallbackToListener(sitemapName, pageId, callback);
            pageOfSubscription.put(subscriptionId, getValue(sitemapName, pageId));

            logger.debug("Subscription {} changed to page {} of sitemap {} ({} active subscriptions}",
                    new Object[] { subscriptionId, pageId, sitemapName, callbacks.size() });
        } else {
            throw new IllegalArgumentException("Subscription " + subscriptionId + " does not exist!");
        }
    }

    private void addCallbackToListener(String sitemapName, String pageId, SitemapSubscriptionCallback callback) {
        PageChangeListener listener = pageChangeListeners.get(getValue(sitemapName, pageId));
        if (listener == null) {
            // there is no listener for this page yet, so let's try to create one
            listener = new PageChangeListener(sitemapName, pageId, itemUIRegistry, collectWidgets(sitemapName, pageId));
            pageChangeListeners.put(getValue(sitemapName, pageId), listener);
        }
        if (listener != null) {
            listener.addCallback(callback);
        }
    }

    private EList<Widget> collectWidgets(String sitemapName, String pageId) {
        EList<Widget> widgets = new BasicEList<Widget>();

        Sitemap sitemap = getSitemap(sitemapName);
        if (sitemap != null) {
            if (pageId.equals(sitemap.getName())) {
                widgets = itemUIRegistry.getChildren(sitemap);
            } else {
                Widget pageWidget = itemUIRegistry.getWidget(sitemap, pageId);
                if (pageWidget instanceof LinkableWidget) {
                    widgets = itemUIRegistry.getChildren((LinkableWidget) pageWidget);
                    // We add the page widget. It will help any UI to update the page title.
                    widgets.add(pageWidget);
                }
            }
        }
        return widgets;
    }

    private void removeCallbackFromListener(String sitemapPage, SitemapSubscriptionCallback callback) {
        PageChangeListener oldListener = pageChangeListeners.get(sitemapPage);
        if (oldListener != null) {
            oldListener.removeCallback(callback);
            if (!pageOfSubscription.values().contains(sitemapPage)) {
                // no other callbacks are left here, so we can safely dispose the listener
                oldListener.dispose();
                pageChangeListeners.remove(sitemapPage);
            }
        }
    }

    private String getValue(String sitemapName, String pageId) {
        return sitemapName + SITEMAP_PAGE_SEPARATOR + pageId;
    }

    private Sitemap getSitemap(String sitemapName) {
        for (SitemapProvider provider : sitemapProviders) {
            Sitemap sitemap = provider.getSitemap(sitemapName);
            if (sitemap != null) {
                return sitemap;
            }
        }
        return null;
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (type != EventType.MODIFIED || !modelName.endsWith(SITEMAP_SUFFIX)) {
            return; // we process only sitemap modifications here
        }

        String changedSitemapName = StringUtils.removeEnd(modelName, SITEMAP_SUFFIX);

        for (Entry<String, PageChangeListener> listenerEntry : pageChangeListeners.entrySet()) {
            String sitemapWithPage = listenerEntry.getKey();
            String sitemapName = extractSitemapName(sitemapWithPage);
            String pageId = extractPageId(sitemapWithPage);

            if (sitemapName.equals(changedSitemapName)) {
                EList<Widget> widgets = collectWidgets(sitemapName, pageId);
                listenerEntry.getValue().sitemapContentChanged(widgets);
            }
        }
    }

    public void checkAliveClients() {
        // Release the subscriptions that are not attached to a page
        for (Entry<String, Long> dateEntry : creationDates.entrySet()) {
            String subscriptionId = dateEntry.getKey();
            SitemapSubscriptionCallback callback = callbacks.get(subscriptionId);
            if (getPageId(subscriptionId) == null && callback != null
                    && (dateEntry.getValue().longValue() + 30000) < System.currentTimeMillis()) {
                logger.debug("Release subscription {} as sitemap page is not set", subscriptionId);
                removeSubscription(subscriptionId);
                callback.onRelease(subscriptionId);
            }
        }
        // Send an ALIVE event to all subscribers to trigger an exception for dead subscribers
        for (Entry<String, PageChangeListener> listenerEntry : pageChangeListeners.entrySet()) {
            listenerEntry.getValue().sendAliveEvent();
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Collections.singleton(ItemStatePredictedEvent.TYPE);
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemStatePredictedEvent) {
            ItemStatePredictedEvent prediction = (ItemStatePredictedEvent) event;
            Item item = itemUIRegistry.get(prediction.getItemName());
            if (item instanceof GroupItem) {
                // don't send out auto-update events for group items as those will calculate their state based on their
                // members and predictions aren't really possible in that case (or at least would be highly complex).
                return;
            }
            for (PageChangeListener pageChangeListener : pageChangeListeners.values()) {
                if (prediction.isConfirmation()) {
                    pageChangeListener.keepCurrentState(item);
                } else {
                    pageChangeListener.changeStateTo(item, prediction.getPredictedState());
                }
            }
        }
    }
}
