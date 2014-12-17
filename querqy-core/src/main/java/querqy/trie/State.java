/**
 * 
 */
package querqy.trie;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class State<T> {
    
    public final T value;
    public final boolean isKnown;
    public final Node<T> node;
    
    public State(boolean isKnown, T value, Node<T> entry) {
        this.isKnown = isKnown;
        this.value = value;
        this.node = entry;
    }
    
    public boolean isKnown() {
        return isKnown;
    }
    
    public boolean isFinal() {
        return isKnown() && value != null;
    }

    public T getValue() {
        return value;
    }
    
    
}
