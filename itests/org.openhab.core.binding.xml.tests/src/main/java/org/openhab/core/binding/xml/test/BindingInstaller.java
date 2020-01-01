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
package org.openhab.core.binding.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.function.Consumer;

import org.openhab.core.binding.BindingInfoRegistry;
import org.openhab.core.test.BundleCloseable;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.osgi.framework.BundleContext;

/**
 * @author Markus Rathgeb - Initial contribution
 */
public class BindingInstaller {

    private final Consumer<Runnable> waitForAssert;
    private final BindingInfoRegistry bindingInfoRegistry;
    private final BundleContext bc;

    public BindingInstaller(Consumer<Runnable> waitForAssert, BindingInfoRegistry bindingInfoRegistry,
            BundleContext bc) {
        this.waitForAssert = waitForAssert;
        this.bindingInfoRegistry = bindingInfoRegistry;
        this.bc = bc;
    }

    public void exec(final String bundleName, final Runnable func) throws Exception {
        // Save the number of currently installed bundles.
        final int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        try (BundleCloseable bundle = new BundleCloseable(SyntheticBundleInstaller.install(bc, bundleName))) {
            assertThat(bundle, is(notNullValue()));

            // Wait for correctly installed bundle.
            waitForAssert.accept(() -> assertThat(bindingInfoRegistry.getBindingInfos().size(),
                    is(initialNumberOfBindingInfos + 1)));

            func.run();
        }

        // Wait for correctly uninstalled bundle.
        waitForAssert.accept(
                () -> assertThat(bindingInfoRegistry.getBindingInfos().size(), is(initialNumberOfBindingInfos)));
    }

}
