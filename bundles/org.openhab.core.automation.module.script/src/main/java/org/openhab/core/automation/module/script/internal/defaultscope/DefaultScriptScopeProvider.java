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
package org.openhab.core.automation.module.script.internal.defaultscope;

import java.io.File;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringListType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.library.unit.BinaryPrefix;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.SmartHomeUnits;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
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
 * @author Simon Merschjohann - Refactored to be a {@link ScriptExtensionProvider}
 */
@Component(immediate = true)
@NonNullByDefault
public class DefaultScriptScopeProvider implements ScriptExtensionProvider {

    private static final String PRESET_DEFAULT = "default";

    private final Map<String, Object> elements = new ConcurrentHashMap<>();

    private final ScriptBusEvent busEvent;
    private final ScriptThingActions thingActions;

    @Activate
    public DefaultScriptScopeProvider(final @Reference ItemRegistry itemRegistry,
            final @Reference ThingRegistry thingRegistry, final @Reference RuleRegistry ruleRegistry,
            final @Reference EventPublisher eventPublisher) {
        this.busEvent = new ScriptBusEvent(itemRegistry, eventPublisher);
        this.thingActions = new ScriptThingActions(thingRegistry);

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

        elements.put("RefreshType", RefreshType.class);
        elements.put("REFRESH", RefreshType.REFRESH);

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
        elements.put("BinaryPrefix", BinaryPrefix.class);

        // services
        elements.put("items", new ItemRegistryDelegate(itemRegistry));
        elements.put("ir", itemRegistry);
        elements.put("itemRegistry", itemRegistry);
        elements.put("things", thingRegistry);
        elements.put("rules", ruleRegistry);
        elements.put("events", busEvent);
        elements.put("actions", thingActions);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    synchronized void addThingActions(ThingActions thingActions) {
        this.thingActions.addThingActions(thingActions);
        elements.put(thingActions.getClass().getSimpleName(), thingActions.getClass());
    }

    protected void removeThingActions(ThingActions thingActions) {
        elements.remove(thingActions.getClass().getSimpleName());
        this.thingActions.removeThingActions(thingActions);
    }

    @Deactivate
    protected void deactivate() {
        busEvent.dispose();
        thingActions.dispose();
        elements.clear();
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Collections.singleton(PRESET_DEFAULT);
    }

    @Override
    public Collection<String> getPresets() {
        return Collections.singleton(PRESET_DEFAULT);
    }

    @Override
    public Collection<String> getTypes() {
        return elements.keySet();
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) {
        return elements.get(type);
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_DEFAULT.equals(preset)) {
            return Collections.unmodifiableMap(elements);
        }
        return Collections.emptyMap();
    }

    @Override
    public void unload(String scriptIdentifier) {
        // nothing todo
    }
}
