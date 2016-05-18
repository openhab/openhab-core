/**
 * Copyright (c) 2015-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.internal.item;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.setup.ThingSetupManager;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapFactory;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.eclipse.smarthome.model.sitemap.impl.FrameImpl;
import org.eclipse.smarthome.model.sitemap.impl.GroupImpl;
import org.eclipse.smarthome.model.sitemap.impl.SitemapImpl;

/**
 * This class dynamically provides a default sitemap which comprises
 * all group items that do not have any parent group.
 *
 * @author Kai Kreuzer
 *
 */
public class DefaultSitemapProvider implements SitemapProvider {

    private static final String SITEMAP_NAME = "_default";

    private ItemRegistry itemRegistry;
    private ThingRegistry thingRegistry;
    private ItemChannelLinkRegistry linkRegistry;

    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

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
                    GroupImpl group = (GroupImpl) SitemapFactory.eINSTANCE.createGroup();
                    group.setItem(thing.getUID().getAsString());
                    group.setLabel(thing.getLabel());
                    // String category = thing.getCategory();
//                    if (category != null) {
//                        group.setIcon(category);
//                    }


                    for(Channel channel : thing.getChannels()) {
                        mainFrame.getChildren().add(group);
                    }
                        thingFrame.getChildren().add(group);
                    }
                }
            }

    if(!mainFrame.getChildren().isEmpty())

    {
        sitemap.getChildren().add(mainFrame);
    } if(!thingFrame.getChildren().isEmpty())

    {
        sitemap.getChildren().add(thingFrame);
    }

    return sitemap;

    }return null;}

    @Override
    public Set<String> getSitemapNames() {
        return Collections.singleton(SITEMAP_NAME);
    }

}
