/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

import querqy.model.Input;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorFactory;
import querqy.rewrite.rules.rule.Rule;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollectionBuilder implements RulesCollectionBuilder {
    
    final TrieMap<InstructionsSupplier> map = new TrieMap<>();

    // we keep this just for the deprecated build() method
    @Deprecated
    private final LookupPreprocessor lookupPreprocessor;
    private final InputSequenceNormalizer inputSequenceNormalizer;
    
    public TrieMapRulesCollectionBuilder(boolean ignoreCase) {
        this(ignoreCase ? LookupPreprocessorFactory.lowercase() : LookupPreprocessorFactory.identity());
    }

    public TrieMapRulesCollectionBuilder(final LookupPreprocessor lookupPreprocessor) {
        inputSequenceNormalizer = new InputSequenceNormalizer(lookupPreprocessor);
        this.lookupPreprocessor = lookupPreprocessor;
    }

    @Override
    public void addRule(final Input.SimpleInput input, final Instructions instructions) {
        addOrMergeInstructionsSupplier(input, new InstructionsSupplier(instructions));
    }

    @Override
    public void addRule(final Input.SimpleInput input, final BooleanInputLiteral literal) {
        addOrMergeInstructionsSupplier(input, new InstructionsSupplier(literal));
    }

    @Override
    public void addRule(Rule rule) {
        addOrMergeInstructionsSupplier(rule.getInput(), rule.getInstructionsSupplier());
    }

    public void addOrMergeInstructionsSupplier(final Input.SimpleInput input,
                                               final InstructionsSupplier instructionsSupplier) {

        final List<CharSequence> seqs = inputSequenceNormalizer.getNormalizedInputSequences(input);
        final List<Term> inputTerms = input.getInputTerms();

        final boolean isPrefix = (!inputTerms.isEmpty()) &&  inputTerms.get(inputTerms.size() -1) instanceof PrefixTerm;
        for (final CharSequence seq : seqs) {
                
            final States<InstructionsSupplier> states = map.get(seq);
                
            if (isPrefix) {
                    
                boolean added = false;

                final List<State<InstructionsSupplier>> prefixes = states.getPrefixes();
                    
                if (prefixes != null) {
                    for (final State<InstructionsSupplier> state: prefixes) {
                        if (state.isFinal() && state.index == (seq.length() - 1) && state.value != null) {
                            state.value.merge(instructionsSupplier);
                            added = true;
                            break;
                        }
                            
                    }
                }
                    
                if (!added) {
                    map.putPrefix(seq, instructionsSupplier);
                }
            } else {
                final State<InstructionsSupplier> state = states.getStateForCompleteSequence();
                if (state.value != null) {
                    state.value.merge(instructionsSupplier);
                } else {
                    map.put(seq, instructionsSupplier);
                }
            }
                
        }

    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#build()
     */
    @Override
    public RulesCollection build() {
        return new TrieMapRulesCollection(map, lookupPreprocessor);
    }

    @Override
    public TrieMap<InstructionsSupplier> getTrieMap() {
        return map;
    }

}
