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
package org.openhab.core.io.rest.update.internal.dto;

import javax.validation.constraints.Pattern;

/**
 * This is a DTO for OpenHAB self updating features.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
public class UpdaterExecuteDTO {
    @Pattern(regexp = "^(STABLE|MILESTONE|SNAPSHOT)$")
    public String targetNewVersionType;
    @Pattern(regexp = "^[a-zA-Z0-9_]{0,20}$")
    public String user;
    @Pattern(regexp = "^\\S{0,20}$")
    public String password;
}
