package querqy.rewrite.lookup;

import querqy.model.Term;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.model.Sequence;

import java.util.List;
import java.util.Optional;

public interface AutomatonWrapper<StateT, ValueT> {

    Optional<StateT> evaluateTerm(final Term term);
    Optional<StateT> evaluateNextTerm(final Sequence<StateT> sequence, final Term term);

    List<Match<ValueT>> getMatches();

}
