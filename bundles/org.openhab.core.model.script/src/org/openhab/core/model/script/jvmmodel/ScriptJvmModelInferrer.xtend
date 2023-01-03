/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.model.script.jvmmodel

import com.google.inject.Inject
import java.util.Set
import org.openhab.core.items.ItemRegistry
import org.openhab.core.model.script.scoping.StateAndCommandProvider
import org.openhab.core.model.script.script.Script
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.openhab.core.items.Item
import org.openhab.core.types.Command
import org.openhab.core.types.State
import org.openhab.core.events.Event
import org.openhab.core.automation.module.script.rulesupport.shared.ValueCache

/**
 * <p>Infers a JVM model from the source model.</p> 
 * 
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>
 * 
 * @author Oliver Libutzki - Xtext 2.5.0 migration
 *      
 */
class ScriptJvmModelInferrer extends AbstractModelInferrer {

    static private final Logger logger = LoggerFactory.getLogger(ScriptJvmModelInferrer)

    /** Variable name for the input string in a "script transformation" or "script profile" */
    public static final String VAR_INPUT = "input";

    /** Variable name for the item in a "state triggered" or "command triggered" rule */
    public static final String VAR_TRIGGERING_ITEM = "triggeringItem";

    /** Variable name for the item in a "state triggered" or "command triggered" rule */
    public static final String VAR_TRIGGERING_ITEM_NAME = "triggeringItemName";

    /** Variable name for the previous state of an item in a "changed state triggered" rule */
    public static final String VAR_PREVIOUS_STATE = "previousState";

    /** Variable name for the new state of an item in a "changed state triggered" or "updated state triggered" rule */
    public static final String VAR_NEW_STATE = "newState";

    /** Variable name for the received command in a "command triggered" rule */
    public static final String VAR_RECEIVED_COMMAND = "receivedCommand";

    /** Variable name for the received event in a "trigger event" rule */
    public static final String VAR_RECEIVED_EVENT = "receivedEvent";

    /** Variable name for the triggering channel in a "trigger event" rule */
    public static final String VAR_TRIGGERING_CHANNEL = "triggeringChannel";

    /** Variable name for the triggering thing in a "thing status trigger" rule */
    public static final String VAR_TRIGGERING_THING = "triggeringThing";
    
    /** Variable name for the previous status of the triggering thing in a "thing status trigger" rule */
    public static final String VAR_PREVIOUS_STATUS = "previousThingStatus";
    
    /** Variable name for the new status of the triggering thing in a "thing status trigger" rule */
    public static final String VAR_NEW_STATUS = "newThingStatus";

    /** Variable name for the cache */
    public static final String VAR_PRIVATE_CACHE = "privateCache";
    public static final String VAR_SHARED_CACHE = "sharedCache";

    /**
     * conveninence API to build and initialize JvmTypes and their members.
     */
    @Inject extension JvmTypesBuilder

    @Inject
    ItemRegistry itemRegistry

    @Inject
    StateAndCommandProvider stateAndCommandProvider

    /**
     * Is called for each instance of the first argument's type contained in a resource.
     * 
     * @param element - the model to create one or more JvmDeclaredTypes from.
     * @param acceptor - each created JvmDeclaredType without a container should be passed to the acceptor in order get attached to the
     *                   current resource.
     * @param isPreLinkingPhase - whether the method is called in a pre linking phase, i.e. when the global index isn't fully updated. You
     *        must not rely on linking using the index if iPrelinkingPhase is <code>true</code>
     */
    def dispatch void infer(Script script, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
        val className = script.eResource.URI.lastSegment.split("\\.").head.toFirstUpper + "Script"
        acceptor.accept(script.toClass(className)).initializeLater [

            val Set<String> fieldNames = newHashSet()

            val types = stateAndCommandProvider.allTypes
            types.forEach [ type |
                val name = type.toString
                if (fieldNames.add(name)) {
                    members += script.toField(name, script.newTypeRef(type.class)) [
                        static = true
                    ]
                } else {
                    logger.warn("Duplicate field: '{}'. Ignoring '{}'.", name, type.class.name)
                }
            ]

            itemRegistry?.items?.forEach [ item |
                val name = item.name
                if (fieldNames.add(name)) {
                    members += script.toField(item.name, script.newTypeRef(item.class)) [
                        static = true
                    ]
                } else {
                    logger.warn("Duplicate field: '{}'. Ignoring '{}'.", item.name, item.class.name)
                }
            ]

            members += script.toMethod("_script", null) [
                static = true
                val inputTypeRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_INPUT, inputTypeRef)
                val itemTypeRef = script.newTypeRef(Item)
                parameters += script.toParameter(VAR_TRIGGERING_ITEM, itemTypeRef)
                val itemNameRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_TRIGGERING_ITEM_NAME, itemNameRef)
                val commandTypeRef = script.newTypeRef(Command)
                parameters += script.toParameter(VAR_RECEIVED_COMMAND, commandTypeRef)
                val stateTypeRef = script.newTypeRef(State)
                parameters += script.toParameter(VAR_PREVIOUS_STATE, stateTypeRef)
                val eventTypeRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_RECEIVED_EVENT, eventTypeRef)
                val channelRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_TRIGGERING_CHANNEL, channelRef)
                val thingRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_TRIGGERING_THING, thingRef)
                val oldThingStatusRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_PREVIOUS_STATUS, oldThingStatusRef)
                val newThingStatusRef = script.newTypeRef(String)
                parameters += script.toParameter(VAR_NEW_STATUS, newThingStatusRef)
                val stateTypeRef2 = script.newTypeRef(State)
                parameters += script.toParameter(VAR_NEW_STATE, stateTypeRef2)
                val privateCacheTypeRef = script.newTypeRef(ValueCache)
                parameters += script.toParameter(VAR_PRIVATE_CACHE, privateCacheTypeRef)
                val sharedCacheTypeRef = script.newTypeRef(ValueCache)
                parameters += script.toParameter(VAR_SHARED_CACHE, sharedCacheTypeRef)
                body = script
            ]
        ]
    }
}
