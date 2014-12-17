/**
 * 
 */
package querqy.trie;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class Node<T> {
    
    public final char character;
    T value;
    Node<T> firstChild;
    Node<T> next;
    
    public Node(char character, T value) {
        this.character = character;
        this.value = value;
    }
    public Node(char character) {
        this(character, null);
    }
    
    public void put(CharSequence seq, int index, T value) {
        if (seq.charAt(index) == character) {
            if (index == (seq.length() - 1)) {
                this.value = value;
            } else {
                
                if (firstChild == null) {
                    synchronized (this) {
                        if (firstChild == null) {
                            firstChild = new Node<T>(seq.charAt(index + 1));
                        }
                    }
                }
                
                firstChild.put(seq, index + 1, value);
            }
        } else {
            if (next == null) {
                synchronized (this) {
                    if (next == null) {
                        next = new Node<>(seq.charAt(index));
                    }
                }
            }
            next.put(seq, index, value);
        }
    }
    
    public State<T> get(CharSequence seq, int index) {
        if (seq.charAt(index) == character) {
            if (index == seq.length() - 1) {
                return new State<T>(true, value, this);
            } else {
                return (firstChild != null) ? firstChild.get(seq, index + 1) : new State<T>(false, null, null);
            }
        } else {
            return (next != null) ? next.get(seq, index) : new State<T>(false, null, null);
        }
    }
    
    public State<T> getNext(CharSequence seq, int index) {
        return (firstChild != null) ? firstChild.get(seq, index) : new State<T>(false, null, null);
    }

}
