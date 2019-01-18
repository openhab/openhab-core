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
package org.eclipse.smarthome.config.discovery.internal;

import static org.eclipse.smarthome.config.discovery.inbox.InboxPredicates.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.config.discovery.inbox.InboxAutoApprovePredicate;
import org.eclipse.smarthome.config.discovery.inbox.InboxListener;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.events.AbstractTypedEventSubscriber;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.events.ThingStatusInfoChangedEvent;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Component;
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
 * @author Andre Fuechsel - Initial Contribution
 * @author Kai Kreuzer - added auto-approve functionality
 * @author Henning Sudbrock - added hook for selectively auto-approving inbox entries
 */
@Component(immediate = true, configurationPid = "org.eclipse.smarthome.inbox", service = EventSubscriber.class, property = {
        "service.config.description.uri=system:inbox", "service.config.label=Inbox", "service.config.category=system",
        "service.pid=org.eclipse.smarthome.inbox" })
@NonNullByDefault
public class AutomaticInboxProcessor extends AbstractTypedEventSubscriber<ThingStatusInfoChangedEvent>
        implements InboxListener, RegistryChangeListener<Thing> {

    public static final String AUTO_IGNORE_CONFIG_PROPERTY = "autoIgnore";
    public static final String ALWAYS_AUTO_APPROVE_CONFIG_PROPERTY = "autoApprove";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @NonNullByDefault({})
    private ThingRegistry thingRegistry;
    @NonNullByDefault({})
    private ThingTypeRegistry thingTypeRegistry;
    @NonNullByDefault({})
    private Inbox inbox;
    private boolean autoIgnore = true;
    private boolean alwaysAutoApprove = false;

    private final Set<InboxAutoApprovePredicate> inboxAutoApprovePredicates = new CopyOnWriteArraySet<>();

    public AutomaticInboxProcessor() {
        super(ThingStatusInfoChangedEvent.TYPE);
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
                Thing thing = thingRegistry.stream()
                        .filter(t -> Objects.equals(value, getRepresentationPropertyValueForThing(t)))
                        .filter(t -> Objects.equals(t.getThingTypeUID(), result.getThingTypeUID())).findFirst()
                        .orElse(null);
                if (thing != null) {
                    logger.debug("Auto-ignoring the inbox entry for the representation value {}", value);
                    inbox.setFlag(result.getThingUID(), DiscoveryResultFlag.IGNORED);
                }
            }
        }
        if (alwaysAutoApprove || isToBeAutoApproved(result)) {
            inbox.approve(result.getThingUID(), result.getLabel());
        }
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
                .filter(forThingTypeUID(thingtypeUID)).collect(Collectors.toList());
        if (results.size() == 1) {
            logger.debug("Auto-ignoring the inbox entry for the representation value {}", representationValue);
            inbox.setFlag(results.get(0).getThingUID(), DiscoveryResultFlag.IGNORED);
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
                .filter(forThingTypeUID(thingtypeUID)).filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList());
        if (results.size() == 1) {
            logger.debug("Removing the ignored result from the inbox for the representation value {}",
                    representationValue);
            inbox.remove(results.get(0).getThingUID());
        }
    }

    private void autoApproveInboxEntries() {
        for (DiscoveryResult result : inbox.getAll()) {
            if (result.getFlag().equals(DiscoveryResultFlag.NEW)) {
                if (alwaysAutoApprove || isToBeAutoApproved(result)) {
                    inbox.approve(result.getThingUID(), result.getLabel());
                }
            }
        }
    }

    private boolean isToBeAutoApproved(DiscoveryResult result) {
        return inboxAutoApprovePredicates.stream().anyMatch(predicate -> predicate.test(result));
    }

    protected void activate(@Nullable Map<String, @Nullable Object> properties) {
        if (properties != null) {
            Object value = properties.get(AUTO_IGNORE_CONFIG_PROPERTY);
            autoIgnore = value == null || !value.toString().equals("false");
            value = properties.get(ALWAYS_AUTO_APPROVE_CONFIG_PROPERTY);
            alwaysAutoApprove = value != null && value.toString().equals("true");
            autoApproveInboxEntries();
        }
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
        thingRegistry.addRegistryChangeListener(this);
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        thingRegistry.removeRegistryChangeListener(this);
        this.thingRegistry = null;
    }

    @Reference
    protected void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    protected void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null;
    }

    @Reference
    protected void setInbox(Inbox inbox) {
        this.inbox = inbox;
        inbox.addInboxListener(this);
    }

    protected void unsetInbox(Inbox inbox) {
        inbox.removeInboxListener(this);
        this.inbox = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addInboxAutoApprovePredicate(InboxAutoApprovePredicate inboxAutoApprovePredicate) {
        inboxAutoApprovePredicates.add(inboxAutoApprovePredicate);
        for (DiscoveryResult result : inbox.getAll()) {
            if (result.getFlag().equals(DiscoveryResultFlag.NEW) && inboxAutoApprovePredicate.test(result)) {
                inbox.approve(result.getThingUID(), result.getLabel());
            }
        }
    }

    protected void removeInboxAutoApprovePredicate(InboxAutoApprovePredicate inboxAutoApprovePredicate) {
        inboxAutoApprovePredicates.remove(inboxAutoApprovePredicate);
    }
}
