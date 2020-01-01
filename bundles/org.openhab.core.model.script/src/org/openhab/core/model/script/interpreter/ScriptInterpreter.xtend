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
package org.openhab.core.model.script.interpreter;

import com.google.inject.Inject
import org.openhab.core.items.Item
import org.openhab.core.items.ItemNotFoundException
import org.openhab.core.items.ItemRegistry
import org.openhab.core.library.types.QuantityType
import org.openhab.core.types.Type
import org.openhab.core.model.script.engine.ScriptError
import org.openhab.core.model.script.engine.ScriptExecutionException
import org.openhab.core.model.script.lib.NumberExtensions
import org.openhab.core.model.script.scoping.StateAndCommandProvider
import org.openhab.core.model.script.script.QuantityLiteral
import org.eclipse.xtext.common.types.JvmField
import org.eclipse.xtext.common.types.JvmIdentifiableElement
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XCastedExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XFeatureCall
import org.eclipse.xtext.xbase.XMemberFeatureCall
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver

/**
 * The script interpreter handles ESH specific script components, which are not known
 * to the standard Xbase interpreter.
 * 
 * @author Kai Kreuzer - Initial contribution and API
 * @author Oliver Libutzki - Xtext 2.5.0 migration
 * 
 */
@SuppressWarnings("restriction")
public class ScriptInterpreter extends XbaseInterpreter {

    @Inject
    ItemRegistry itemRegistry

    @Inject
    StateAndCommandProvider stateAndCommandProvider
    
    @Inject
    private IBatchTypeResolver typeResolver;

    @Inject
    extension IJvmModelAssociations

    override protected _invokeFeature(JvmField jvmField, XAbstractFeatureCall featureCall, Object receiver,
        IEvaluationContext context, CancelIndicator indicator) {

        // Check if the JvmField is inferred
        val sourceElement = jvmField.sourceElements.head
        if (sourceElement !== null) {
            val value = context.getValue(QualifiedName.create(jvmField.simpleName))
            value ?: {

                // Looks like we have an state, command or item field
                val fieldName = jvmField.simpleName
                fieldName.stateOrCommand ?: fieldName.item
            }
        } else {
            super._invokeFeature(jvmField, featureCall, receiver, context, indicator)
        }

    }

    override protected invokeFeature(JvmIdentifiableElement feature, XAbstractFeatureCall featureCall,
        Object receiverObj, IEvaluationContext context, CancelIndicator indicator) {
        if (feature !== null && feature.eIsProxy) {
            if (featureCall instanceof XMemberFeatureCall) {
                val expression = featureCall.memberCallTarget;
                val type = typeResolver.resolveTypes(expression)?.getActualType(expression)?.identifier;
                throw new ScriptExecutionException(new ScriptError(
                    "'" + featureCall.getConcreteSyntaxFeatureName() + "' is not a member of '" + type + "'", featureCall));
            } else if (featureCall instanceof XFeatureCall) {
                throw new ScriptExecutionException(new ScriptError(
                    "The name '" + featureCall.getConcreteSyntaxFeatureName() +
                        "' cannot be resolved to an item or type", featureCall));
            } else {
                throw new ScriptExecutionException(new ScriptError(
                    "Unknown variable or command '" + featureCall.getConcreteSyntaxFeatureName() + "'", featureCall));
            }
        }
        super.invokeFeature(feature, featureCall, receiverObj, context, indicator)
    }

    def protected Type getStateOrCommand(String name) {
        for (Type type : stateAndCommandProvider.getAllTypes()) {
            if (type.toString == name) {
                return type
            }
        }
    }

    def protected Item getItem(String name) {
        try {
            return itemRegistry.getItem(name);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    override protected boolean eq(Object a, Object b) {
        if (a instanceof Type && b instanceof Number) {
            return NumberExtensions.operator_equals(a as Type, b as Number);
        } else if (a instanceof Number && b instanceof Type) {
            return NumberExtensions.operator_equals(b as Type, a as Number);
        } else {
            return super.eq(a, b);
        }
    }

    override _assignValueTo(JvmField jvmField, XAbstractFeatureCall assignment, Object value,
        IEvaluationContext context, CancelIndicator indicator) {

        // Check if the JvmField is inferred
        val sourceElement = jvmField.sourceElements.head
        if (sourceElement != null) {
            context.assignValue(QualifiedName.create(jvmField.simpleName), value)
            value
        } else {
            super._assignValueTo(jvmField, assignment, value, context, indicator)
        }
    }

    override protected doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
        if (expression === null) {
            return null
        } else if (expression instanceof QuantityLiteral) {
            return doEvaluate(expression as QuantityLiteral, context, indicator)
        } else {
            return super.doEvaluate(expression, context, indicator)
        }
    }

    def  protected  Object doEvaluate(QuantityLiteral literal, IEvaluationContext context, CancelIndicator indicator) {
        return QuantityType.valueOf(literal.value + " " + literal.unit.value);
    }

    override Object _doEvaluate(XCastedExpression castedExpression, IEvaluationContext context,
        CancelIndicator indicator) {
        try {
            return super._doEvaluate(castedExpression, context, indicator)
        } catch (RuntimeException e) {
            if (e.cause instanceof ClassCastException) {
                val Object result = internalEvaluate(castedExpression.getTarget(), context, indicator);
                throw new ScriptExecutionException(new ScriptError(
                    "Could not cast " + result + " to " + castedExpression.getType().getType().getQualifiedName(),
                    castedExpression));
                } else {
                    throw e;
                }
            }
        }

    }
    