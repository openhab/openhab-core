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
package org.openhab.core.model.validation

import org.openhab.core.model.items.ModelItem
import org.eclipse.xtext.validation.Check
import org.openhab.core.model.items.ItemsPackage
import org.openhab.core.types.util.UnitUtils

/**
 * Custom validation rules. 
 *
 * see http://www.eclipse.org/Xtext/documentation.html#validation
 */
class ItemsValidator extends AbstractItemsValidator {

	@Check
	def checkItemName(ModelItem item) {
		if (item === null || item.name === null) {
			return
		}
		if (item.name.contains("-")) {
			error('Item name must not contain dashes.', ItemsPackage.Literals.MODEL_ITEM__NAME)
		}
	}
	
	@Check
	def checkDiemension(ModelItem item) {
	    if (item === null || item.type === null) {
            return
        }
        if (item.type.startsWith("Number:")) {
            var dimension = item.type.substring(item.type.indexOf(":") + 1)
            try {
                UnitUtils.parseDimension(dimension)                
            } catch (IllegalArgumentException e) {
                warning("'" + dimension + "' is not a valid dimension.", ItemsPackage.Literals.MODEL_ITEM__TYPE)
            }
        }
	}
}
