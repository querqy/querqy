package querqy.parser;

import querqy.model.Query;

/**
 * Transforms a query string into the {@linkplain Query} model.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 * @author René Kriegler, @renekrie
 */
public interface QuerqyParser {

    /**
     * Accepts a query input and transforms it into a {@linkplain Query}.
     * @param input The input string, must not be null.
     * @return The query parsed from the input
     */
    Query parse(String input);

}
