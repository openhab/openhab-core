/*
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
package org.openhab.core.persistence;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;

/**
 * The {@link PersistenceManager} interface is used to communicate between external components (e.g. REST interface)
 * that modify persisted data bypassing the {@link org.openhab.core.persistence.internal.PersistenceManagerImpl}. This
 * is required because forecast jobs might need an update when the persisted data changes.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface PersistenceManager {
    void handleExternalPersistenceDataChange(PersistenceService persistenceService, Item item);
}
