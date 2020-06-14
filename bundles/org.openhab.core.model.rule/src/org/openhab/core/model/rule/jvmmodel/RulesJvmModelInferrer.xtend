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
package org.openhab.core.model.rule.jvmmodel

import com.google.inject.Inject
import java.util.Set
import org.openhab.core.items.Item
import org.openhab.core.items.ItemRegistry
import org.openhab.core.thing.ThingRegistry
import org.openhab.core.thing.events.ChannelTriggeredEvent
import org.openhab.core.types.Command
import org.openhab.core.types.State
import org.openhab.core.model.rule.rules.ChangedEventTrigger
import org.openhab.core.model.rule.rules.CommandEventTrigger
import org.openhab.core.model.rule.rules.EventEmittedTrigger
import org.openhab.core.model.rule.rules.EventTrigger
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger
import org.openhab.core.model.rule.rules.Rule
import org.openhab.core.model.rule.rules.RuleModel
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger
import org.openhab.core.model.rule.rules.UpdateEventTrigger
import org.openhab.core.model.script.jvmmodel.ScriptJvmModelInferrer
import org.openhab.core.model.script.scoping.StateAndCommandProvider
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.eclipse.xtext.common.types.JvmFormalParameter
import org.eclipse.emf.common.util.EList

/**
 * <p>Infers a JVM model from the source model.</p> 
 * 
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p> 
 * 
 * @author Oliver Libutzki - Xtext 2.5.0 migration
 *     
 */
class RulesJvmModelInferrer extends ScriptJvmModelInferrer {

    private final Logger logger = LoggerFactory.getLogger(RulesJvmModelInferrer)

    /**
     * conveninence API to build and initialize JvmTypes and their members.
     */
    @Inject extension JvmTypesBuilder
    @Inject extension IQualifiedNameProvider

    @Inject
    ItemRegistry itemRegistry

    @Inject
    ThingRegistry thingRegistry

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
    def dispatch void infer(RuleModel ruleModel, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
        val className = ruleModel.eResource.URI.lastSegment.split("\\.").head.toFirstUpper + "Rules"
        acceptor.accept(ruleModel.toClass(className)).initializeLater [
            members += ruleModel.variables.map [
                toField(name, type?.cloneWithProxies) => [ field |
                    field.static = true
                    field.final = !writeable
                    field.initializer = right
                ]
            ]

            val Set<String> fieldNames = newHashSet()

            val types = stateAndCommandProvider.allTypes
            types.forEach [ type |
                val name = type.toString
                if (fieldNames.add(name)) {
                    members += ruleModel.toField(name, ruleModel.newTypeRef(type.class)) [
                        static = true
                    ]
                } else {
                    logger.warn("Duplicate field: '{}'. Ignoring '{}'.", name, type.class.name)
                }
            ]

            itemRegistry?.items?.forEach [ item |
                val name = item.name
                if (fieldNames.add(name)) {
                    members += ruleModel.toField(item.name, ruleModel.newTypeRef(item.class)) [
                        static = true
                    ]
                } else {
                    logger.warn("Duplicate field: '{}'. Ignoring '{}'.", item.name, item.class.name)
                }
            ]

            val things = thingRegistry?.getAll()
            things?.forEach [ thing |
                val name = thing.getUID().toString()
                if (fieldNames.add(name)) {
                    members += ruleModel.toField(name, ruleModel.newTypeRef(thing.class)) [
                        static = true
                    ]
                } else {
                    logger.warn("Duplicate field: '{}'. Ignoring '{}'.", name, thing.class.name)
                }
            ]

            members += ruleModel.rules.map [ rule |
                rule.toMethod("_" + rule.name, ruleModel.newTypeRef(Void.TYPE)) [
                    static = true
                    if ((containsCommandTrigger(rule)) || (containsStateChangeTrigger(rule)) || (containsStateUpdateTrigger(rule))) {
                        val itemTypeRef = ruleModel.newTypeRef(Item)
                        parameters += rule.toParameter(VAR_TRIGGERING_ITEM, itemTypeRef)
                    }
                    if (containsCommandTrigger(rule)) {
                        val commandTypeRef = ruleModel.newTypeRef(Command)
                        parameters += rule.toParameter(VAR_RECEIVED_COMMAND, commandTypeRef)
                    }
                    if (containsStateChangeTrigger(rule) && !containsParam(parameters, VAR_PREVIOUS_STATE)) {
                        val stateTypeRef = ruleModel.newTypeRef(State)
                        parameters += rule.toParameter(VAR_PREVIOUS_STATE, stateTypeRef)
                    }
                    if (containsEventTrigger(rule)) {
                        val eventTypeRef = ruleModel.newTypeRef(ChannelTriggeredEvent)
                        parameters += rule.toParameter(VAR_RECEIVED_EVENT, eventTypeRef)
                    }
                    if (containsThingStateChangedEventTrigger(rule) && !containsParam(parameters, VAR_PREVIOUS_STATE)) {
                        val stateTypeRef = ruleModel.newTypeRef(State)
                        parameters += rule.toParameter(VAR_PREVIOUS_STATE, stateTypeRef)
                    }
                    if ((containsStateChangeTrigger(rule) || containsStateUpdateTrigger(rule)) && !containsParam(parameters, VAR_NEW_STATE)) {
                        val stateTypeRef = ruleModel.newTypeRef(State)
                        parameters += rule.toParameter(VAR_NEW_STATE, stateTypeRef)
                    }

                    body = rule.script
                ]
            ]
        ]
    }

    def private boolean containsParam(EList<JvmFormalParameter> params, String param) {
        return params.map[name].contains(param);
    }

    def private boolean containsCommandTrigger(Rule rule) {
        for (EventTrigger trigger : rule.getEventtrigger()) {
            if (trigger instanceof CommandEventTrigger) {
                return true;
            }
            if (trigger instanceof GroupMemberCommandEventTrigger) {
                return true;
            }
        }
        return false;
    }

    def private boolean containsStateChangeTrigger(Rule rule) {
        for (EventTrigger trigger : rule.getEventtrigger()) {
            if (trigger instanceof ChangedEventTrigger) {
                return true;
            }
            if (trigger instanceof GroupMemberChangedEventTrigger) {
                return true;
            }
        }
        return false;
    }

    def private boolean containsStateUpdateTrigger(Rule rule) {
        for (EventTrigger trigger : rule.getEventtrigger()) {
            if (trigger instanceof UpdateEventTrigger) {
                return true;
            }
            if (trigger instanceof GroupMemberUpdateEventTrigger) {
                return true;
            }
        }
        return false;
    }

    def private boolean containsEventTrigger(Rule rule) {
        for (EventTrigger trigger : rule.getEventtrigger()) {
            if (trigger instanceof EventEmittedTrigger) {
                return true;
            }
        }
        return false;
    }

    def private boolean containsThingStateChangedEventTrigger(Rule rule) {
        for (EventTrigger trigger : rule.getEventtrigger()) {
            if (trigger instanceof ThingStateChangedEventTrigger) {
                return true;
            }
        }
        return false;
    }
}
