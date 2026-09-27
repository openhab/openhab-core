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
package org.openhab.core.config.discovery.internal;

import static org.openhab.core.config.discovery.inbox.InboxPredicates.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.config.discovery.inbox.InboxAutoApprovePredicate;
import org.openhab.core.config.discovery.inbox.InboxListener;
import org.openhab.core.events.AbstractTypedEventSubscriber;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.events.ThingStatusInfoChangedEvent;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.framework.Constants;
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
 * This class implements a service to automatically ignore or approve {@link Inbox} entries of newly discovered things.
 * <p>
 * <strong>Automatically ignoring inbox entries</strong>
 * </p>
 * <p>
 * The {@link AutomaticInboxProcessor} service implements an {@link EventSubscriber} that is triggered
 * for each thing when coming ONLINE. {@link Inbox} entries with the same representation value like the
 * newly created thing will be automatically set to {@link DiscoveryResultFlag#IGNORED}.
 * </p>
 * <p>
 * If a thing is being removed, possibly existing {@link Inbox} entries with the same representation value
 * are removed from the {@link Inbox} so they could be discovered again afterwards.
 * </p>
 * <p>
 * Automatically ignoring inbox entries can be enabled or disabled by setting the {@code autoIgnore} property to either
 * {@code true} or {@code false} via ConfigAdmin.
 * </p>
 * <p>
 * <strong>Automatically approving inbox entries</strong>
 * </p>
 * <p>
 * For each new discovery result, the {@link AutomaticInboxProcessor} queries all DS components implementing
 * {@link InboxAutoApprovePredicate} whether the result should be automatically approved.
 * </p>
 * <p>
 * If all new discovery results should be automatically approved (regardless of {@link InboxAutoApprovePredicate}s), the
 * {@code autoApprove} configuration property can be set to {@code true}.
 * </p>
 *
 * @author Andre Fuechsel - Initial contribution
 * @author Kai Kreuzer - added auto-approve functionality
 * @author Henning Sudbrock - added hook for selectively auto-approving inbox entries
 */
@Component(immediate = true, configurationPid = "org.openhab.inbox", service = EventSubscriber.class, //
        property = Constants.SERVICE_PID + "=org.openhab.inbox")
@ConfigurableService(category = "system", label = "Inbox", description_uri = AutomaticInboxProcessor.CONFIG_URI)
@NonNullByDefault
public class AutomaticInboxProcessor extends AbstractTypedEventSubscriber<ThingStatusInfoChangedEvent>
        implements InboxListener, RegistryChangeListener<Thing> {

    public static final String AUTO_IGNORE_CONFIG_PROPERTY = "autoIgnore";
    public static final String ALWAYS_AUTO_APPROVE_CONFIG_PROPERTY = "autoApprove";
    public static final String AUTO_APPROVE_RULES_ENABLED_CONFIG_PROPERTY = "autoApproveRulesEnabled";
    public static final String AUTO_APPROVE_RULES_CONFIG_PROPERTY = "autoApproveRules";

    protected static final String CONFIG_URI = "system:inbox";
    private static final Set<String> AUTO_APPROVE_RULE_KEYS = Set.of("thingUid", "bridgeUid", "label",
            "representationProperty", "propertyMatch", "configMatch", "setLabel", "setLocation", "setConfig",
            "setProperties");

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final Logger logger = LoggerFactory.getLogger(AutomaticInboxProcessor.class);

    private final ThingRegistry thingRegistry;
    private final ThingTypeRegistry thingTypeRegistry;
    private final Inbox inbox;
    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private boolean autoIgnore = true;
    private boolean alwaysAutoApprove = false;
    private boolean autoApproveRulesEnabled = true;
    private List<Map<String, String>> autoApproveRules = List.of();

    private final Set<InboxAutoApprovePredicate> inboxAutoApprovePredicates = new CopyOnWriteArraySet<>();

    @Activate
    public AutomaticInboxProcessor(final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ThingRegistry thingRegistry, final @Reference Inbox inbox,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry) {
        super(ThingStatusInfoChangedEvent.TYPE);
        this.thingTypeRegistry = thingTypeRegistry;
        this.thingRegistry = thingRegistry;
        this.inbox = inbox;
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    @Activate
    protected void activate(@Nullable Map<String, Object> properties) {
        thingRegistry.addRegistryChangeListener(this);
        inbox.addInboxListener(this);

        modified(properties);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> properties) {
        autoApproveRulesEnabled = true;
        if (properties != null) {
            Map<String, @Nullable Object> normalizedProperties = new HashMap<>();
            normalizedProperties.putAll(properties);
            ConfigDescription configDescription = configDescriptionRegistry
                    .getConfigDescription(URI.create(CONFIG_URI));
            if (configDescription != null) {
                normalizedProperties = ConfigUtil.normalizeTypes(normalizedProperties, List.of(configDescription));
            }

            @Nullable
            Object value = normalizedProperties.get(AUTO_IGNORE_CONFIG_PROPERTY);
            autoIgnore = value == null || !"false".equals(value.toString());

            value = normalizedProperties.get(ALWAYS_AUTO_APPROVE_CONFIG_PROPERTY);
            alwaysAutoApprove = value != null && "true".equals(value.toString());

            value = normalizedProperties.get(AUTO_APPROVE_RULES_ENABLED_CONFIG_PROPERTY);
            autoApproveRulesEnabled = value == null || !"false".equals(value.toString());

            Object rawRules = normalizedProperties.get(AUTO_APPROVE_RULES_CONFIG_PROPERTY);
            List<String> configuredRules = null;
            if (rawRules instanceof List<?> list) {
                configuredRules = list.stream().map(Objects::toString).toList();
            } else if (rawRules instanceof String str && !str.isBlank()) {
                configuredRules = List.of(str);
            }
            autoApproveRules = parseRules(configuredRules);
            autoApproveInboxEntries();
        }
    }

    private List<Map<String, String>> parseRules(@Nullable List<String> configuredRules) {
        List<Map<String, String>> rules = new ArrayList<>();
        if (configuredRules != null) {
            for (String configuredRule : configuredRules) {
                addRule(rules, configuredRule);
            }
        }
        return List.copyOf(rules);
    }

    private void addRule(List<Map<String, String>> rules, String ruleString) {
        ruleString = ruleString.stripLeading();
        if (ruleString.startsWith("#") || ruleString.startsWith("//")) {
            return;
        }
        Map<String, String> rule = new HashMap<>();
        boolean validRule = true;
        for (String pair : ruleString.split(";")) {
            int separator = pair.indexOf('=');
            if (separator > 0) {
                String key = pair.substring(0, separator).trim();
                String value = pair.substring(separator + 1).trim();
                if (AUTO_APPROVE_RULE_KEYS.contains(key) && !value.isEmpty()) {
                    rule.put(key, value);
                } else if (!pair.isBlank()) {
                    validRule = false;
                }
            } else if (!pair.isBlank()) {
                validRule = false;
            }
        }
        if (validRule && !rule.isEmpty()) {
            rules.add(Map.copyOf(rule));
        }
    }

    private void autoApprove(DiscoveryResult result) {
        if (result.getFlag() == DiscoveryResultFlag.IGNORED) {
            return;
        }
        if (alwaysAutoApprove) {
            Thing thing = inbox.approve(result.getThingUID(), result.getLabel(), null);
            if (thing != null) {
                logger.info("Auto-approved discovery result '{}' ({}) because 'autoApprove' is enabled.",
                        result.getLabel(), result.getThingUID());
            }
            return;
        }
        if (autoApproveRulesEnabled) {
            for (Map<String, String> rule : autoApproveRules) {
                if (ruleMatches(rule, result)) {
                    applyRuleAndApprove(rule, result);
                    return;
                }
            }
        }
        if (isToBeAutoApproved(result)) {
            inbox.approve(result.getThingUID(), result.getLabel(), null);
        }
    }

    private boolean ruleMatches(Map<String, String> rule, DiscoveryResult result) {
        if (!matchesIfConfigured(rule, "thingUid", result.getThingUID().getAsString())
                || !matchesIfConfigured(rule, "bridgeUid",
                        result.getBridgeUID() == null ? null : result.getBridgeUID().getAsString())
                || !matchesIfConfigured(rule, "label", result.getLabel())) {
            return false;
        }
        String representationProperty = result.getRepresentationProperty();
        String representationValue = representationProperty == null ? null
                : Objects.toString(result.getProperties().get(representationProperty), null);
        if (!matchesIfConfigured(rule, "representationProperty", representationValue)) {
            return false;
        }
        return matchesProperties(rule.get("propertyMatch"), result.getProperties())
                && matchesProperties(rule.get("configMatch"), result.getProperties());
    }

    private boolean matchesIfConfigured(Map<String, String> rule, String key, @Nullable String value) {
        String pattern = rule.get(key);
        return pattern == null || value != null && globMatches(pattern, value);
    }

    private boolean matchesProperties(@Nullable String conditions, Map<String, Object> properties) {
        if (conditions == null) {
            return true;
        }
        for (String entry : conditions.split(",")) {
            String pair = entry.trim();
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                separator = pair.indexOf(':');
            }
            if (separator <= 0 || !globMatches(pair.substring(separator + 1).trim(),
                    String.valueOf(properties.get(pair.substring(0, separator).trim())))) {
                return false;
            }
        }
        return true;
    }

    private boolean globMatches(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char character = pattern.charAt(i);
            if (character == '*') {
                regex.append(".*");
            } else if (character == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(character)));
            }
        }
        return Pattern.compile(regex.append('$').toString()).matcher(value).matches();
    }

    private String interpolate(String value, DiscoveryResult result) {
        Matcher matcher = INTERPOLATION_PATTERN.matcher(value);
        StringBuilder interpolated = new StringBuilder();
        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement = switch (variable) {
                case "label" -> Objects.toString(result.getLabel(), "");
                case "thingUID" -> result.getThingUID().getAsString();
                case "bridgeUID" -> Objects.toString(result.getBridgeUID(), "");
                default -> variable.startsWith("properties.")
                        ? Objects.toString(result.getProperties().get(variable.substring("properties.".length())), "")
                        : "";
            };
            matcher.appendReplacement(interpolated, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(interpolated);
        return interpolated.toString();
    }

    private void applyRuleAndApprove(Map<String, String> rule, DiscoveryResult result) {
        String ruleString = rule.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).sorted()
                .collect(Collectors.joining("; "));
        String configuredLabel = rule.get("setLabel");
        String label = configuredLabel == null ? result.getLabel() : interpolate(configuredLabel, result);
        Thing thing = inbox.approve(result.getThingUID(), label, null);
        if (thing == null) {
            return;
        }
        logger.info("Auto-approved discovery result '{}' ({}) because it matched rule [{}]", result.getLabel(),
                result.getThingUID(), ruleString);
        if (rule.get("setLocation") == null && rule.get("setProperties") == null && rule.get("setConfig") == null) {
            return;
        }

        ThingBuilder builder = ThingBuilder.create(thing);
        String location = rule.get("setLocation");
        if (location != null) {
            builder.withLocation(interpolate(location, result));
        }
        Map<String, String> thingProperties = new HashMap<>(thing.getProperties());
        applyConfiguredValues(rule.get("setProperties"), thingProperties, result);
        builder.withProperties(thingProperties);
        Map<String, Object> configuration = new HashMap<>(thing.getConfiguration().getProperties());
        applyConfiguredValues(rule.get("setConfig"), configuration, result);
        builder.withConfiguration(new Configuration(configuration));
        thingRegistry.update(builder.build());
    }

    private <T> void applyConfiguredValues(@Nullable String configuredValues, Map<String, T> target,
            DiscoveryResult result) {
        if (configuredValues != null) {
            for (String entry : configuredValues.split(",")) {
                String assignment = entry.trim();
                int separator = assignment.indexOf('=');
                if (separator <= 0) {
                    separator = assignment.indexOf(':');
                }
                if (separator <= 0) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                T interpolated = (T) interpolate(assignment.substring(separator + 1).trim(), result);
                target.put(assignment.substring(0, separator).trim(), interpolated);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        inbox.removeInboxListener(this);
        thingRegistry.removeRegistryChangeListener(this);
    }

    @Override
    public void receiveTypedEvent(ThingStatusInfoChangedEvent event) {
        if (autoIgnore) {
            Thing thing = thingRegistry.get(event.getThingUID());
            ThingStatus thingStatus = event.getStatusInfo().getStatus();
            autoIgnore(thing, thingStatus);
        }
    }

    @Override
    public void thingAdded(Inbox inbox, DiscoveryResult result) {
        if (autoIgnore) {
            String value = getRepresentationValue(result);
            if (value != null) {
                Optional<Thing> thing = thingRegistry.stream()
                        .filter(t -> Objects.equals(value, getRepresentationPropertyValueForThing(t)))
                        .filter(t -> Objects.equals(t.getThingTypeUID(), result.getThingTypeUID())).findFirst();
                if (thing.isPresent()) {
                    logger.debug("Auto-ignoring the inbox entry for the representation value '{}'.", value);
                    inbox.setFlag(result.getThingUID(), DiscoveryResultFlag.IGNORED);
                }
            }
        }
        autoApprove(result);
    }

    @Override
    public void thingUpdated(Inbox inbox, DiscoveryResult result) {
    }

    @Override
    public void thingRemoved(Inbox inbox, DiscoveryResult result) {
    }

    @Override
    public void added(Thing element) {
    }

    @Override
    public void removed(Thing element) {
        removePossiblyIgnoredResultInInbox(element);
    }

    @Override
    public void updated(Thing oldElement, Thing element) {
    }

    private @Nullable String getRepresentationValue(DiscoveryResult result) {
        return result.getRepresentationProperty() != null
                ? Objects.toString(result.getProperties().get(result.getRepresentationProperty()), null)
                : null;
    }

    private void autoIgnore(@Nullable Thing thing, ThingStatus thingStatus) {
        if (ThingStatus.ONLINE.equals(thingStatus)) {
            checkAndIgnoreInInbox(thing);
        }
    }

    private void checkAndIgnoreInInbox(@Nullable Thing thing) {
        if (thing != null) {
            String representationValue = getRepresentationPropertyValueForThing(thing);
            if (representationValue != null) {
                ignoreInInbox(thing.getThingTypeUID(), representationValue);
            }
        }
    }

    private void ignoreInInbox(ThingTypeUID thingtypeUID, String representationValue) {
        List<DiscoveryResult> results = inbox.stream().filter(withRepresentationPropertyValue(representationValue))
                .filter(forThingTypeUID(thingtypeUID)).toList();
        if (results.size() == 1) {
            logger.debug("Auto-ignoring the inbox entry for the representation value '{}'.", representationValue);

            inbox.setFlag(results.getFirst().getThingUID(), DiscoveryResultFlag.IGNORED);
        }
    }

    private void removePossiblyIgnoredResultInInbox(@Nullable Thing thing) {
        if (thing != null) {
            String representationValue = getRepresentationPropertyValueForThing(thing);
            if (representationValue != null) {
                removeFromInbox(thing.getThingTypeUID(), representationValue);
            }
        }
    }

    private @Nullable String getRepresentationPropertyValueForThing(Thing thing) {
        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
        if (thingType != null) {
            String representationProperty = thingType.getRepresentationProperty();
            if (representationProperty == null) {
                return null;
            }
            Map<String, String> properties = thing.getProperties();
            if (properties.containsKey(representationProperty)) {
                return properties.get(representationProperty);
            }
            Configuration configuration = thing.getConfiguration();
            if (configuration.containsKey(representationProperty)) {
                return String.valueOf(configuration.get(representationProperty));
            }
        }
        return null;
    }

    private void removeFromInbox(ThingTypeUID thingtypeUID, String representationValue) {
        List<DiscoveryResult> results = inbox.stream().filter(withRepresentationPropertyValue(representationValue))
                .filter(forThingTypeUID(thingtypeUID)).filter(withFlag(DiscoveryResultFlag.IGNORED)).toList();
        if (results.size() == 1) {
            logger.debug("Removing the ignored result from the inbox for the representation value '{}'.",
                    representationValue);
            inbox.remove(results.getFirst().getThingUID());
        }
    }

    private void autoApproveInboxEntries() {
        for (DiscoveryResult result : inbox.getAll()) {
            if (DiscoveryResultFlag.NEW.equals(result.getFlag())) {
                autoApprove(result);
            }
        }
    }

    private boolean isToBeAutoApproved(DiscoveryResult result) {
        return inboxAutoApprovePredicates.stream().anyMatch(predicate -> predicate.test(result));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addInboxAutoApprovePredicate(InboxAutoApprovePredicate inboxAutoApprovePredicate) {
        inboxAutoApprovePredicates.add(inboxAutoApprovePredicate);
        for (DiscoveryResult result : inbox.getAll()) {
            if (DiscoveryResultFlag.NEW.equals(result.getFlag()) && inboxAutoApprovePredicate.test(result)) {
                inbox.approve(result.getThingUID(), result.getLabel(), null);
            }
        }
    }

    protected void removeInboxAutoApprovePredicate(InboxAutoApprovePredicate inboxAutoApprovePredicate) {
        inboxAutoApprovePredicates.remove(inboxAutoApprovePredicate);
    }
}
