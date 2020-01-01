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
package org.openhab.core.config.xml.util;

/**
 * The {@link NodeName} interface defines common features for all {@code Node}* classes.
 * <p>
 * Each {@code Node}* class has to return its node name.
 *
 * @author Michael Grammling - Initial contribution
 */
public interface NodeName {

    /**
     * Returns the name of the node this object belongs to.
     *
     * @return the name of the node this object belongs to (neither null, nor empty)
     */
    String getNodeName();

}
