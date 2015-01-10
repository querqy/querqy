/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import querqy.ComparableCharSequence;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TermMatches extends LinkedList<TermMatch> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    Map<Integer, ComparableCharSequence> replacements;
    
    public TermMatches() {
        super();
    }
    
    public TermMatches(Collection<? extends TermMatch> c) {
        super(c);
    }
    
    public TermMatches(TermMatch match) {
        super();
        add(match);
    }
    
    public ComparableCharSequence getReplacement(int position) {
        if (replacements == null) {
            throw new IndexOutOfBoundsException(Integer.toString(position));
        }
        ComparableCharSequence replacement = replacements.get(position);
        if (replacement == null) {
            throw new IndexOutOfBoundsException(Integer.toString(position));
        }
        
        return replacement;
    }
    
    @Override
    public boolean add(TermMatch match) {
        updateReplacements(match);
        return super.add(match);
    }
    
    @Override
    public void add(int index, TermMatch match) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addFirst(TermMatch match) {
        Map<Integer, ComparableCharSequence> newReplacements = new HashMap<>();
        newReplacements.put(1, match.getWildcardMatch());
        for (Map.Entry<Integer, ComparableCharSequence> entry : replacements.entrySet()) {
            newReplacements.put(entry.getKey() + 1, entry.getValue());
        }
        replacements = newReplacements;
    }
    
    @Override
    public boolean addAll(Collection<? extends TermMatch> c) {
        for (TermMatch match: c) {
            updateReplacements(match);
        }
        return super.addAll(c);
    }
    
    protected void updateReplacements(TermMatch match) {
        if (replacements == null) {
            replacements = new HashMap<Integer, ComparableCharSequence>();
        }
        replacements.put(replacements.size() + 1, match.getWildcardMatch());
    }

}
