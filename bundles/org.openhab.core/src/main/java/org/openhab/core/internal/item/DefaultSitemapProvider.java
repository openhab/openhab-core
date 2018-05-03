/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.internal.item;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.model.core.ModelRepositoryChangeListener;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapFactory;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.eclipse.smarthome.model.sitemap.impl.DefaultImpl;
import org.eclipse.smarthome.model.sitemap.impl.FrameImpl;
import org.eclipse.smarthome.model.sitemap.impl.SitemapImpl;
import org.eclipse.smarthome.model.sitemap.impl.TextImpl;

/**
 * This class dynamically provides a default sitemap which comprises
 * all group items that do not have any parent group.
 *
 * @author Kai Kreuzer
 *
 */
public class DefaultSitemapProvider implements SitemapProvider {

    private static final String SITEMAP_NAME = "_default";

    private ThingRegistry thingRegistry;
    private ItemChannelLinkRegistry linkRegistry;

    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry linkRegistry) {
        this.linkRegistry = linkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry linkRegistry) {
        this.linkRegistry = null;
    }

    @Override
    public Sitemap getSitemap(String sitemapName) {
        if (sitemapName.equals(SITEMAP_NAME)) {
            SitemapImpl sitemap = (SitemapImpl) SitemapFactory.eINSTANCE.createSitemap();
            FrameImpl mainFrame = (FrameImpl) SitemapFactory.eINSTANCE.createFrame();

            FrameImpl thingFrame = (FrameImpl) SitemapFactory.eINSTANCE.createFrame();
            thingFrame.setLabel("Things");

            sitemap.setLabel("Home");
            sitemap.setName(SITEMAP_NAME);

            for (Thing thing : thingRegistry.getAll()) {
                TextImpl thingWidget = (TextImpl) SitemapFactory.eINSTANCE.createText();
                thingWidget.setLabel(thing.getLabel());
                thingWidget.setIcon("player");

                for (Channel channel : thing.getChannels()) {
                    Set<String> items = linkRegistry.getLinkedItemNames(channel.getUID());
                    if (!items.isEmpty()) {
                        DefaultImpl widget = (DefaultImpl) SitemapFactory.eINSTANCE.createDefault();
                        widget.setItem(items.iterator().next());
                        thingWidget.getChildren().add(widget);
                    }
                }
                if (!thingWidget.getChildren().isEmpty()) {
                    thingFrame.getChildren().add(thingWidget);
                }
            }

            if (!mainFrame.getChildren().isEmpty()) {
                sitemap.getChildren().add(mainFrame);
            }
            if (!thingFrame.getChildren().isEmpty()) {
                sitemap.getChildren().add(thingFrame);
            }

            return sitemap;

        }
        return null;

    }

    @Override
    public Set<String> getSitemapNames() {
        return Collections.singleton(SITEMAP_NAME);
    }

    @Override
    public void addModelChangeListener(ModelRepositoryChangeListener listener) {
    }

    @Override
    public void removeModelChangeListener(ModelRepositoryChangeListener listener) {
    }

}
