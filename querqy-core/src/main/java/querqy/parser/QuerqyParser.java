package querqy.parser;

import querqy.model.Query;

/**
 * Transforms a query string into the {@linkplain Query} model.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public interface QuerqyParser {

   /**
    * Accepts a query input and transforms it into a {@linkplain Query}.
    */
   Query parse(String input);
   
}
