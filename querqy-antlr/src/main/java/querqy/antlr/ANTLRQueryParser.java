/**
 * 
 */
package querqy.antlr;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import querqy.antlr.parser.QueryLexer;
import querqy.antlr.parser.QueryParser;
import querqy.antlr.parser.QueryParser.QueryContext;
import querqy.model.Query;
import querqy.parser.QuerqyParser;

/**
 * @author rene
 *
 */
public class ANTLRQueryParser implements QuerqyParser {
    
    
    @Override
   public Query parse(String input) {

        char[] inputChars = input.toCharArray();
        
        QueryLexer lex = new QueryLexer(new ANTLRInputStream(inputChars, inputChars.length));
        CommonTokenStream tokens = new CommonTokenStream(lex);
        QueryParser parser = new QueryParser(tokens);
        
        QueryContext t = parser.query();
        return (Query) t.accept(new QueryTransformerVisitor(inputChars));
    }

}
