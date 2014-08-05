package querqy.antlr.rewrite.commonrules;

import java.util.Arrays;

import querqy.AbstractQueryTest;
import querqy.antlr.ANTLRQueryParser;
import querqy.model.Query;
import querqy.rewrite.commonrules.model.Term;

public abstract class AbstractCommonRulesTest extends AbstractQueryTest {
    
    protected Query makeQuery(String input) {
        return new ANTLRQueryParser().parse(input);
    }

    protected Term mkTerm(String s) {
        return new Term(s.toCharArray(), 0, s.length(), null);
    }
    
    protected Term mkTerm(String s, String...fieldName) {
        return new Term(s.toCharArray(), 0, s.length(), Arrays.asList(fieldName));
    }


}
