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
package org.eclipse.smarthome.automation.module.script.defaultscope.internal;

import java.io.File;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.module.script.ScriptExtensionProvider;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.StringListType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.MetricPrefix;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is a default scope provider for stuff that is of general interest in an ESH-based solution.
 * Nonetheless, solutions are free to remove it and have more specific scope providers for their own purposes.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann - refactored to be an ScriptExtensionProvider
 *
 */
@Component(immediate = true)
public class DefaultScriptScopeProvider implements ScriptExtensionProvider {

    private final Queue<ThingActions> queuedBeforeActivation = new LinkedList<>();

    private Map<String, Object> elements;

    private ItemRegistry itemRegistry;

    private ThingRegistry thingRegistry;

    private EventPublisher eventPublisher;

    private ScriptBusEvent busEvent;

    private ScriptThingActions thingActions;

    private RuleRegistry ruleRegistry;

    @Reference
    protected void setRuleRegistry(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    protected void unsetRuleRegistry(RuleRegistry ruleRegistry) {
        this.ruleRegistry = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    synchronized void addThingActions(ThingActions thingActions) {
        if (this.thingActions == null) { // bundle may not be active yet
            queuedBeforeActivation.add(thingActions);
        } else {
            this.thingActions.addThingActions(thingActions);
            elements.put(thingActions.getClass().getSimpleName(), thingActions.getClass());
        }
    }

    protected void removeThingActions(ThingActions thingActions) {
        elements.remove(thingActions.getClass().getSimpleName());
        this.thingActions.removeThingActions(thingActions);
    }

    @Activate
    protected synchronized void activate() {
        busEvent = new ScriptBusEvent(itemRegistry, eventPublisher);
        thingActions = new ScriptThingActions(thingRegistry);

        elements = new HashMap<>();
        elements.put("State", State.class);
        elements.put("Command", Command.class);
        elements.put("StringUtils", StringUtils.class);
        elements.put("URLEncoder", URLEncoder.class);
        elements.put("FileUtils", FileUtils.class);
        elements.put("FilenameUtils", FilenameUtils.class);
        elements.put("File", File.class);

        // ESH types
        elements.put("IncreaseDecreaseType", IncreaseDecreaseType.class);
        elements.put("DECREASE", IncreaseDecreaseType.DECREASE);
        elements.put("INCREASE", IncreaseDecreaseType.INCREASE);

        elements.put("OnOffType", OnOffType.class);
        elements.put("ON", OnOffType.ON);
        elements.put("OFF", OnOffType.OFF);

        elements.put("OpenClosedType", OpenClosedType.class);
        elements.put("CLOSED", OpenClosedType.CLOSED);
        elements.put("OPEN", OpenClosedType.OPEN);

        elements.put("StopMoveType", StopMoveType.class);
        elements.put("MOVE", StopMoveType.MOVE);
        elements.put("STOP", StopMoveType.STOP);

        elements.put("UpDownType", UpDownType.class);
        elements.put("DOWN", UpDownType.DOWN);
        elements.put("UP", UpDownType.UP);

        elements.put("UnDefType", UnDefType.class);
        elements.put("NULL", UnDefType.NULL);
        elements.put("UNDEF", UnDefType.UNDEF);

        elements.put("NextPreviousType", NextPreviousType.class);
        elements.put("NEXT", NextPreviousType.NEXT);
        elements.put("PREVIOUS", NextPreviousType.PREVIOUS);

        elements.put("PlayPauseType", PlayPauseType.class);
        elements.put("PLAY", PlayPauseType.PLAY);
        elements.put("PAUSE", PlayPauseType.PAUSE);

        elements.put("RewindFastforwardType", RewindFastforwardType.class);
        elements.put("REWIND", RewindFastforwardType.REWIND);
        elements.put("FASTFORWARD", RewindFastforwardType.FASTFORWARD);

        elements.put("QuantityType", QuantityType.class);
        elements.put("StringListType", StringListType.class);
        elements.put("RawType", RawType.class);
        elements.put("DateTimeType", DateTimeType.class);
        elements.put("DecimalType", DecimalType.class);
        elements.put("HSBType", HSBType.class);
        elements.put("PercentType", PercentType.class);
        elements.put("PointType", PointType.class);
        elements.put("StringType", StringType.class);

        elements.put("SIUnits", SIUnits.class);
        elements.put("ImperialUnits", ImperialUnits.class);
        elements.put("MetricPrefix", MetricPrefix.class);
        elements.put("SmartHomeUnits", SmartHomeUnits.class);

        // services
        elements.put("items", new ItemRegistryDelegate(itemRegistry));
        elements.put("ir", itemRegistry);
        elements.put("itemRegistry", itemRegistry);
        elements.put("things", thingRegistry);
        elements.put("events", busEvent);
        elements.put("rules", ruleRegistry);
        elements.put("actions", thingActions);

        // if any thingActions were queued before this got activated, add them now
        queuedBeforeActivation.forEach(thingActions -> this.addThingActions(thingActions));
        queuedBeforeActivation.clear();
    }

    @Deactivate
    protected void deactivate() {
        busEvent.dispose();
        busEvent = null;
        thingActions.dispose();
        thingActions = null;
        elements = null;
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Collections.singleton("default");
    }

    @Override
    public Collection<String> getPresets() {
        return Collections.singleton("default");
    }

    @Override
    public Collection<String> getTypes() {
        return elements.keySet();
    }

    @Override
    public Object get(String scriptIdentifier, String type) {
        return elements.get(type);
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (preset.equals("default")) {
            return elements;
        }

        return null;
    }

    @Override
    public void unload(String scriptIdentifier) {
        // nothing todo
    }

}
