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
package org.openhab.core.automation.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Input parameter for an action module
 *
 * @author Stefan Triller - Initial contribution
 */
@Repeatable(ActionInputs.class)
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ActionInput {

    String name();

    String type() default "";

    String label() default "";

    String description() default "";

    String[] tags() default {};

    boolean required() default false;

    String reference() default "";

    String defaultValue() default "";
}
