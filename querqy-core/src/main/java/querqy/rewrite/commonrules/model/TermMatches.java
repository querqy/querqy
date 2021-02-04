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
    
    public TermMatches(final Collection<? extends TermMatch> c) {
        super();
        for (final TermMatch match: c) {
            add(match);
        }
    }
    
    public TermMatches(final TermMatch match) {
        super();
        add(match);
    }
    
    public TermMatches(final TermMatches termMatches) {
        super();
        addAll(termMatches);
        this.replacements.putAll(termMatches.replacements);
    }

    public ComparableCharSequence getReplacement(final int position) {
        if (replacements == null) {
            throw new IndexOutOfBoundsException(Integer.toString(position));
        }
        final ComparableCharSequence replacement = replacements.get(position);
        if (replacement == null) {
            throw new IndexOutOfBoundsException(Integer.toString(position));
        }
        
        return replacement;
    }
    
    @Override
    public boolean add(final TermMatch match) {
        if (match.isPrefix) {
            updateReplacements(match);
        }
        return super.add(match);
    }
    
    @Override
    public void add(final int index, final TermMatch match) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addFirst(final TermMatch match) {
        final Map<Integer, ComparableCharSequence> newReplacements = new HashMap<>();
        newReplacements.put(1, match.getWildcardMatch());
        for (final Map.Entry<Integer, ComparableCharSequence> entry : replacements.entrySet()) {
            newReplacements.put(entry.getKey() + 1, entry.getValue());
        }
        replacements = newReplacements;
    }
    
    @Override
    public boolean addAll(final Collection<? extends TermMatch> c) {
        for (final TermMatch match: c) {
            updateReplacements(match);
        }
        return super.addAll(c);
    }
    
    protected void updateReplacements(final TermMatch match) {
        if (replacements == null) {
            replacements = new HashMap<>();
        }
        replacements.put(replacements.size() + 1, match.getWildcardMatch());
    }

    public static TermMatches empty() {
        return EmptyTermMatches.EMPTY_TERM_MATCHES;
    }

    private static class EmptyTermMatches extends TermMatches {
        private static final TermMatches EMPTY_TERM_MATCHES = new EmptyTermMatches();

        @Override
        public boolean add(final TermMatch match) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(final int index, final TermMatch match) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addFirst(final TermMatch match) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(final Collection<? extends TermMatch> c) {
            throw new UnsupportedOperationException();
        }

    }


}
