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
package org.openhab.core.model.formatting

import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter
import org.eclipse.xtext.formatting.impl.FormattingConfig
import com.google.inject.Inject
import org.openhab.core.model.services.ItemsGrammarAccess

/**
 * This class contains custom formatting description.
 */
class ItemsFormatter extends AbstractDeclarativeFormatter {

	@Inject extension ItemsGrammarAccess

	override protected void configureFormatting(FormattingConfig c) {
		c.setLinewrap(1, 1, 2).before(modelItemRule)

		// No space between the item type and the opening parenthesis for the group function arguments
		c.setNoSpace().between(modelItemTypeRule, modelItemAccess.leftParenthesisKeyword_1_0)

		c.setNoSpace().withinKeywordPairs("<", ">")
		c.setNoSpace().withinKeywordPairs("(", ")")
		c.setNoSpace().withinKeywordPairs("[", "]")

		c.setNoSpace().around(":", "=")
		c.setNoSpace().before(",")

		c.autoLinewrap = 400

		c.setLinewrap(0, 1, 2).before(SL_COMMENTRule)
		c.setLinewrap(0, 1, 2).before(ML_COMMENTRule)
		c.setLinewrap(0, 1, 1).after(ML_COMMENTRule)
	}

	def withinKeywordPairs(FormattingConfig.NoSpaceLocator locator, String leftKW, String rightKW) {
		for (pair : findKeywordPairs(leftKW, rightKW)) {
			locator.after(pair.first)
			locator.before(pair.second)
		}
	}

	def around(FormattingConfig.ElementLocator locator, String ... listKW) {
		for (keyword : findKeywords(listKW)) {
			locator.around(keyword)
		}
	}

	def before(FormattingConfig.ElementLocator locator, String ... listKW) {
		for (keyword : findKeywords(listKW)) {
			locator.before(keyword)
		}
	}
}
