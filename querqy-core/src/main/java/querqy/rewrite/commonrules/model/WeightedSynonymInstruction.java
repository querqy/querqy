package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.ComparableCharSequence;
import querqy.model.BoostedTerm;
import querqy.model.DisjunctionMaxQuery;

public class WeightedSynonymInstruction extends SynonymInstruction {

    private final float boost;

    public WeightedSynonymInstruction(final List<querqy.rewrite.commonrules.model.Term> synonym, float boost) {
        super(synonym);
        this.boost = boost;
    }
    
    protected void addSynonymTermToDisjunctionMaxQuery(final DisjunctionMaxQuery dmq,
                                                       final querqy.rewrite.commonrules.model.Term synTerm,
                                                       final TermMatches termMatches) {
        final List<String> fieldNames = synTerm.getFieldNames();
        final ComparableCharSequence charSequence = synTerm.fillPlaceholders(termMatches);
        if (fieldNames == null || fieldNames.isEmpty()) {
            dmq.addClause(new BoostedTerm(dmq, charSequence, boost));
        } else {
            for (final String fieldName: fieldNames) {
                dmq.addClause(new BoostedTerm(dmq, fieldName, charSequence, boost));
            }
        }
    }
}
