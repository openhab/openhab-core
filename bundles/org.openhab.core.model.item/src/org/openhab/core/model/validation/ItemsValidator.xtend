/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.validation.Check
import org.openhab.core.common.AbstractUID
import org.openhab.core.items.GroupFunction
import org.openhab.core.items.ItemUtil
import org.openhab.core.library.CoreItemFactory
import org.openhab.core.model.items.ItemsPackage
import org.openhab.core.model.items.ModelBinding
import org.openhab.core.model.items.ModelItem
import org.openhab.core.model.items.ModelProperty
import org.openhab.core.types.util.UnitUtils

/**
 * Custom validation rules.
 *
 * see https://eclipse.dev/Xtext/documentation/303_runtime_concepts.html#validation
 */
class ItemsValidator extends AbstractItemsValidator {

	@Check
	def checkItemName(ModelItem item) {
		if (item === null || item.name === null) {
			return
		}
        if (!ItemUtil.isValidItemName(item.name)) {
			error(buildMsgWithLineNb(item, "Item name '" + item.name + "' is invalid. It must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols."),
			    ItemsPackage.Literals.MODEL_ITEM__NAME)
        }
    }

    @Check
    def checkValidItemType(ModelItem item) {
        if (item === null || item.type === null) {
            return
        }

        val segments = item.type.split(":")
        val mainType = segments.get(0)
        switch mainType {
            case "Group": checkGroupType(item, segments)
            case "Number": checkNumberType(item, segments)
            default: checkBasicItemType(item)
        }
    }

    def checkNumberType(ModelItem item, String[] segments) {
        switch (segments.size) {
            case 1: return // Just "Number", valid
            case 2: checkDimension(item, segments.get(1)) // "Number:<dimension>"
            default: error(buildMsgWithLineNb(item, "Item: '" + item.name + "' has an invalid Number type, too many segments: '" + item.type + "'"),
                ItemsPackage.Literals.MODEL_ITEM__TYPE)
        }
    }

    def checkBasicItemType(ModelItem item) {
        if (!CoreItemFactory.VALID_ITEM_TYPES.contains(item.type)) {
            error(buildMsgWithLineNb(item, "Item '" + item.name + "' has an invalid type: '" + item.type + "'"),
                ItemsPackage.Literals.MODEL_ITEM__TYPE)
        }
    }

    def checkGroupType(ModelItem item, String[] segments) {
        switch (segments.size) {
            case 1: return // Just "Group", valid
            case 2: checkGroupBaseType(item, segments.get(1))
            case 3: checkGroupWithOneParam(item, segments.get(1), segments.get(2))
            case 4: checkGroupWithTwoParams(item, segments.get(1), segments.get(2), segments.get(3))
            // The xtext grammar will not allow more than 4 segments for Group types, so no default case is needed
        }
    }

    def checkGroupBaseType(ModelItem item, String baseType) {
        if (!CoreItemFactory.VALID_ITEM_TYPES.contains(baseType)) {
            error(buildMsgWithLineNb(item, "Item '" + item.name + "' has an invalid base item type: '" + baseType + "'"),
                ItemsPackage.Literals.MODEL_ITEM__TYPE)
        }
    }

    def checkGroupWithOneParam(ModelItem item, String baseType, String param) {
        checkGroupBaseType(item, baseType)

        if (item.args.size == 0 && baseType == "Number") {
            if (!isValidGroupFunction(param)) {
                // Group:Number:Dimension
                checkDimension(item, param)
            }
        } else {
            checkGroupFunction(item, param)
        }
    }

    def checkGroupWithTwoParams(ModelItem item, String baseType, String dimension, String function) {
        checkGroupBaseType(item, baseType)

        if (baseType == "Number") {
            checkDimension(item, dimension)
        } else {
            error(buildMsgWithLineNb(item, "Item '" + item.name + "' with type '" + item.type + "' cannot have a dimension. Dimensions are only valid for Number base type. The dimension '" + dimension + "' is ignored."),
                ItemsPackage.Literals.MODEL_ITEM__TYPE)
        }

        checkGroupFunction(item, function)
    }

    def checkDimension(ModelItem item, String dimension) {
        try {
            UnitUtils.parseDimension(dimension)
        } catch (IllegalArgumentException e) {
            error(buildMsgWithLineNb(item, "Item '" + item.name + "' has an unknown dimension: '" + dimension + "'. The Item will not be created."),
                ItemsPackage.Literals.MODEL_ITEM__TYPE)
        }
    }

    def checkGroupFunction(ModelItem item, String function) {
        if (!isValidGroupFunction(function)) {
            warning(buildMsgWithLineNb(item, "Item '" + item.name + "' has an unknown group function: '" + function + "'. Using EQUALITY instead."),
                ItemsPackage.Literals.MODEL_ITEM__TYPE)
        }
    }

    def isValidGroupFunction(String value) {
        return GroupFunction.VALID_FUNCTIONS.contains(value)
    }

    @Check
    def checkValidChannelProfileType(ModelItem item) {
        if (item === null || item.bindings === null) {
            return
        }
        for (ModelBinding binding : item.bindings) {
            if (binding.type == "channel") {
                for (ModelProperty property : binding.properties) {
                    if (property.key == "profile") {
                        for (Object value : property.value) {
                            try {
                                new ProfileTypeUID(value.toString.split(":").length == 1
                                    ? "system:" + value.toString
                                    : value.toString)
                            } catch (IllegalArgumentException e) {
                                error(buildMsgWithLineNb(property, "Item '" + item.name + "' has an invalid profile value '" + value.toString
                                    + "' for channel '" + binding.configuration + "': " + e.message),
                                    ItemsPackage.Literals.MODEL_ITEM__BINDINGS)
                            }
                        }
                    }
                }
            }
        }
    }

    static class ProfileTypeUID extends AbstractUID {
        new(String profileType) {
            super(profileType)
        }

        override int getMinimalNumberOfSegments() {
            return 2
        }
    }

    def private buildMsgWithLineNb(EObject object, String msg) {
        val node = NodeModelUtils.getNode(object)
        val startLine = node.startLine
        val endLine = node.endLine
        if (startLine == endLine) {
            return "Line " + startLine + ": " + msg
        } else {
            return "Line " + startLine + "-" + endLine + ": " + msg
        }
    }
}
