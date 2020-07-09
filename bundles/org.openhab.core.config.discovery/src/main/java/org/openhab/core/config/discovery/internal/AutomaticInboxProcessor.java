/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
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

    protected static final String CONFIG_URI = "system:inbox";

    private final Logger logger = LoggerFactory.getLogger(AutomaticInboxProcessor.class);

    private final ThingRegistry thingRegistry;
    private final ThingTypeRegistry thingTypeRegistry;
    private final Inbox inbox;
    private boolean autoIgnore = true;
    private boolean alwaysAutoApprove = false;

    private final Set<InboxAutoApprovePredicate> inboxAutoApprovePredicates = new CopyOnWriteArraySet<>();

    @Activate
    public AutomaticInboxProcessor(final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ThingRegistry thingRegistry, final @Reference Inbox inbox) {
        super(ThingStatusInfoChangedEvent.TYPE);
        this.thingTypeRegistry = thingTypeRegistry;
        this.thingRegistry = thingRegistry;
        this.inbox = inbox;
    }

    @Activate
    protected void activate(@Nullable Map<String, @Nullable Object> properties) {
        thingRegistry.addRegistryChangeListener(this);
        inbox.addInboxListener(this);

        modified(properties);
    }

    @Modified
    protected void modified(@Nullable Map<String, @Nullable Object> properties) {
        if (properties != null) {
            Object value = properties.get(AUTO_IGNORE_CONFIG_PROPERTY);
            autoIgnore = value == null || !"false".equals(value.toString());
            value = properties.get(ALWAYS_AUTO_APPROVE_CONFIG_PROPERTY);
            alwaysAutoApprove = value != null && "true".equals(value.toString());
            autoApproveInboxEntries();
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
            logger.debug("Auto-ignoring the inbox entry for the representation value '{}'.", representationValue);

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
            logger.debug("Removing the ignored result from the inbox for the representation value '{}'.",
                    representationValue);
            inbox.remove(results.get(0).getThingUID());
        }
    }

    private void autoApproveInboxEntries() {
        for (DiscoveryResult result : inbox.getAll()) {
            if (DiscoveryResultFlag.NEW.equals(result.getFlag())) {
                if (alwaysAutoApprove || isToBeAutoApproved(result)) {
                    inbox.approve(result.getThingUID(), result.getLabel());
                }
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
                inbox.approve(result.getThingUID(), result.getLabel());
            }
        }
    }

    protected void removeInboxAutoApprovePredicate(InboxAutoApprovePredicate inboxAutoApprovePredicate) {
        inboxAutoApprovePredicates.remove(inboxAutoApprovePredicate);
    }
}
