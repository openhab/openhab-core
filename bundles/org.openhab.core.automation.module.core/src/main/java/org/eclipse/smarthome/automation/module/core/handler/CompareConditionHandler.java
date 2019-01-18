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
package org.eclipse.smarthome.automation.module.core.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.eclipse.smarthome.automation.module.core.internal.exception.UncomparableException;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.TypeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Comparation Condition
 *
 * @author Benedikt Niehues - Initial contribution and API
 *
 */
public class CompareConditionHandler extends BaseModuleHandler<Condition> implements ConditionHandler {

    public final Logger logger = LoggerFactory.getLogger(CompareConditionHandler.class);

    public static final String MODULE_TYPE = "core.GenericCompareCondition";

    public static final String INPUT_LEFT_OBJECT = "input";
    public static final String INPUT_LEFT_FIELD = "inputproperty";
    public static final String RIGHT_OP = "right";
    public static final String OPERATOR = "operator";

    public CompareConditionHandler(Condition module) {
        super(module);
    }

    @Override
    public boolean isSatisfied(Map<String, Object> context) {
        Object operatorObj = this.module.getConfiguration().get(OPERATOR);
        String operator = (operatorObj != null && operatorObj instanceof String) ? (String) operatorObj : null;
        Object rightObj = this.module.getConfiguration().get(RIGHT_OP);
        String rightOperandString = (rightObj != null && rightObj instanceof String) ? (String) rightObj : null;
        Object leftObjFieldNameObj = this.module.getConfiguration().get(INPUT_LEFT_FIELD);
        String leftObjectFieldName = (leftObjFieldNameObj != null && leftObjFieldNameObj instanceof String)
                ? (String) leftObjFieldNameObj : null;
        if (rightOperandString == null || operator == null) {
            return false;
        } else {
            Object leftObj = context.get(INPUT_LEFT_OBJECT);
            Object toCompare = getCompareValue(leftObj, leftObjectFieldName);
            Object rightValue = getRightOperandValue(rightOperandString, toCompare);
            if (rightValue == null) {
                if (leftObj != null) {
                    logger.info("unsupported type for compare condition: {}", leftObj.getClass());
                } else {
                    logger.info("unsupported type for compare condition: null ({})",
                            module.getInputs().get(INPUT_LEFT_FIELD));
                }
                return false;
            }
            try {
                switch (operator) {
                    case "eq":
                    case "EQ":
                    case "=":
                    case "==":
                    case "equals":
                    case "EQUALS":
                        // EQUALS
                        if (toCompare == null) {
                            if (rightOperandString.equals("null") || rightOperandString.equals("")) {
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return toCompare.equals(rightValue);
                        }
                    case "gt":
                    case "GT":
                    case ">":
                        // Greater
                        if (toCompare == null || rightValue == null) {
                            return false;
                        } else {
                            return compare(toCompare, rightValue) > 0;
                        }
                    case "gte":
                    case "GTE":
                    case ">=":
                    case "=>":
                        // Greater or equal
                        if (toCompare == null || rightValue == null) {
                            return false;
                        } else {
                            return compare(toCompare, rightValue) >= 0;
                        }
                    case "lt":
                    case "LT":
                    case "<":
                        if (toCompare == null || rightValue == null) {
                            return false;
                        } else {
                            return compare(toCompare, rightValue) < 0;
                        }
                    case "lte":
                    case "LTE":
                    case "<=":
                    case "=<":
                        if (toCompare == null || rightValue == null) {
                            return false;
                        } else {
                            return compare(toCompare, rightValue) <= 0;
                        }
                    case "matches":
                        // Matcher...
                        if (toCompare instanceof String && rightValue != null && rightValue instanceof String) {
                            return ((String) toCompare).matches((String) rightValue);
                        }
                    default:
                        break;
                }
            } catch (UncomparableException e) {
                // values can not be compared, so assume that the condition is not satisfied
                return false;
            }

            return false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private int compare(Object a, Object b) throws UncomparableException {
        if (Comparable.class.isAssignableFrom(a.getClass()) && a.getClass().equals(b.getClass())) {
            try {
                return ((Comparable) a).compareTo(b);
            } catch (ClassCastException e) {
                // should never happen but to be save here!
                throw new UncomparableException();
            }
        }
        throw new UncomparableException();
    }

    private Object getRightOperandValue(String rightOperandString2, Object toCompare) {
        if (rightOperandString2.equals("null")) {
            return rightOperandString2;
        }
        if (toCompare instanceof State) {
            List<Class<? extends State>> stateTypeList = new ArrayList<Class<? extends State>>();
            stateTypeList.add(((State) toCompare).getClass());
            return TypeParser.parseState(stateTypeList, rightOperandString2);
        } else if (toCompare instanceof Integer) {
            return Integer.parseInt(rightOperandString2);
        } else if (toCompare instanceof String) {
            return rightOperandString2;
        } else if (toCompare instanceof Long) {
            return Long.parseLong(rightOperandString2);
        } else if (toCompare instanceof Double) {
            return Double.parseDouble(rightOperandString2);
        }
        return null;
    }

    private Object getCompareValue(Object leftObj, String leftObjFieldName) {
        if (leftObj == null || leftObjFieldName == null || leftObjFieldName.isEmpty() || leftObj instanceof String
                || leftObj instanceof Integer || leftObj instanceof Long || leftObj instanceof Double) {
            return leftObj;
        } else {
            try {
                Method m = leftObj.getClass().getMethod(
                        "get" + leftObjFieldName.substring(0, 1).toUpperCase() + leftObjFieldName.substring(1));
                return m.invoke(leftObj);
            } catch (NoSuchMethodException | SecurityException | StringIndexOutOfBoundsException
                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                return null;
            }
        }
    }

}
