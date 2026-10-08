/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import static org.openhab.core.addon.marketplace.MarketplaceConstants.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.marketplace.AbstractRemoteAddonService;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscoursePosterInfo;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscourseTopicItem;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscourseUser;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponseDTO;
import org.openhab.core.common.VersionRange;
import org.openhab.core.config.core.ConfigParser;
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
    public static final String CODE_CONTENT_SUFFIX = "_content";
    public static final String JSON_CONTENT_PROPERTY = "json" + CODE_CONTENT_SUFFIX;
    public static final String YAML_CONTENT_PROPERTY = "yaml" + CODE_CONTENT_SUFFIX;

    // constants for the configuration properties
    static final String SERVICE_NAME = "Community Marketplace";
    static final String SERVICE_PID = "org.openhab.marketplace";
    static final String CONFIG_URI = "system:marketplace";
    static final String CONFIG_API_KEY = "apiKey";
    static final String CONFIG_SHOW_UNPUBLISHED_ENTRIES_KEY = "showUnpublished";
    static final String CONFIG_ENABLED_KEY = "enable";

    private static final String COMMUNITY_BASE_URL = "https://community.openhab.org";
    private static final String COMMUNITY_MARKETPLACE_URL = COMMUNITY_BASE_URL + "/c/marketplace/69/l/latest";
    private static final String COMMUNITY_TOPIC_URL = COMMUNITY_BASE_URL + "/t/";
    private static final Pattern BUNDLE_NAME_PATTERN = Pattern.compile(".*/(.*?)-\\d+\\.\\d+\\.\\d+.*");

    private static final String SERVICE_ID = "marketplace";
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";

    private static final Pattern CODE_MARKUP_PATTERN = Pattern.compile(
            "<pre(?: data-code-wrap=\"[-a-zA-Z]+\")?><code class=\"lang-(?<lang>[-a-zA-Z]+)\">(?<content>.*?)</code></pre>\\n?",
            Pattern.DOTALL);
    private static final Pattern LAST_RESOURCE_LINK_PATTERN = Pattern.compile(
            ".*href=\"(?<url>[^\"]+\\.(?<extension>jar|kar|json|yaml))\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Integer BUNDLES_CATEGORY = 73;
    private static final Integer RULETEMPLATES_CATEGORY = 74;
    private static final Integer UIWIDGETS_CATEGORY = 75;
    private static final Integer BLOCKLIBRARIES_CATEGORY = 76;
    private static final Integer TRANSFORMATIONS_CATEGORY = 80;

    private static final String PUBLISHED_TAG = "published";

    private final Logger logger = LoggerFactory.getLogger(CommunityMarketplaceAddonService.class);

    private @Nullable String apiKey = null;
    private boolean showUnpublished = false;
    private boolean enabled = true;

    @Activate
    public CommunityMarketplaceAddonService(final @Reference EventPublisher eventPublisher,
            @Reference ConfigurationAdmin configurationAdmin, @Reference StorageService storageService,
            @Reference AddonInfoRegistry addonInfoRegistry, Map<String, Object> config) {
        super(eventPublisher, configurationAdmin, storageService, addonInfoRegistry, SERVICE_PID);
        modified(config);
    }

    @Modified
    public void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            this.apiKey = (String) config.get(CONFIG_API_KEY);
            this.showUnpublished = ConfigParser.valueAsOrElse(config.get(CONFIG_SHOW_UNPUBLISHED_ENTRIES_KEY),
                    Boolean.class, false);
            this.enabled = ConfigParser.valueAsOrElse(config.get(CONFIG_ENABLED_KEY), Boolean.class, true);
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
        if (!enabled) {
            return List.of();
        }

        List<Addon> addons = new ArrayList<>();
        try {
            List<DiscourseCategoryResponseDTO> pages = new ArrayList<>();

            URL url = URI.create(COMMUNITY_MARKETPLACE_URL).toURL();
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
                        url = URI.create(COMMUNITY_MARKETPLACE_URL + "?page=" + pageNb++).toURL();
                    } else {
                        url = null;
                    }
                }
            }

            List<DiscourseUser> users = pages.stream().flatMap(p -> Stream.of(p.users)).toList();
            pages.stream().flatMap(p -> Stream.of(p.topicList.topics)).filter(t -> showUnpublished
                    || (t.tags != null && Arrays.stream(t.tags).anyMatch(tag -> PUBLISHED_TAG.equals(tag.name))))
                    .map(t -> Optional.ofNullable(convertTopicItemToAddon(t, users)))
                    .forEach(a -> a.ifPresent(addons::add));
        } catch (Exception e) {
            logger.warn("Unable to retrieve marketplace add-ons: {}", e.getMessage());
        }
        return addons;
    }

    @Override
    public @Nullable Addon getAddon(String uid, @Nullable Locale locale) {
        String queryId = uid.startsWith(ADDON_ID_PREFIX) ? uid : ADDON_ID_PREFIX + uid;

        // check if it is an installed add-on (cachedAddons also contains possibly incomplete results from the remote
        // side, we need to retrieve them from Discourse)

        if (installedAddonIds.contains(queryId)) {
            return cachedAddons.stream().filter(e -> queryId.equals(e.getUid())).findAny().orElse(null);
        }

        if (!remoteEnabled()) {
            return null;
        }

        // retrieve from remote
        try {
            URL url = URI.create(COMMUNITY_TOPIC_URL + uid.replace(ADDON_ID_PREFIX, "")).toURL();
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
            logger.debug("An error occurred while creating add-on for '{}': {}", uid, e.getMessage());
            logger.trace("", e);
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
        if (TRANSFORMATIONS_CATEGORY.equals(category)) {
            return AddonType.TRANSFORMATION;
        } else if (RULETEMPLATES_CATEGORY.equals(category)) {
            return AddonType.AUTOMATION;
        } else if (UIWIDGETS_CATEGORY.equals(category)) {
            return AddonType.UI;
        } else if (BLOCKLIBRARIES_CATEGORY.equals(category)) {
            return AddonType.AUTOMATION;
        } else if (BUNDLES_CATEGORY.equals(category)) {
            // try to get it from tags if we have tags
            return AddonType.DEFAULT_TYPES.stream().filter(type -> tags.contains(type.getId())).findFirst()
                    .orElse(null);
        }

        // or return null
        return null;
    }

    private String getContentType(@Nullable Integer category, List<String> tags) {
        // check if we can determine the addon type from the category
        if (TRANSFORMATIONS_CATEGORY.equals(category)) {
            return TRANSFORMATIONS_CONTENT_TYPE;
        } else if (RULETEMPLATES_CATEGORY.equals(category)) {
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
     * Transforms a {@link DiscourseTopicItem} to an {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private @Nullable Addon convertTopicItemToAddon(DiscourseTopicItem topic, List<DiscourseUser> users) {
        try {
            List<String> tags = topic.tags == null ? List.of() : Arrays.stream(topic.tags).map(t -> t.name).toList();

            String uid = ADDON_ID_PREFIX + topic.id.toString();
            AddonType addonType = getAddonType(topic.categoryId, tags);
            if (addonType == null) {
                logger.debug("Ignoring topic '{}' because no add-on type could be found", topic.id);
                return null;
            }
            String type = addonType.getId();
            String id = topic.id.toString(); // this will be replaced after installation by the correct id if available
            String contentType = getContentType(topic.categoryId, tags);

            String title = topic.title;
            boolean compatible = true;

            Matcher matcher = VersionRange.RANGE_PATTERN.matcher(title);
            if (matcher.find()) {
                try {
                    compatible = VersionRange.valueOf(matcher.group().trim()).includes(coreVersion);
                    title = title.substring(0, matcher.start());
                    logger.debug("{} is {}compatible with core version {}", topic.title, compatible ? "" : "NOT ",
                            coreVersion);
                } catch (IllegalArgumentException e) {
                    logger.debug("Failed to determine compatibility for add-on {}: {}", topic.title, e.getMessage());
                    compatible = true;
                }
            } else {
                logger.trace("No version range pattern found for add-on {}", topic.title);
            }

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

            // try to use a handler to determine if the add-on is installed
            boolean installed = addonHandlers.stream()
                    .anyMatch(handler -> handler.supports(type, contentType) && handler.isInstalled(uid));

            return Addon.create(uid).withType(type).withId(id).withContentType(contentType)
                    .withImageLink(topic.imageUrl).withAuthor(author).withProperties(properties).withLabel(title)
                    .withInstalled(installed).withMaturity(maturity).withCompatible(compatible).withLink(link).build();
        } catch (RuntimeException e) {
            logger.debug("Ignoring marketplace add-on '{}' due: {}", topic.title, e.getMessage());
            return null;
        }
    }

    /**
     * Unescapes occurrences of XML entities found in the supplied content.
     *
     * @param content the content with potentially escaped entities
     * @return the unescaped content
     */
    private String unescapeEntities(String content) {
        return content.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    /**
     * Transforms a {@link DiscourseTopicResponseDTO} to an {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private Addon convertTopicToAddon(DiscourseTopicResponseDTO topic) {
        String uid = ADDON_ID_PREFIX + topic.id.toString();
        List<String> tags = topic.tags == null ? List.of() : Arrays.stream(topic.tags).map(t -> t.name).toList();

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
        if (createdDate != null) {
            properties.put("created_at", createdDate);
        }
        if (updatedDate != null) {
            properties.put("updated_at", updatedDate);
        }
        if (lastPostedDate != null) {
            properties.put("last_posted", lastPostedDate);
        }
        properties.put("like_count", likeCount);
        properties.put("views", views);
        properties.put("posts_count", postsCount);
        properties.put("tags", tags.toArray(String[]::new));

        String detailedDescription = topic.postStream.posts[0].cooked;
        String id = null;

        Matcher matcher = LAST_RESOURCE_LINK_PATTERN.matcher(detailedDescription);
        if (matcher.find()) {
            try {
                URI uri = processResourceURL(matcher.group("url"));
                String path = uri.getPath();
                if (path != null) {
                    switch (matcher.group("extension").toLowerCase(Locale.ROOT)) {
                        case "jar":
                            properties.put(JAR_DOWNLOAD_URL_PROPERTY, uri.toString());
                            id = determineIdFromUrl(path);
                            break;
                        case "kar":
                            properties.put(KAR_DOWNLOAD_URL_PROPERTY, uri.toString());
                            id = determineIdFromUrl(path);
                            break;
                        case "json":
                            properties.put(JSON_DOWNLOAD_URL_PROPERTY, uri.toString());
                            break;
                        case "yaml":
                            properties.put(YAML_DOWNLOAD_URL_PROPERTY, uri.toString());
                            break;
                    }
                } else {
                    logger.debug(
                            "Failed to extract path from resource URL for marketplace add-on '{}'. This should be impossible",
                            topic.title);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Add-on '{}' ({}) has an invalid resource URL '{}': {}", topic.title, topic.id,
                        matcher.group("url"), e.getMessage());
            }
        }

        if (id == null) {
            id = topic.id.toString(); // this is a fallback if we couldn't find a better id
        }

        matcher = CODE_MARKUP_PATTERN.matcher(detailedDescription);
        if (matcher.find()) {
            properties.put(matcher.group("lang") + CODE_CONTENT_SUFFIX, unescapeEntities(matcher.group("content")));
        }

        // try to use a handler to determine if the add-on is installed
        boolean installed = addonHandlers.stream()
                .anyMatch(handler -> handler.supports(type, contentType) && handler.isInstalled(uid));

        String title = topic.title;
        boolean compatible = true;
        matcher = VersionRange.RANGE_PATTERN.matcher(title);
        if (matcher.find()) {
            compatible = VersionRange.valueOf(matcher.group().trim()).includes(coreVersion);
            title = matcher.replaceFirst("").trim();
        }

        Addon.Builder builder = Addon.create(uid).withType(type).withId(id).withContentType(contentType)
                .withCompatible(compatible).withLabel(title).withImageLink(topic.imageUrl)
                .withLink(COMMUNITY_TOPIC_URL + topic.id.toString())
                .withAuthor(topic.postStream.posts[0].displayUsername).withMaturity(maturity)
                .withDetailedDescription(detailedDescription).withInstalled(installed).withProperties(properties);

        return builder.build();
    }

    private URI processResourceURL(String url) throws IllegalArgumentException {
        URI uri;
        try {
            uri = new URI(url);
            if (uri.getFragment() != null) {
                throw new IllegalArgumentException("Fragment not allowed in resource URLs: " + uri.getFragment());
            }
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Missing host in resource URL: " + url);
            }
            String query, path;
            switch (host) {
                case "github.com":
                case "www.github.com":
                    // Modify GitHub URLs to use their "raw" version if necessary
                    path = uri.getPath();
                    if (path != null && path.contains("/blob/")) {
                        query = uri.getQuery();
                        if (query == null) {
                            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), "raw=true", null);
                        }
                        if (!query.contains("raw=true")) {
                            String[] parts = query.split("&");
                            String[] newParts = new String[parts.length + 1];
                            System.arraycopy(parts, 0, newParts, 0, parts.length);
                            newParts[parts.length] = "raw=true";
                            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                                    String.join("&", newParts), null);
                        }
                    }
                    break;
                default:
                    break;
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private @Nullable String determineIdFromUrl(String url) {
        Matcher matcher = BUNDLE_NAME_PATTERN.matcher(url);
        if (matcher.matches()) {
            String bundleName = matcher.group(1);
            return bundleName.substring(bundleName.lastIndexOf(".") + 1);
        } else {
            logger.warn("Could not determine bundle name from url: {}", url);
        }
        return null;
    }
}
