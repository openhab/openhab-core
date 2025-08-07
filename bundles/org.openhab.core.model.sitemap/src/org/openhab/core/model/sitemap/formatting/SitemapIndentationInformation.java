package org.openhab.core.model.sitemap.formatting;

import org.eclipse.xtext.formatting.IIndentationInformation;

public class SitemapIndentationInformation implements IIndentationInformation {
    @Override
    public String getIndentString() {
        return "  "; // 2 spaces
    }
}
