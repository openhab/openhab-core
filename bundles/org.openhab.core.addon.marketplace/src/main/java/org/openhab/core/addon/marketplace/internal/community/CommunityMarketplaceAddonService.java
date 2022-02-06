/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.openhab.core.addon.Addon.CODE_MATURITY_LEVELS;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.marketplace.AbstractRemoteAddonService;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscoursePosterInfo;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscourseTopicItem;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscourseUser;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponseDTO;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponseDTO.DiscoursePostLink;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.StorageService;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an {@link org.openhab.core.addon.AddonService} retrieving posts on community.openhab.org (Discourse).
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(immediate = true, configurationPid = CommunityMarketplaceAddonService.SERVICE_PID, //
        property = Constants.SERVICE_PID + "="
                + CommunityMarketplaceAddonService.SERVICE_PID, service = AddonService.class)
@ConfigurableService(category = "system", label = CommunityMarketplaceAddonService.SERVICE_NAME, description_uri = CommunityMarketplaceAddonService.CONFIG_URI)
@NonNullByDefault
public class CommunityMarketplaceAddonService extends AbstractRemoteAddonService {
    public static final String JAR_CONTENT_TYPE = "application/vnd.openhab.bundle";
    public static final String KAR_CONTENT_TYPE = "application/vnd.openhab.feature;type=karfile";
    public static final String RULETEMPLATES_CONTENT_TYPE = "application/vnd.openhab.ruletemplate";
    public static final String UIWIDGETS_CONTENT_TYPE = "application/vnd.openhab.uicomponent;type=widget";
    public static final String BLOCKLIBRARIES_CONTENT_TYPE = "application/vnd.openhab.uicomponent;type=blocks";

    // constants for the configuration properties
    static final String SERVICE_NAME = "Community Marketplace";
    static final String SERVICE_PID = "org.openhab.marketplace";
    static final String CONFIG_URI = "system:marketplace";
    static final String CONFIG_API_KEY = "apiKey";
    static final String CONFIG_SHOW_UNPUBLISHED_ENTRIES_KEY = "showUnpublished";

    private static final String COMMUNITY_BASE_URL = "https://community.openhab.org";
    private static final String COMMUNITY_MARKETPLACE_URL = COMMUNITY_BASE_URL + "/c/marketplace/69/l/latest";
    private static final String COMMUNITY_TOPIC_URL = COMMUNITY_BASE_URL + "/t/";

    private static final String SERVICE_ID = "marketplace";
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";

    private static final String JSON_CODE_MARKUP_START = "<pre><code class=\"lang-json\">";
    private static final String YAML_CODE_MARKUP_START = "<pre><code class=\"lang-yaml\">";
    private static final String CODE_MARKUP_END = "</code></pre>";

    private static final Integer BUNDLES_CATEGORY = 73;
    private static final Integer RULETEMPLATES_CATEGORY = 74;
    private static final Integer UIWIDGETS_CATEGORY = 75;
    private static final Integer BLOCKLIBRARIES_CATEGORY = 76;

    private static final String PUBLISHED_TAG = "published";

    private final Logger logger = LoggerFactory.getLogger(CommunityMarketplaceAddonService.class);

    private @Nullable String apiKey = null;
    private boolean showUnpublished = false;

    @Activate
    public CommunityMarketplaceAddonService(final @Reference EventPublisher eventPublisher,
            @Reference ConfigurationAdmin configurationAdmin, @Reference StorageService storageService,
            Map<String, Object> config) {
        super(eventPublisher, configurationAdmin, storageService, SERVICE_PID);
        modified(config);
    }

    @Modified
    public void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            this.apiKey = (String) config.get(CONFIG_API_KEY);
            Object showUnpublishedConfigValue = config.get(CONFIG_SHOW_UNPUBLISHED_ENTRIES_KEY);
            this.showUnpublished = showUnpublishedConfigValue != null
                    && "true".equals(showUnpublishedConfigValue.toString());
            cachedRemoteAddons.invalidateValue();
            refreshSource();
        }
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.add(handler);
    }

    @Override
    protected void removeAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.remove(handler);
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    protected List<Addon> getRemoteAddons() {
        List<Addon> addons = new ArrayList<>();
        try {
            List<DiscourseCategoryResponseDTO> pages = new ArrayList<>();

            URL url = new URL(COMMUNITY_MARKETPLACE_URL);
            int pageNb = 1;
            while (url != null) {
                URLConnection connection = url.openConnection();
                connection.addRequestProperty("Accept", "application/json");
                if (this.apiKey != null) {
                    connection.addRequestProperty("Api-Key", this.apiKey);
                }

                try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                    DiscourseCategoryResponseDTO parsed = gson.fromJson(reader, DiscourseCategoryResponseDTO.class);
                    if (parsed.topicList.topics.length != 0) {
                        pages.add(parsed);
                    }

                    if (parsed.topicList.moreTopicsUrl != null) {
                        // Discourse URL for next page is wrong
                        url = new URL(COMMUNITY_MARKETPLACE_URL + "?page=" + pageNb++);
                    } else {
                        url = null;
                    }
                }
            }

            List<DiscourseUser> users = pages.stream().flatMap(p -> Stream.of(p.users)).collect(Collectors.toList());
            pages.stream().flatMap(p -> Stream.of(p.topicList.topics))
                    .filter(t -> showUnpublished || Arrays.asList(t.tags).contains(PUBLISHED_TAG))
                    .map(t -> convertTopicItemToAddon(t, users)).forEach(addons::add);
        } catch (Exception e) {
            logger.error("Unable to retrieve marketplace add-ons", e);
        }
        return addons;
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        String fullId = ADDON_ID_PREFIX + id;
        // check if it is an installed add-on (cachedAddons also contains possibly incomplete results from the remote
        // side, we need to retrieve them from Discourse)
        if (installedAddons.contains(id)) {
            return cachedAddons.stream().filter(e -> fullId.equals(e.getId())).findAny().orElse(null);
        }

        if (!remoteEnabled()) {
            return null;
        }

        // retrieve from remote
        try {
            URL url = new URL(String.format("%s%s", COMMUNITY_TOPIC_URL, id.replace(ADDON_ID_PREFIX, "")));
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Accept", "application/json");
            if (this.apiKey != null) {
                connection.addRequestProperty("Api-Key", this.apiKey);
            }

            try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                DiscourseTopicResponseDTO parsed = gson.fromJson(reader, DiscourseTopicResponseDTO.class);
                return convertTopicToAddon(parsed);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        if (addonURI.toString().startsWith(COMMUNITY_TOPIC_URL)) {
            return addonURI.toString().substring(0, addonURI.toString().indexOf("/", COMMUNITY_BASE_URL.length()));
        }
        return null;
    }

    private @Nullable AddonType getAddonType(@Nullable Integer category, List<String> tags) {
        // check if we can determine the addon type from the category
        if (RULETEMPLATES_CATEGORY.equals(category)) {
            return TAG_ADDON_TYPE_MAP.get("automation");
        } else if (UIWIDGETS_CATEGORY.equals(category)) {
            return TAG_ADDON_TYPE_MAP.get("ui");
        } else if (BLOCKLIBRARIES_CATEGORY.equals(category)) {
            return TAG_ADDON_TYPE_MAP.get("automation");
        } else if (BUNDLES_CATEGORY.equals(category)) {
            // try to get it from tags if we have tags
            return tags.stream().map(TAG_ADDON_TYPE_MAP::get).filter(Objects::nonNull).findFirst().orElse(null);
        }

        // or return null
        return null;
    }

    private String getContentType(@Nullable Integer category, List<String> tags) {
        // check if we can determine the addon type from the category
        if (RULETEMPLATES_CATEGORY.equals(category)) {
            return RULETEMPLATES_CONTENT_TYPE;
        } else if (UIWIDGETS_CATEGORY.equals(category)) {
            return UIWIDGETS_CONTENT_TYPE;
        } else if (BLOCKLIBRARIES_CATEGORY.equals(category)) {
            return BLOCKLIBRARIES_CONTENT_TYPE;
        } else if (BUNDLES_CATEGORY.equals(category)) {
            if (tags.contains("kar")) {
                return KAR_CONTENT_TYPE;
            } else {
                // default to plain jar bundle for addons
                return JAR_CONTENT_TYPE;
            }
        }

        // empty string if content type could not be defined
        return "";
    }

    /**
     * Transforms a {@link DiscourseTopicItem} to a {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private Addon convertTopicItemToAddon(DiscourseTopicItem topic, List<DiscourseUser> users) {
        List<String> tags = Arrays.asList(Objects.requireNonNullElse(topic.tags, new String[0]));

        String id = ADDON_ID_PREFIX + topic.id.toString();
        AddonType addonType = getAddonType(topic.categoryId, tags);
        String type = (addonType != null) ? addonType.getId() : "";
        String contentType = getContentType(topic.categoryId, tags);

        String title = topic.title;
        String link = COMMUNITY_TOPIC_URL + topic.id.toString();
        int likeCount = topic.likeCount;
        int views = topic.views;
        int postsCount = topic.postsCount;
        Date createdDate = topic.createdAt;
        String author = "";
        for (DiscoursePosterInfo posterInfo : topic.posters) {
            if (posterInfo.description.contains("Original Poster")) {
                author = users.stream().filter(u -> u.id.equals(posterInfo.userId)).findFirst().get().name;
            }
        }

        String maturity = tags.stream().filter(CODE_MATURITY_LEVELS::contains).findAny().orElse(null);

        Map<String, Object> properties = Map.of("created_at", createdDate, //
                "like_count", likeCount, //
                "views", views, //
                "posts_count", postsCount, //
                "tags", tags.toArray(String[]::new));

        // try to use an handler to determine if the add-on is installed
        boolean installed = addonHandlers.stream()
                .anyMatch(handler -> handler.supports(type, contentType) && handler.isInstalled(id));

        return Addon.create(id).withType(type).withContentType(contentType).withImageLink(topic.imageUrl)
                .withAuthor(author).withProperties(properties).withLabel(title).withInstalled(installed)
                .withMaturity(maturity).withLink(link).build();
    }

    /**
     * Unescapes occurrences of XML entities found in the supplied content.
     *
     * @param content the content with potentially escaped entities
     * @return the unescaped content
     */
    private String unescapeEntities(String content) {
        return content.replace("&quot;", "\"").replace("&amp;", "&").replace("&apos;", "'").replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    /**
     * Transforms a {@link DiscourseTopicResponseDTO} to a {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private Addon convertTopicToAddon(DiscourseTopicResponseDTO topic) {
        String id = ADDON_ID_PREFIX + topic.id.toString();
        List<String> tags = Arrays.asList(Objects.requireNonNullElse(topic.tags, new String[0]));

        AddonType addonType = getAddonType(topic.categoryId, tags);
        String type = (addonType != null) ? addonType.getId() : "";
        String contentType = getContentType(topic.categoryId, tags);

        int likeCount = topic.likeCount;
        int views = topic.views;
        int postsCount = topic.postsCount;
        Date createdDate = topic.postStream.posts[0].createdAt;
        Date updatedDate = topic.postStream.posts[0].updatedAt;
        Date lastPostedDate = topic.lastPosted;

        String maturity = tags.stream().filter(CODE_MATURITY_LEVELS::contains).findAny().orElse(null);

        Map<String, Object> properties = new HashMap<>(10);
        properties.put("created_at", createdDate);
        properties.put("updated_at", updatedDate);
        properties.put("last_posted", lastPostedDate);
        properties.put("like_count", likeCount);
        properties.put("views", views);
        properties.put("posts_count", postsCount);
        properties.put("tags", tags.toArray(String[]::new));

        String detailedDescription = topic.postStream.posts[0].cooked;

        // try to extract contents or links
        if (topic.postStream.posts[0].linkCounts != null) {
            for (DiscoursePostLink postLink : topic.postStream.posts[0].linkCounts) {
                if (postLink.url.endsWith(".jar")) {
                    properties.put("jar_download_url", postLink.url);
                }
                if (postLink.url.endsWith(".kar")) {
                    properties.put("kar_download_url", postLink.url);
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
            properties.put("json_content", unescapeEntities(jsonContent));
        }
        if (detailedDescription.contains(YAML_CODE_MARKUP_START)) {
            String yamlContent = detailedDescription.substring(
                    detailedDescription.indexOf(YAML_CODE_MARKUP_START) + YAML_CODE_MARKUP_START.length(),
                    detailedDescription.indexOf(CODE_MARKUP_END, detailedDescription.indexOf(YAML_CODE_MARKUP_START)));
            properties.put("yaml_content", unescapeEntities(yamlContent));
        }

        // try to use a handler to determine if the add-on is installed
        boolean installed = addonHandlers.stream()
                .anyMatch(handler -> handler.supports(type, contentType) && handler.isInstalled(id));

        return Addon.create(id).withType(type).withContentType(contentType).withLabel(topic.title)
                .withImageLink(topic.imageUrl).withLink(COMMUNITY_TOPIC_URL + topic.id.toString())
                .withAuthor(topic.postStream.posts[0].displayUsername).withMaturity(maturity)
                .withDetailedDescription(detailedDescription).withInstalled(installed).withProperties(properties)
                .build();
    }
}
