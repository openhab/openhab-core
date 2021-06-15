/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.addon.marketplace.internal.community;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponse;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponse.DiscoursePosterInfo;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponse.DiscourseTopicItem;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponse.DiscourseUser;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponse;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponse.DiscoursePostLink;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class is a {@link AddonService} retrieving posts on community.openhab.org (Discourse).
 *
 * @author Yannick Schaus - Initial contribution
 *
 */
@Component(immediate = true, configurationPid = "org.openhab.marketplace", //
        property = Constants.SERVICE_PID + "=org.openhab.marketplace")
@ConfigurableService(category = "system", label = "Community Marketplace", description_uri = CommunityMarketplaceAddonService.CONFIG_URI)
public class CommunityMarketplaceAddonService implements AddonService {

    // constants for the configuration properties
    static final String CONFIG_URI = "system:marketplace";
    static final String CONFIG_API_KEY = "apiKey";

    private final Logger logger = LoggerFactory.getLogger(CommunityMarketplaceAddonService.class);

    private static final String COMMUNITY_BASE_URL = "https://community.openhab.org";
    private static final String COMMUNITY_GALLERY_URL = COMMUNITY_BASE_URL + "/c/marketplace/69/l/latest";
    private static final String COMMUNITY_TOPIC_URL = COMMUNITY_BASE_URL + "/t/";

    private static final String ADDON_ID_PREFIX = "marketplace:";

    private static final String JSON_CODE_MARKUP_START = "<pre><code class=\"lang-json\">";
    private static final String YAML_CODE_MARKUP_START = "<pre><code class=\"lang-yaml\">";
    private static final String CODE_MARKUP_END = "</code></pre>";

    private HashMap<Integer, AddonType> types = new HashMap<Integer, AddonType>(3);
    private static final Integer BINDINGS_CATEGORY = 73;
    private static final Integer RULETEMPLATES_CATEGORY = 74;
    private static final Integer UIWIDGETS_CATEGORY = 75;

    private HashMap<String, String> contentTypes = new HashMap<String, String>(3);
    private static final String BINDINGS_CONTENT_TYPE = "application/java-archive";
    private static final String RULETEMPLATES_CONTENT_TYPE = "application/vnd.openhab.ruletemplate";
    private static final String UIWIDGETS_CONTENT_TYPE = "application/vnd.openhab.uicomponent;type=widget";

    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();

    private final Set<MarketplaceAddonHandler> addonHandlers = new HashSet<>();
    private EventPublisher eventPublisher;
    private String apiKey = null;

    @Activate
    protected void activate(Map<String, Object> config) {
        types.put(BINDINGS_CATEGORY, new AddonType("binding", "Bindings"));
        types.put(RULETEMPLATES_CATEGORY, new AddonType("automation", "Automation"));
        types.put(UIWIDGETS_CATEGORY, new AddonType("ui", "User Interfaces"));
        contentTypes.put("binding", BINDINGS_CONTENT_TYPE);
        contentTypes.put("automation", RULETEMPLATES_CONTENT_TYPE);
        contentTypes.put("ui", UIWIDGETS_CONTENT_TYPE);
        modified(config);
    }

    @Modified
    void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            this.apiKey = (String) config.get(CONFIG_API_KEY);
        }
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.add(handler);
    }

    protected void removeAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.remove(handler);
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Override
    public String getId() {
        return "marketplace";
    }

    @Override
    public String getName() {
        return "Community Marketplace";
    }

    @Override
    public void refreshSource() {
    }

    @Override
    public List<Addon> getAddons(Locale locale) {
        try {
            List<DiscourseCategoryResponse> pages = new ArrayList<DiscourseCategoryResponse>();

            URL url = new URL(COMMUNITY_GALLERY_URL);
            int pageNb = 1;
            while (url != null) {
                URLConnection connection = url.openConnection();
                connection.addRequestProperty("Accept", "application/json");
                if (this.apiKey != null) {
                    connection.addRequestProperty("Api-Key", this.apiKey);
                }

                try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                    DiscourseCategoryResponse parsed = gson.fromJson(reader, DiscourseCategoryResponse.class);
                    pages.add(parsed);

                    if (parsed.topic_list.more_topics_url != null) {
                        // Discourse URL for next page is wrong
                        url = new URL(COMMUNITY_GALLERY_URL + "?page=" + pageNb++);
                    } else {
                        url = null;
                    }
                }
            }

            List<DiscourseUser> users = pages.stream().flatMap(p -> Stream.of(p.users)).collect(Collectors.toList());
            return pages.stream().flatMap(p -> Stream.of(p.topic_list.topics))
                    .map(t -> convertTopicItemToAddon(t, users)).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Unable to retrieve marketplace add-ons", e);
            return new ArrayList<Addon>();
        }
    }

    @Override
    public Addon getAddon(String id, Locale locale) {
        URL url;
        try {
            url = new URL(String.format("%s%s", COMMUNITY_TOPIC_URL, id.replace(ADDON_ID_PREFIX, "")));
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Accept", "application/json");
            if (this.apiKey != null) {
                connection.addRequestProperty("Api-Key", this.apiKey);
            }

            try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                DiscourseTopicResponse parsed = gson.fromJson(reader, DiscourseTopicResponse.class);
                return convertTopicToAddon(parsed);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<AddonType> getTypes(Locale locale) {
        return new ArrayList<AddonType>(types.values());
    }

    @Override
    public void install(String id) {
        Addon addon = getAddon(id, null);
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(addon.getType(), addon.getContentType())) {
                if (!handler.isInstalled(addon.getId())) {
                    try {
                        handler.install(addon);
                        postInstalledEvent(id);
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(id, e.getMessage());
                    }
                } else {
                    postFailureEvent(id, "Add-on is already installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public void uninstall(String id) {
        Addon addon = getAddon(id, null);
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(addon.getType(), addon.getContentType())) {
                if (handler.isInstalled(addon.getId())) {
                    try {
                        handler.uninstall(addon);
                        postUninstalledEvent(id);
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(id, e.getMessage());
                    }
                } else {
                    postFailureEvent(id, "Add-on is not installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public String getAddonId(URI addonURI) {
        // TODO Auto-generated method stub
        return "";
    }

    /**
     * Transforms a {@link DiscourseTopicItem} to a {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private Addon convertTopicItemToAddon(DiscourseTopicItem topic, List<DiscourseUser> users) {
        String id = ADDON_ID_PREFIX + topic.id.toString();
        AddonType addonType = types.get(topic.category_id);
        String type = (addonType != null) ? addonType.getId() : "";
        String contentType = (contentTypes.get(type) != null) ? contentTypes.get(type) : "";
        String version = "";
        String title = topic.title;
        String link = COMMUNITY_TOPIC_URL + topic.id.toString();
        int likeCount = topic.like_count;
        int views = topic.views;
        int postsCount = topic.posts_count;
        String[] tags = topic.tags;
        Date createdDate = topic.created_at;
        String author = "";
        boolean verifiedAuthor = false;
        for (DiscoursePosterInfo posterInfo : topic.posters) {
            if (posterInfo.description.contains("Original Poster")) {
                author = users.stream().filter(u -> u.id.equals(posterInfo.user_id)).findFirst().get().name;
            }
        }

        HashMap<String, Object> properties = new HashMap<>(10);
        properties.put("created_at", createdDate);
        properties.put("like_count", likeCount);
        properties.put("views", views);
        properties.put("posts_count", postsCount);
        properties.put("tags", tags);

        String description = "";
        String detailedDescription = "";

        // try to use an handler to determine if the add-on is installed
        boolean installed = false;
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(type, contentType)) {
                if (handler.isInstalled(id)) {
                    installed = true;
                }
            }
        }

        String configDescriptionURI = "";
        String keywords = "";
        String countries = "";
        String connection = "";
        String backgroundColor = "";
        String imageLink = topic.image_url;
        Addon addon = new Addon(id, type, title, version, contentType, link, author, verifiedAuthor, installed,
                description, detailedDescription, configDescriptionURI, keywords, countries, connection,
                backgroundColor, imageLink, properties);
        return addon;
    }

    /**
     * Transforms a {@link DiscourseTopicResponse} to a {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private Addon convertTopicToAddon(DiscourseTopicResponse topic) {
        String id = ADDON_ID_PREFIX + topic.id.toString();
        AddonType addonType = types.get(topic.category_id);
        String type = (addonType != null) ? addonType.getId() : "";
        String contentType = contentTypes.get(type);
        String version = "";
        String title = topic.title;
        String link = COMMUNITY_TOPIC_URL + topic.id.toString();
        int likeCount = topic.like_count;
        int views = topic.views;
        int postsCount = topic.posts_count;
        String[] tags = topic.tags;
        Date createdDate = topic.post_stream.posts[0].created_at;
        Date updatedDate = topic.post_stream.posts[0].updated_at;
        Date lastPostedDate = topic.last_posted;
        String author = topic.post_stream.posts[0].display_username;
        boolean verifiedAuthor = false;

        HashMap<String, Object> properties = new HashMap<>(10);
        properties.put("created_at", createdDate);
        properties.put("updated_at", updatedDate);
        properties.put("last_posted", lastPostedDate);
        properties.put("like_count", likeCount);
        properties.put("views", views);
        properties.put("posts_count", postsCount);
        properties.put("tags", tags);

        String description = "";
        String detailedDescription = topic.post_stream.posts[0].cooked;

        // try to extract contents or links
        if (topic.post_stream.posts[0].link_counts != null) {
            for (DiscoursePostLink postLink : topic.post_stream.posts[0].link_counts) {
                if (postLink.url.endsWith(".jar")) {
                    properties.put("jar_download_url", postLink.url);
                }
                if (postLink.url.endsWith(".json")) {
                    properties.put("json_download_url", postLink.url);
                }
                if (postLink.url.endsWith(".yaml")) {
                    properties.put("yaml_download_url", postLink.url);
                }
            }
        }
        if (detailedDescription.contains(JSON_CODE_MARKUP_START)) {
            String jsonContent = detailedDescription.substring(
                    detailedDescription.indexOf(JSON_CODE_MARKUP_START) + JSON_CODE_MARKUP_START.length(),
                    detailedDescription.indexOf(CODE_MARKUP_END, detailedDescription.indexOf(JSON_CODE_MARKUP_START)));
            properties.put("json_content", HtmlUnescapeUtil.unescapeHtml3(jsonContent));
        }
        if (detailedDescription.contains(YAML_CODE_MARKUP_START)) {
            String yamlContent = detailedDescription.substring(
                    detailedDescription.indexOf(YAML_CODE_MARKUP_START) + YAML_CODE_MARKUP_START.length(),
                    detailedDescription.indexOf(CODE_MARKUP_END, detailedDescription.indexOf(YAML_CODE_MARKUP_START)));
            properties.put("yaml_content", HtmlUnescapeUtil.unescapeHtml3(yamlContent));
        }

        // try to use an handler to determine if the add-on is installed
        boolean installed = false;
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(type, (contentType != null) ? contentType : "")) {
                if (handler.isInstalled(id)) {
                    installed = true;
                }
            }
        }

        String configDescriptionURI = "";
        String keywords = "";
        String countries = "";
        String connection = "";
        String backgroundColor = "";
        Addon addon = new Addon(id, type, title, version, contentType, link, author, verifiedAuthor, installed,
                description, detailedDescription, configDescriptionURI, keywords, countries, connection,
                backgroundColor, null, properties);
        return addon;
    }

    private void postInstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonInstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postUninstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonUninstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postFailureEvent(String extensionId, String msg) {
        Event event = AddonEventFactory.createAddonFailureEvent(extensionId, msg);
        eventPublisher.post(event);
    }
}
