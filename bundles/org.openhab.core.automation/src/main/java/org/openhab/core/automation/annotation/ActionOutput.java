/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import static org.openhab.core.automation.internal.module.handler.AnnotationActionHandler.MODULE_RESULT;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Output parameter for an action module
 *
 * @author Stefan Triller - Initial contribution
 */
@Repeatable(ActionOutputs.class)
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface ActionOutput {

    /**
     * Name of the output parameter
     * <p>
     * There are some reserved names that make the UI render the output pretty and not just as a text field:
     * <ul>
     * <li>qrCode: Render the output as a QR code.</li>
     * </ul>
     *
     * @return the name of the output parameter
     */
    String name() default MODULE_RESULT;

    /**
     * Type of the output parameter
     * <p>
     * There are some special types that make the UI render the output pretty and not just as a text field:
     * <ul>
     * <li>qrCode: Render the output as a QR code.</li>
     * </ul>
     *
     * @return the type of the output parameter
     */
    String type();

    String label() default "";

    String description() default "";

    String[] tags() default {};

    String reference() default "";

    String defaultValue() default "";
}
