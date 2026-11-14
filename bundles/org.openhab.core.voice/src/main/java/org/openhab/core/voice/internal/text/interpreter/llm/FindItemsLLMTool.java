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
package org.openhab.core.voice.internal.text.interpreter.llm;

import static org.openhab.core.semantics.SemanticTags.METADATA_NAMESPACE;
import static org.openhab.core.semantics.SemanticTags.REL_HAS_LOCATION;
import static org.openhab.core.semantics.SemanticTags.REL_IS_PART_OF;
import static org.openhab.core.semantics.SemanticTags.REL_IS_POINT_OF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParam;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParamType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link FindItemsLLMTool} is an {@link LLMTool} that searches the accessible items by name, label, tag and the
 * labels of their locations, equipment and groups.
 * <p>
 * It's meant for Items that aren't in the system prompt (see the {@code promptItems} voice setting).
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
@Component(service = LLMTool.class, immediate = true)
public class FindItemsLLMTool implements LLMTool {
    public static final String ID = "item-find";

    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 25;
    private static final int MAX_PARENT_DEPTH = 5;
    private static final int MAX_CONTEXT_LABELS = 3;

    private final ItemRegistry itemRegistry;
    private final ItemPermissionResolver itemPermissionResolver;
    private final MetadataRegistry metadataRegistry;
    private final TimeZoneProvider timeZoneProvider;

    private record Match(Item item, List<String> context, int matchedTerms, int score) {
    }

    @Activate
    public FindItemsLLMTool(final @Reference ItemRegistry itemRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver,
            final @Reference MetadataRegistry metadataRegistry, final @Reference TimeZoneProvider timeZoneProvider) {
        this.itemRegistry = itemRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
        this.metadataRegistry = metadataRegistry;
        this.timeZoneProvider = timeZoneProvider;
    }

    @Override
    public String getUID() {
        return ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Find Items";
    }

    @Override
    public String getShortDescription(@Nullable Locale locale) {
        return "Searches Items by name, label, tag or location and returns them with their current state.";
    }

    @Override
    public String getDescription(@Nullable Locale locale) {
        return """
                Searches the accessible Items by name, label, tag and the labels of their locations, equipment and groups.
                Use it when the user refers to a device that is not listed in the system prompt.
                Returns the best matches with their name, type, context and current state.""";
    }

    @Override
    public List<LLMToolParam> getParamDescriptions(@Nullable Locale locale) {
        return List.of(
                new LLMToolParam("query", LLMToolParamType.STRING,
                        "Words describing the Item, e.g. 'garage door' or 'office temperature'", List.of(), true),
                new LLMToolParam("limit", LLMToolParamType.NUMBER,
                        "Maximum number of results, default " + DEFAULT_LIMIT + ", maximum " + MAX_LIMIT, List.of(),
                        false));
    }

    @Override
    public String call(Map<String, Object> params, @Nullable Locale locale) throws LLMToolException {
        String query = params.get("query") instanceof String s ? s.trim() : "";
        List<String> terms = terms(query);
        if (terms.isEmpty()) {
            throw new LLMToolException("Missing or invalid required parameter 'query'");
        }
        int limit = DEFAULT_LIMIT;
        if (params.get("limit") instanceof Number n) {
            limit = Math.max(1, Math.min(MAX_LIMIT, n.intValue()));
        }

        List<Match> matches = new ArrayList<>();
        for (Item item : itemRegistry.getItems()) {
            if (itemPermissionResolver.getPermission(item) == ItemPermission.NO_ACCESS) {
                continue;
            }
            Match match = match(item, terms);
            if (match != null) {
                matches.add(match);
            }
        }
        if (matches.isEmpty()) {
            return "No accessible Items found matching '" + query + "'.";
        }

        matches.sort(Comparator.comparingInt(Match::matchedTerms).reversed()
                .thenComparing(Comparator.comparingInt(Match::score).reversed())
                .thenComparing(m -> m.item().getName()));

        StringBuilder result = new StringBuilder();
        for (Match match : matches.subList(0, Math.min(limit, matches.size()))) {
            Item item = match.item();
            result.append("- ").append(item.getName()).append(" (").append(item.getType());
            if (itemPermissionResolver.getPermission(item) == ItemPermission.READ_ONLY) {
                result.append(", read-only");
            }
            result.append(")");
            String label = item.getLabel();
            if (label != null && !label.isBlank()) {
                result.append(" \"").append(label).append("\"");
            }
            List<String> context = match.context();
            if (!context.isEmpty()) {
                result.append(" in ")
                        .append(String.join(", ", context.subList(0, Math.min(MAX_CONTEXT_LABELS, context.size()))));
            }
            result.append(": ").append(LLMToolUtil.formatItemState(item, locale, timeZoneProvider.getTimeZone()))
                    .append("\n");
        }
        if (matches.size() > limit) {
            result.append("(").append(matches.size() - limit).append(" more, refine the query to narrow down)\n");
        }
        return result.toString().trim();
    }

    private @Nullable Match match(Item item, List<String> terms) {
        String own = (item.getName() + " " + Objects.requireNonNullElse(item.getLabel(), "")).toLowerCase(Locale.ROOT);
        List<String> context = parentLabels(item);
        String other = (String.join(" ", item.getTags()) + " " + String.join(" ", context)).toLowerCase(Locale.ROOT);

        int matchedTerms = 0;
        int score = 0;
        for (String term : terms) {
            if (own.contains(term)) {
                matchedTerms++;
                score += 2;
            } else if (other.contains(term)) {
                matchedTerms++;
                score++;
            }
        }
        return matchedTerms == 0 ? null : new Match(item, context, matchedTerms, score);
    }

    /**
     * Collects the labels of the item's semantic parents and groups, closest first.
     */
    private List<String> parentLabels(Item item) {
        Set<String> visited = new LinkedHashSet<>();
        visited.add(item.getName());
        List<String> labels = new ArrayList<>();
        List<Item> currentLayer = List.of(item);
        for (int depth = 0; depth < MAX_PARENT_DEPTH && !currentLayer.isEmpty(); depth++) {
            List<Item> nextLayer = new ArrayList<>();
            for (Item child : currentLayer) {
                for (String parentName : parentNames(child)) {
                    if (!visited.add(parentName)) {
                        continue;
                    }
                    Item parent = itemRegistry.get(parentName);
                    if (parent == null || itemPermissionResolver.getPermission(parent) == ItemPermission.NO_ACCESS) {
                        continue;
                    }
                    String label = parent.getLabel();
                    labels.add(label != null && !label.isBlank() ? label : parent.getName());
                    nextLayer.add(parent);
                }
            }
            currentLayer = nextLayer;
        }
        return labels;
    }

    private Set<String> parentNames(Item item) {
        Set<String> names = new LinkedHashSet<>();
        Metadata semantics = metadataRegistry.get(new MetadataKey(METADATA_NAMESPACE, item.getName()));
        if (semantics != null) {
            for (String relation : List.of(REL_IS_POINT_OF, REL_IS_PART_OF, REL_HAS_LOCATION)) {
                if (semantics.getConfiguration().get(relation) instanceof String parent && !parent.isBlank()) {
                    names.add(parent);
                }
            }
        }
        names.addAll(item.getGroupNames());
        return names;
    }

    private static List<String> terms(String query) {
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")).filter(t -> !t.isEmpty())
                .distinct().toList();
    }
}
