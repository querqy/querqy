package querqy.parser;

import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.SHOULD;

import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;

/**
 * <p>A {@link QuerqyParser} similar to {@link WhiteSpaceQuerqyParser}. In addition, the query string can
 * include field names, which must be prefixed to the value using a colon:
 * <pre>
 *     field:value
 * </pre>
 * </p>
 *
 * @author René Kriegler, @renekrie
 *
 */
public class FieldAwareWhiteSpaceQuerqyParser implements QuerqyParser {


    @Override
    public Query parse(final String input) {

        final Query query = new Query();

        final int len = input.length();
        int start = -1;
        while (start < len) {
            int pos = start + 1;
            while (pos < len && " \t\n\r\f".indexOf(input.charAt(pos)) > -1 ) {
                pos++;
            }
            if (pos == len) {
                break;
            } else {
                start = pos;
            }


            Clause.Occur occur = SHOULD;
            int colon = -1;

            switch (input.charAt(start)) {
                case '-': occur = Clause.Occur.MUST_NOT; start++; break;
                case '+': occur = Clause.Occur.MUST; start++; break;
                default: break;
            }

            pos = start;

            while (pos < len && " \t\n\r\f".indexOf(input.charAt(pos)) < 0) {
                if (input.charAt(pos) == ':' && colon == -1) {
                    colon = pos;
                }
                pos++;
            }

            int end = pos;

            if (pos == start && occur != SHOULD) {
                // the token consists of the operator only, interpret operator as text
                final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, SHOULD, false);
                query.addClause(dmq);
                final Term term = new Term(dmq, occur == MUST ? "+" : "-");
                dmq.addClause(term);

            } else {

                final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, occur, false);
                query.addClause(dmq);
                final Term term;
                if (colon > start && colon < end - 1) {
                    term = new Term(dmq, input.substring(start, colon), input.substring(colon + 1, end));
                } else {
                    term = new Term(dmq, input.substring(start, end));
                }

                dmq.addClause(term);

            }

            start = end;


        }

        return query;
    }
}
