/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * <p>
 * {@link ConfigurableService} can be used as a marker interface for configurable services. But the interface itself is
 * not relevant for the runtime. Each service which has the property
 * {@link ConfigurableService#description_uri} set will be considered as a configurable service. The
 * properties {@link ConfigurableService#label} and {@link ConfigurableService#category} are optional.
 *
 * <p>
 * The services are configured through the OSGi configuration admin. Therefore each service must provide a PID or a
 * component name service property if the configuration is done by declarative services. If the
 * {@link Constants#SERVICE_PID} property is not set the
 * {@link ComponentConstants#COMPONENT_NAME} property will be used as fallback.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Change to ComponentPropertyType
 */
@ComponentPropertyType
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@NonNullByDefault
public @interface ConfigurableService {

    String PREFIX_ = "service.config.";

    /**
     * The config description URI for the configurable service. See also {@link ConfigDescription}.
     */
    String description_uri();

    /**
     * The label of the service to be configured.
     */
    String label() default "";

    /**
     * The category of the service to be configured (e.g. binding).
     */
    String category() default "";

    /**
     * Marker for multiple configurations for this service ("true" = multiple configurations possible)
     */
    boolean factory() default false;
}
