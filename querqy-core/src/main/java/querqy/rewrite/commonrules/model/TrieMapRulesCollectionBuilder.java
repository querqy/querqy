/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import querqy.ComparableCharSequence;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollectionBuilder implements RulesCollectionBuilder {
    
    
    Map<CharSequence, List<Instructions>> rules = new HashMap<>();
    
    final boolean ignoreCase;
    
    public TrieMapRulesCollectionBuilder(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#addRule(querqy.rewrite.commonrules.model.Input, querqy.rewrite.commonrules.model.Instructions)
     */
    @Override
    public void addRule(Input input, Instructions instructions) {
        for (ComparableCharSequence seq : input.getInputSequences(ignoreCase)) {
            
            List<Instructions> instructionsList = rules.get(seq);
            if (instructionsList == null) {
                instructionsList = new LinkedList<>();
                rules.put(seq, instructionsList);
            }
            
            instructionsList.add(instructions);
            
       }

    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#build()
     */
    @Override
    public RulesCollection build() {
        TrieMap<List<Instructions>> map = new TrieMap<>();
        for (Map.Entry<CharSequence, List<Instructions>> entry: rules.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        
       
        return new TrieMapRulesCollection(map, ignoreCase);
    }

}
