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
package org.eclipse.smarthome.model.formatting

import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter
import org.eclipse.xtext.formatting.impl.FormattingConfig
import com.google.inject.Inject
import org.eclipse.smarthome.model.services.ItemsGrammarAccess

/**
 * This class contains custom formatting description.
 */
class ItemsFormatter extends AbstractDeclarativeFormatter {

	@Inject extension ItemsGrammarAccess

	override protected void configureFormatting(FormattingConfig c) {
		c.setLinewrap(1, 1, 2).before(modelGroupItemRule)
		c.setLinewrap(1, 1, 2).before(modelItemTypeRule)

		c.setNoSpace().withinKeywordPairs("<", ">")
		c.setNoSpace().withinKeywordPairs("(", ")")

		c.setIndentationIncrement.after(modelItemTypeRule)
		c.setIndentationDecrement.before(modelItemTypeRule)
		c.setIndentationIncrement.after(modelGroupItemRule)
		c.setIndentationDecrement.before(modelGroupItemRule)

		c.autoLinewrap = 160
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
}
