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
package org.eclipse.smarthome.automation.sample.moduletype.demo.internal.handlers;

import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.eclipse.smarthome.automation.sample.moduletype.demo.internal.factory.HandlerFactory;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * This class is handler for 'CompareCondition' {@link Condition}.
 *
 * <pre>
 * Example usage:
 *
 * "id":"RuleCondition",
 * "type":"CompareCondition",
 * "configuration":{
 *    "operator":"=",
 *    "constraint":10
 * },
 * "inputs":{
 *    "inputValue":"RuleTrigger.outputValue"
 * }
 * </pre>
 *
 * It evaluates an expression which has the following format:
 *
 * <pre>
 * constraint operator inputValue
 * </pre>
 * 
 * @author Plamen Peev - Initial contribution
 */
public class CompareCondition extends BaseModuleHandler<Condition> implements ConditionHandler {

    /**
     * This constant is used by {@link HandlerFactory} to create a correct handler instance. It must be the same as in
     * JSON definition of the module type.
     */
    public static final String UID = "CompareCondition";

    /**
     * Describes all possible operators which this handler can perform
     *
     * @author Plamen Peev
     *
     */
    private static enum Operator {
        EQUAL("="),
        NOT_EQUAL("!="),
        LESS("<"),
        GREATER(">");

        private final String operation;

        private Operator(String operation) {
            this.operation = operation;
        }

        public String getOperation() {
            return operation;
        }

        public static Operator getOperatorByStr(String str) {
            if (str.equals(EQUAL.getOperation())) {
                return EQUAL;
            } else if (str.equals(NOT_EQUAL.getOperation())) {
                return NOT_EQUAL;
            } else if (str.equals(LESS.getOperation())) {
                return LESS;
            } else if (str.equals(GREATER.getOperation())) {
                return GREATER;
            } else {
                throw new IllegalArgumentException("Unknown operator");
            }
        }
    }

    /**
     * This constant is used to get the value of the 'operator' property from {@link Condition}'s {@link Configuration}.
     */
    public static final String OPERATOR = "operator";

    /**
     * This constant is used to get the value of the 'constraint' property from {@link Condition}'s
     * {@link Configuration}.
     */
    public static final String CONSTRAINT = "constraint";

    /**
     * This constant contains the name of the input for this {@link Condition} handler.
     */
    private static final String INPUT_NAME = "inputValue";

    /**
     * This field will be containing the constraint value which will be used in evaluation this {@link Condition}
     * handler.
     */
    private final int constraint;

    /**
     * This field will be containing the operator which will be used to evaluate this {@link Condition} handler.
     */
    private final Operator operator;

    /**
     * Constructs a {@link CompareCondition} instance.
     *
     * @param module - the {@link Condition} for which the instance is created.
     */
    public CompareCondition(Condition module) {
        super(module);

        if (module == null) {
            throw new IllegalArgumentException("'module' can not be null.");
        }

        final Configuration configuration = module.getConfiguration();
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration can't be null.");
        }

        final Number constraint = (Number) configuration.get(CONSTRAINT);
        if (constraint == null) {
            throw new IllegalArgumentException("'constraint' can not be null.");
        }
        this.constraint = constraint.intValue();

        final String operatorAsString = (String) configuration.get(OPERATOR);
        if (operatorAsString == null) {
            throw new IllegalArgumentException("'operator' can not be null.");
        }
        this.operator = Operator.getOperatorByStr(operatorAsString);
    }

    /**
     * This method is used in the evaluation of this {@link Condition} handler.
     * It compares the value from the input with the value from {@link Condition}'s configuration.
     */
    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        final Integer value = (Integer) inputs.get(INPUT_NAME);
        if (value == null) {
            return false;
        }
        switch (operator) {
            case EQUAL:
                return constraint == value.intValue();
            case NOT_EQUAL:
                return constraint != value.intValue();
            case GREATER:
                return constraint > value.intValue();
            case LESS:
                return constraint < value.intValue();
            default:
                return false;
        }
    }

}
