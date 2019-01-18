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
package org.eclipse.smarthome.automation.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Output parameter for an action module
 *
 * @author Stefan Triller - initial contribution
 */
@Repeatable(ActionOutputs.class)
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface ActionOutput {

    String name();

    String type();

    String label() default "";

    String description() default "";

    String[] tags() default {};

    String reference() default "";

    String defaultValue() default "";
}
