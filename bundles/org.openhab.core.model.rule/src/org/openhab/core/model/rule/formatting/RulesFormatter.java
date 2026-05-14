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
package org.openhab.core.model.rule.formatting;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter;
import org.eclipse.xtext.formatting.impl.AbstractFormattingConfig;
import org.eclipse.xtext.formatting.impl.FormattingConfig;
import org.eclipse.xtext.parsetree.reconstr.ITokenStream;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xtext.XtextFormatter;
import org.openhab.core.model.rule.services.RulesGrammarAccess;

import com.google.inject.Inject;

/**
 * This class contains custom formatting description.
 *
 * see : http://www.eclipse.org/Xtext/documentation.html#formatting
 * on how and when to use it
 *
 * Also see {@link XtextFormatter} as an example
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RulesFormatter extends AbstractDeclarativeFormatter {

    @Inject
    @Extension
    private RulesGrammarAccess rulesGrammarAccess;

    @Override
    protected void configureFormatting(@NonNullByDefault({}) FormattingConfig c) {
        c.setLinewrap(1, 1, 2).before(rulesGrammarAccess.getRuleModelRule());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getRuleModelRule());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getXImportDeclarationRule());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getXFunctionTypeRefRule());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getXBlockExpressionRule());
        c.setLinewrap(1, 2, 2).before(rulesGrammarAccess.getRuleAccess().getRuleKeyword_0());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getRuleAccess().getOrKeyword_4_2_0());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getRuleAccess().getAndKeyword_5_4_0());
        c.setLinewrap(1, 1, 2).before(rulesGrammarAccess.getRuleAccess().getWhenKeyword_4_0());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getRuleAccess().getWhenKeyword_4_0());
        c.setLinewrap(1, 1, 2).before(rulesGrammarAccess.getRuleAccess().getButKeyword_5_0());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getRuleAccess().getIfKeyword_5_2());
        c.setLinewrap(1, 1, 2).before(rulesGrammarAccess.getRuleAccess().getThenKeyword_6());
        c.setLinewrap(1, 1, 2).after(rulesGrammarAccess.getRuleAccess().getThenKeyword_6());
        c.setLinewrap(1, 1, 2).before(rulesGrammarAccess.getRuleAccess().getEndKeyword_8());

        after(c.setIndentationIncrement(), "{");
        before(c.setIndentationDecrement(), "}");
        c.setIndentationIncrement().after(rulesGrammarAccess.getRuleAccess().getWhenKeyword_4_0());
        c.setIndentationDecrement().before(rulesGrammarAccess.getRuleAccess().getButKeyword_5_0());
        c.setIndentationIncrement().after(rulesGrammarAccess.getRuleAccess().getIfKeyword_5_2());
        c.setIndentationDecrement().before(rulesGrammarAccess.getRuleAccess().getThenKeyword_6());
        c.setIndentationIncrement().after(rulesGrammarAccess.getRuleAccess().getThenKeyword_6());
        c.setIndentationDecrement().before(rulesGrammarAccess.getRuleAccess().getEndKeyword_8());

        before(c.setLinewrap(), "}");

        withinKeywordPairs(c.setNoSpace(), "(", ")");
        withinKeywordPairs(c.setNoSpace(), "[", "]");
        around(c.setNoSpace(), "=");
        around(c.setNoSpace(), ".");
        before(c.setNoSpace(), ",");

        c.setAutoLinewrap(120);

        c.setLinewrap(0, 1, 2).before(rulesGrammarAccess.getSL_COMMENTRule());
        c.setLinewrap(0, 1, 2).before(rulesGrammarAccess.getML_COMMENTRule());
        c.setLinewrap(0, 1, 1).after(rulesGrammarAccess.getML_COMMENTRule());
    }

    public void withinKeywordPairs(FormattingConfig.NoSpaceLocator locator, String leftKW, String rightKW) {
        List<Pair<Keyword, Keyword>> keywordPairs = rulesGrammarAccess.findKeywordPairs(leftKW, rightKW);
        for (Pair<Keyword, Keyword> pair : keywordPairs) {
            {
                locator.after(pair.getFirst());
                locator.before(pair.getSecond());
            }
        }
    }

    public void around(final AbstractFormattingConfig.ElementLocator locator, String... listKW) {
        List<Keyword> keywords = rulesGrammarAccess.findKeywords(listKW);
        for (Keyword keyword : keywords) {
            locator.around(keyword);
        }
    }

    public void after(final AbstractFormattingConfig.ElementLocator locator, String... listKW) {
        List<Keyword> keywords = rulesGrammarAccess.findKeywords(listKW);
        for (Keyword keyword : keywords) {
            locator.after(keyword);
        }
    }

    public void before(AbstractFormattingConfig.ElementLocator locator, String... listKW) {
        List<Keyword> keywords = rulesGrammarAccess.findKeywords(listKW);
        for (Keyword keyword : keywords) {
            locator.before(keyword);
        }
    }

    @Override
    public ITokenStream createFormatterStream(@Nullable String indent, @Nullable ITokenStream out,
            boolean preserveWhitespaces) {
        return new RuleFormattingConfigBasedStream(out, indent, getConfig(), createMatcher(), getHiddenTokenHelper(),
                preserveWhitespaces);
    }

    @Override
    public ITokenStream createFormatterStream(@Nullable EObject context, @Nullable String indent,
            @Nullable ITokenStream out, boolean preserveWhitespaces) {

        // We have no choice but to call super() and then ignore the returned ITokenStream. The reason is that
        // contextResourceURI is private, so we can't set it from here, making the call to super() required. But,
        // we don't want the returned instance because it's of the wrong type, so we have to create another one.

        super.createFormatterStream(context, indent, out, preserveWhitespaces);

        return new RuleFormattingConfigBasedStream(out, indent, getConfig(), createMatcher(), getHiddenTokenHelper(),
                preserveWhitespaces);
    }
}
