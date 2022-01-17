/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.tools.i18n.plugin;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.binding.xml.internal.BindingInfoXmlResult;

/**
 * Tests {@link BundleInfoReader}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class BundleInfoReaderTest {

    @Test
    public void readBindingInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/acmeweather.bundle/OH-INF"));

        BindingInfoXmlResult bindingInfoXml = bundleInfo.getBindingInfoXml();
        assertThat(bindingInfoXml, is(notNullValue()));
        if (bindingInfoXml != null) {
            assertThat(bindingInfoXml.getBindingInfo().getName(), is("ACME Weather Binding"));
            assertThat(bindingInfoXml.getBindingInfo().getDescription(),
                    is("ACME Weather - Current weather and forecasts in your city."));
        }

        assertThat(bundleInfo.getBindingId(), is("acmeweather"));
        assertThat(bundleInfo.getChannelGroupTypesXml().size(), is(1));
        assertThat(bundleInfo.getChannelTypesXml().size(), is(2));
        assertThat(bundleInfo.getConfigDescriptions().size(), is(1));
        assertThat(bundleInfo.getThingTypesXml().size(), is(2));
    }

    @Test
    public void readGenericBundleInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/acmetts.bundle/OH-INF"));

        assertThat(bundleInfo.getBindingInfoXml(), is(nullValue()));
        assertThat(bundleInfo.getBindingId(), is(""));
        assertThat(bundleInfo.getChannelGroupTypesXml().size(), is(0));
        assertThat(bundleInfo.getChannelTypesXml().size(), is(0));
        assertThat(bundleInfo.getConfigDescriptions().size(), is(1));
        assertThat(bundleInfo.getThingTypesXml().size(), is(0));
    }

    @Test
    public void readPathWithoutAnyInfo() throws IOException {
        BundleInfoReader reader = new BundleInfoReader(new SystemStreamLog());
        BundleInfo bundleInfo = reader.readBundleInfo(Path.of("src/test/resources/infoless.bundle/OH-INF"));

        assertThat(bundleInfo.getBindingInfoXml(), is(nullValue()));
        assertThat(bundleInfo.getBindingId(), is(""));
        assertThat(bundleInfo.getChannelGroupTypesXml().size(), is(0));
        assertThat(bundleInfo.getChannelTypesXml().size(), is(0));
        assertThat(bundleInfo.getConfigDescriptions().size(), is(0));
        assertThat(bundleInfo.getThingTypesXml().size(), is(0));
    }
}
