/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public Query parse(final String input) {
        return parseString(input);
    }

    public static Query parseString(final String input) {

        final Query query = new Query();

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
