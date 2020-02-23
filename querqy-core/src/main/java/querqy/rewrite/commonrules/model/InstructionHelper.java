package querqy.rewrite.commonrules.model;

import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.SHOULD;

import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.Query;
import querqy.model.RawQuery;

import java.util.List;

/**
 *
 * @author René Kriegler, @renekrie
 *
 * Created by rene on 07/07/2017.
 */
public class InstructionHelper {

    /**
     * Currently applies mm=100%
     * @param query The query on which mm should be applied
     * @return The query with mm applied
     */
    public static BooleanQuery applyMinShouldMatchAndGeneratedToBooleanQuery(final BooleanQuery query) {

        // TODO: avoid creating new query for single clause BQ

        final List<BooleanClause> clauses = query.getClauses();

        boolean applicable = true;
        for (final BooleanClause clause : clauses) {
            if (clause instanceof BooleanQuery || clause instanceof RawQuery) {
                applicable = false;
                break;
            }
        }

        final BooleanQuery newQuery = query instanceof Query
                ? new Query(true)
                : new BooleanQuery(query.getParent(), query.getOccur(), true);

        for (final BooleanClause clause : clauses) {
            if (applicable && clause.getOccur() == SHOULD) {
                newQuery.addClause(clause.clone(newQuery, MUST, true));
            } else {
                newQuery.addClause(clause.clone(newQuery, true));
            }
        }

        return newQuery;

    }
}
