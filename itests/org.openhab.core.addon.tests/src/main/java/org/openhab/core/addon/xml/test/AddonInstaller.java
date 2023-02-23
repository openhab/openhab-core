/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.addon.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.test.BundleCloseable;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.osgi.framework.BundleContext;

/**
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class AddonInstaller {

    private final Consumer<Runnable> waitForAssert;
    private final AddonInfoRegistry addonInfoRegistry;
    private final BundleContext bc;

    public AddonInstaller(Consumer<Runnable> waitForAssert, AddonInfoRegistry addonInfoRegistry, BundleContext bc) {
        this.waitForAssert = waitForAssert;
        this.addonInfoRegistry = addonInfoRegistry;
        this.bc = bc;
    }

    public void exec(final String bundleName, final Runnable func) throws Exception {
        // Save the number of currently installed bundles.
        final int initialNumberOfBindingInfos = addonInfoRegistry.getAddonInfos().size();

        // install test bundle
        try (BundleCloseable bundle = new BundleCloseable(SyntheticBundleInstaller.install(bc, bundleName))) {
            assertThat(bundle, is(notNullValue()));

            // Wait for correctly installed bundle.
            waitForAssert.accept(
                    () -> assertThat(addonInfoRegistry.getAddonInfos().size(), is(initialNumberOfBindingInfos + 1)));

            func.run();
        }

        // Wait for correctly uninstalled bundle.
        waitForAssert
                .accept(() -> assertThat(addonInfoRegistry.getAddonInfos().size(), is(initialNumberOfBindingInfos)));
    }
}
