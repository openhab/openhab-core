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
package org.openhab.core.tools.i18n.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Base class for internationalization mojos using openHAB XML information.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractI18nMojo extends AbstractMojo {

    /**
     * The directory containing the bundle openHAB information
     */
    @Parameter(property = "i18n.ohinf.dir", defaultValue = "${project.basedir}/src/main/resources/OH-INF")
    protected @NonNullByDefault({}) File ohinfDirectory;

    protected BundleInfo bundleInfo = new BundleInfo();

    protected boolean ohinfExists() {
        return ohinfDirectory.exists();
    }

    protected void readAddonInfo() throws IOException {
        BundleInfoReader bundleInfoReader = new BundleInfoReader(getLog());
        bundleInfo = bundleInfoReader.readBundleInfo(ohinfDirectory.toPath());
    }

    void setOhinfDirectory(File ohinfDirectory) {
        this.ohinfDirectory = ohinfDirectory;
    }
}
