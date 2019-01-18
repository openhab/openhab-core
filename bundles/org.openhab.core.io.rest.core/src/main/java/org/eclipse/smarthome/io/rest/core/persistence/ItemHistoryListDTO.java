/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.core.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.smarthome.core.persistence.dto.ItemHistoryDTO;

/**
 * This is a java bean that is used to serialize item lists.
 *
 * @author Chris Jackson - Initial Contribution
 *
 */
public class ItemHistoryListDTO {
    public final List<ItemHistoryDTO> item = new ArrayList<ItemHistoryDTO>();

    public ItemHistoryListDTO() {
    }

    public ItemHistoryListDTO(Collection<ItemHistoryDTO> list) {
        item.addAll(list);
    }
}
