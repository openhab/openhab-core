/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.boot.rest.internal.dto;

import java.time.Instant;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author David Graeff - Initial contribution
 */
@ApiModel(description = "Provides information about this openHAB instance")
public class About {
    @ApiModelProperty(value = "The product core name, usually \"openHAB\"")
    public final String name;
    @ApiModelProperty(value = "The product core version. This does not express any API version guarantees.")
    public final String version;
    @ApiModelProperty(value = "The build date in the form of 2017-04-07T18:07:00")
    public final Date builddate;

    @ApiModel(description = "Optional distribution information")
    public static class Distribution {
        @ApiModelProperty(value = "The distribution name")
        public final String name;
        @ApiModelProperty(value = "The distribution version")
        public final String version;
        @ApiModelProperty(value = "An optional, version independant url for further information about the distribution")
        public final String abouturl;

        Distribution() {
            name = "";
            version = "";
            abouturl = "";
        }

        public Distribution(String name, String version, String abouturl) {
            this.name = name;
            this.version = version;
            this.abouturl = abouturl;
        }

    }

    public final Distribution distribution;

    About() {
        name = "";
        version = "";
        builddate = Date.from(Instant.now());
        distribution = null;
    }

    public About(String name, String version, Date builddate, Distribution distribution) {
        this.name = name;
        this.version = version;
        this.builddate = builddate;
        this.distribution = distribution;
    }
}
