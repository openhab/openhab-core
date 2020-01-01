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
package org.openhab.core.config.xml.osgi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.framework.Bundle;

/**
 * The {@link XmlDocumentProviderFactory} is responsible to create {@link XmlDocumentProvider} instances for any certain
 * module. The factory is <i>not</i> responsible to clean-up any
 * created providers.
 * <p>
 * The {@link XmlDocumentProviderFactory} is used by the {@link XmlDocumentBundleTracker} to create for each module an
 * own {@link XmlDocumentProvider} instance to process any result objects from the XML conversion.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @param <T> the result type of the conversion
 * @see XmlDocumentProvider
 */
@NonNullByDefault
public interface XmlDocumentProviderFactory<T> {

    /**
     * Creates an XML document provider for the specified module which is used to process
     * any result objects from the XML conversion.
     *
     * @param bundle the module for which the provider must be created (must not be null)
     * @return the created provider for the specified module (must not be null)
     */
    XmlDocumentProvider<T> createDocumentProvider(Bundle bundle);
}
