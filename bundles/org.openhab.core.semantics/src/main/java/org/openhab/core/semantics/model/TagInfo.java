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
package org.openhab.core.semantics.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This is an annotation to be used on semantic tag classes for providing their ids, labels and descriptions.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface TagInfo {

    String id();

    String label() default "";

    String synonyms() default "";

    String description() default "";

}
