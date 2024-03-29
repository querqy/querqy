/**
 *
 */
package querqy.rewrite.commonrules.model;

import java.util.*;

import querqy.ComparableCharSequence;
import querqy.model.*;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * @author René Kriegler, @renekrie
 */
public class BoostInstruction implements Instruction {

    public enum BoostDirection {
        UP, DOWN
    }

    public enum BoostMethod {
        ADDITIVE, MULTIPLICATIVE
    }

    final QuerqyQuery<?> query;
    final BoostDirection direction;
    final BoostMethod boostMethod;
    final boolean hasPlaceHolder;
    final float boost;

    private final InstructionDescription instructionDescription;

    public BoostInstruction(final QuerqyQuery<?> query, final BoostDirection direction, final BoostMethod boostMethod,
                            final float boost) {
        this(query, direction, boostMethod, boost, InstructionDescription.empty());
    }

    public BoostInstruction(final QuerqyQuery<?> query, final BoostDirection direction, final BoostMethod boostMethod,
                            final float boost, final InstructionDescription instructionDescription) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }

        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }

        if (boostMethod == null) {
            throw new IllegalArgumentException("boost method must not be null");
        }

        if (query instanceof BooleanQuery) {
            hasPlaceHolder = (query instanceof Query)
                    && new ToPlaceHolderTermRewriter().rewritePlaceHolders((Query) query);
            this.query = InstructionHelper.applyMinShouldMatchAndGeneratedToBooleanQuery((BooleanQuery) query);
        } else {
            hasPlaceHolder = false;
            this.query = query;
        }

        this.direction = direction;
        this.boostMethod = boostMethod;
        this.boost = boost;
        this.instructionDescription = instructionDescription;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence,
     *                           querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery,
     *                           java.util.Map)
     */
    @Override
    public void apply(final TermMatches termMatches, final ExpandedQuery expandedQuery,
                      final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        if (boost == 0) {
            // zero boost = no boost
            return;
        }

        // TODO: we might not need to clone here, if we already cloned all queries in the constructor
        final QuerqyQuery<?> q = (hasPlaceHolder)
                ? new CloneAndReplacePlaceHolderRewriter(termMatches).cloneAndReplace(query)
                : query.clone(null, true);

        switch(boostMethod) {
            case ADDITIVE:
                final BoostQuery boostQuery = new BoostQuery(q, boost);
                switch (direction) {
                    case DOWN:
                        expandedQuery.addBoostDownQuery(boostQuery);
                        break;
                    case UP:
                        expandedQuery.addBoostUpQuery(boostQuery);
                        break;
                }
                break;
            case MULTIPLICATIVE:
                // in multiplicative case boost are interpreted as UP(factor) = score * factor and DOWN(factor) = score * 1/factor
                switch (direction) {
                    case DOWN:
                        expandedQuery.addMultiplicativeBoostQuery(new BoostQuery(q, 1 / boost));
                        break;
                    case UP:
                        expandedQuery.addMultiplicativeBoostQuery(new BoostQuery(q, boost));
                        break;
                }
                break;
        }

    }

    @Override
    public Set<Term> getGenerableTerms() {
        return (query instanceof Query)
                ? TermsCollector.collectGenerableTerms((Query) query)
                : QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    @Override
    public InstructionDescription getInstructionDescription() {
        return instructionDescription;
    }

    public BoostDirection getDirection() {
        return direction;
    }

    public BoostMethod getBoostMethod() {
        return boostMethod;
    }

    public boolean hasPlaceHolderInBoostQuery() {
        return hasPlaceHolder;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(boost);
        result = prime * result
                + ((direction == null) ? 0 : direction.hashCode());
        result = prime * result
                + ((boostMethod == null) ? 0 : boostMethod.hashCode());
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BoostInstruction other = (BoostInstruction) obj;
        if (Float.floatToIntBits(boost) != Float.floatToIntBits(other.boost))
            return false;
        if (direction != other.direction)
            return false;
        if (boostMethod != other.boostMethod)
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        } else if (!query.equals(other.query))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BoostInstruction [query=" + query + ", direction=" + direction
                + ", boostMethod=" + boostMethod + ", boost=" + boost + "]";
    }

    class CloneAndReplacePlaceHolderRewriter extends AbstractNodeVisitor<Node> {

        final TermMatches termMatches;
        final LinkedList<Node> newParentStack = new LinkedList<>();

        CloneAndReplacePlaceHolderRewriter(TermMatches termMatches) {
            this.termMatches = termMatches;
        }

        public QuerqyQuery<?> cloneAndReplace(QuerqyQuery<?> querqyQuery) {
            if (querqyQuery instanceof Query) {
                return (QuerqyQuery<?>) visit((Query) query);
            } else {
                return (QuerqyQuery) querqyQuery.accept(this);
            }
        }

        protected Node getNewParent() {
            return newParentStack.isEmpty() ? null : newParentStack.getLast();
        }


        @Override
        public Node visit(final Query query) {
            Query clone = new Query();
            newParentStack.add(clone);
            for (final BooleanClause clause : query.getClauses()) {
                clone.addClause((BooleanClause) clause.accept(this));
            }
            newParentStack.removeLast();
            return clone;
        }

        @Override
        public DisjunctionMaxQuery visit(final DisjunctionMaxQuery disjunctionMaxQuery) {


            final DisjunctionMaxQuery newDMQ
                    = new DisjunctionMaxQuery((BooleanQuery) getNewParent(), disjunctionMaxQuery.occur, true);
            newParentStack.add(newDMQ);

            for (final DisjunctionMaxClause clause : disjunctionMaxQuery.getClauses()) {
                newDMQ.addClause((DisjunctionMaxClause) clause.accept(this));
            }

            newParentStack.removeLast();

            return newDMQ;

        }

        @Override
        public Node visit(final Term term) {
            final ComparableCharSequence value = term.getValue();
            if (value instanceof querqy.rewrite.commonrules.model.Term) {
                querqy.rewrite.commonrules.model.Term termValue = (querqy.rewrite.commonrules.model.Term) value;
                final ComparableCharSequence newValue = termValue.fillPlaceholders(termMatches);
                return new Term((DisjunctionMaxQuery) getNewParent(), term.getField(), newValue, true);
            } else {
                return term.clone((DisjunctionMaxQuery) getNewParent(), true);
            }
        }

        @Override
        public Node visit(final BooleanQuery booleanQuery) {

            final BooleanQuery newBQ
                    = new BooleanQuery((BooleanParent) getNewParent(), booleanQuery.occur, true);
            newParentStack.add(newBQ);

            for (final BooleanClause clause : booleanQuery.getClauses()) {
                newBQ.addClause((BooleanClause) clause.accept(this));
            }
            newParentStack.removeLast();

            return newBQ;

        }

        @Override
        public Node visit(final RawQuery rawQuery) {
            return rawQuery.clone((BooleanParent) getNewParent(), true);
        }

    }

    class ToPlaceHolderTermRewriter extends AbstractNodeVisitor<Node> {

        private boolean hasPlaceHolder = false;


        public boolean rewritePlaceHolders(final Query query) {
            visit(query);
            return hasPlaceHolder;
        }


        @Override
        public Node visit(final Query query) {
            super.visit(query);
            return query;
        }

        @Override
        public DisjunctionMaxQuery visit(final DisjunctionMaxQuery disjunctionMaxQuery) {

            boolean hasPlaceHolderChild = false;

            final List<DisjunctionMaxClause> oldClauses = disjunctionMaxQuery.getClauses();

            final List<DisjunctionMaxClause> newClauses = new ArrayList<>(oldClauses.size());

            for (final DisjunctionMaxClause clause : oldClauses) {

                final DisjunctionMaxClause mayBeRewritten = (DisjunctionMaxClause) clause.accept(this);
                newClauses.add(mayBeRewritten);
                hasPlaceHolderChild |= (mayBeRewritten != clause);
            }

            if (hasPlaceHolderChild) {

                for (final DisjunctionMaxClause clause : oldClauses) {
                    disjunctionMaxQuery.removeClause(clause);
                }

                for (final DisjunctionMaxClause clause : newClauses) {
                    disjunctionMaxQuery.addClause(clause);
                }

                hasPlaceHolder = true;
            }


            return disjunctionMaxQuery;
        }

        @Override
        public Node visit(final Term term) {
            final String value = term.getValue().toString();
            final int pos = value.indexOf('$');
            if ((pos < 0) || (pos == value.length() - 1) || !Character.isDigit(value.charAt(pos + 1))) {
                return term;
            }
            final querqy.rewrite.commonrules.model.Term charSequence
                    = new querqy.rewrite.commonrules.model.Term(
                    value.toCharArray(),
                    0,
                    value.length(),
                    term.getField() == null ? null : Collections.singletonList(term.getField()));

            return new Term(term.getParent(), term.getField(), charSequence, true);
        }

        @Override
        public Node visit(final BooleanQuery booleanQuery) {
            super.visit(booleanQuery);
            return booleanQuery;
        }

        @Override
        public Node visit(final RawQuery rawQuery) {
            super.visit(rawQuery);
            return rawQuery;
        }

    }
}
