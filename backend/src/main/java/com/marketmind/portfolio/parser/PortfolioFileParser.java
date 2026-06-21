package com.marketmind.portfolio.parser;

import java.io.InputStream;

public interface PortfolioFileParser {

    PortfolioParseResult parse(InputStream input);
}
