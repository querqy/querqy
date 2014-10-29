/**
 * 
 */
package querqy.parser;

import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;

/**
 * A simple QuerqyParser that breaks a query string into clauses at whitespace.
 * Clauses are either terms or terms with a boolean operator prefix
 * (&quot;+&quot; &quot;-&quot;).
 * 
 * @author René Kriegler, @renekrie
 * 
 */
public class WhiteSpaceQuerqyParser implements QuerqyParser {

    enum Status {
        DEFAULT, OP, TERM
    }

    /*
     * (non-Javadoc)
     * 
     * @see querqy.parser.QuerqyParser#parse(java.lang.String)
     */
    @Override
    public Query parse(String input) {

        Query query = new Query();

        if (input.length() > 0) {

            int start = -1;
            Status status = Status.DEFAULT;
            Occur occur = Occur.SHOULD;

            for (int idx = 0, len = input.length(); idx < len; idx++) {

                char ch = input.charAt(idx);

                switch (ch) {
                case ' ':
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                    switch (status) {
                    case TERM: {
                        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(
                                query, occur, false);
                        query.addClause(dmq);
                        Term term = new Term(dmq, input.substring(start, idx));
                        dmq.addClause(term);
                        status = Status.DEFAULT;
                        occur = Occur.SHOULD;
                    }
                        break;

                    case OP: {

                        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(
                                query, Occur.SHOULD, false);
                        query.addClause(dmq);
                        Term term = new Term(dmq, occur == Occur.MUST ? "+"
                                : "-");
                        dmq.addClause(term);
                        status = Status.DEFAULT;
                        occur = Occur.SHOULD;
                    }
                        break;

                    case DEFAULT:
                        break;
                    }

                    break;

                case '-':
                case '+':
                    switch (status) {
                    case DEFAULT:
                        status = Status.OP;
                        occur = ch == '+' ? Occur.MUST : Occur.MUST_NOT;
                        break;
                    case OP:
                        status = Status.TERM;
                        start = idx;
                        break;
                    default:
                        break;
                    }
                    break;

                default:
                    if (status != Status.TERM) {
                        start = idx;
                        status = Status.TERM;
                    }

                }
            }
            
            switch (status) {
            case TERM: {
                DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, occur,
                        false);
                query.addClause(dmq);
                Term term = new Term(dmq, input.substring(start));
                dmq.addClause(term);
            }
            break;
            case OP: {
                DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(
                        query, Occur.SHOULD, false);
                query.addClause(dmq);
                Term term = new Term(dmq, occur == Occur.MUST ? "+"
                        : "-");
                dmq.addClause(term);
            }
            break;
            default:
            }

        }

        return query;
    }

}
