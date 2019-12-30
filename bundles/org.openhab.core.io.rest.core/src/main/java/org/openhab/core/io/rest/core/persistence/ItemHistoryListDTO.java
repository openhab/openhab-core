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
package org.openhab.core.io.rest.core.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openhab.core.persistence.dto.ItemHistoryDTO;

/**
 * This is a java bean that is used to serialize item lists.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ItemHistoryListDTO {
    public final List<ItemHistoryDTO> item = new ArrayList<>();

    public ItemHistoryListDTO() {
    }

    public ItemHistoryListDTO(Collection<ItemHistoryDTO> list) {
        item.addAll(list);
    }
}
