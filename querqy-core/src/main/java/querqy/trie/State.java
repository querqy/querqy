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
    /**
     * The index of the last matching char
     */
    public final int index;
    
    public State(boolean isKnown, T value, Node<T> node) {
        this(isKnown, value, node, -1);
    }
    
    public State(boolean isKnown, T value, Node<T> node, int index) {
        this.isKnown = isKnown;
        this.value = value;
        this.node = node;
        this.index = index;
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

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "State [value=" + value + ", isKnown=" + isKnown + ", index=" + index + "]";
    }


}
