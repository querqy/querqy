/**
 * 
 */
package querqy.trie;

import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class States<T> {
    
    private List<State<T>> prefixes = null;
    private final State<T> completeSequence;
    
    public States(final State<T> completeSequence) {
        this.completeSequence = completeSequence;
    }
    
    public void addPrefix(final State<T> prefix) {
        if (prefixes == null) {
            prefixes = new LinkedList<>();
        }
        prefixes.add(prefix);
    }
    
    public State<T> getStateForCompleteSequence() {
        return completeSequence;
    }

    public List<State<T>> getPrefixes() {
        return prefixes;
    }

    @Override
    public String toString() {
        return "States [prefixes=" + prefixes + ", completeSequence="
                + completeSequence + "]";
    }
    
    
}
