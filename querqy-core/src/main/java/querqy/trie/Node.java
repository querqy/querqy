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
    T prefixValue;
    Node<T> firstChild;
    Node<T> next;
    boolean hasPrefix = false;
    
    public Node(char character, T value) {
        this.character = character;
        this.value = value;
    }
    public Node(char character) {
        this(character, null);
    }
    
    public void putPrefix(CharSequence seq, int index, T value) {
        put(seq, index, value, true);
    }
    
    public void put(CharSequence seq, int index, T value) {
        put(seq, index, value, false);
    }
    
    public void put(CharSequence seq, int index, T value, boolean isPrefix) {
        
        if (seq.charAt(index) == character) {
            
            if (index == (seq.length() - 1)) {
                
                if (isPrefix) {
                    this.prefixValue = value;
                } else {
                    this.value = value;
                }
                
                this.hasPrefix |= isPrefix;
                
            } else {
                
                if (firstChild == null) {
                    synchronized (this) {
                        if (firstChild == null) {
                            firstChild = new Node<T>(seq.charAt(index + 1));
                        }
                    }
                }
                firstChild.put(seq, index + 1, value, isPrefix);
            }
        } else {
            if (next == null) {
                synchronized (this) {
                    if (next == null) {
                        next = new Node<>(seq.charAt(index));
                    }
                }
            }
            next.put(seq, index, value, isPrefix);
        }
    }
    
    public States<T> get(CharSequence seq, int index) {
        if (seq.charAt(index) == character) {
            if (index == seq.length() - 1) {
                return new States<>(new State<T>(true, value, this, index));
                // do not add prefix match here, as we should have at least one char matching the wildcard
            } else {
                if (firstChild == null) {
                    States<T> states = new States<>(new State<T>(false, null, null));
                    if (hasPrefix) {
                        states.addPrefix(new State<>(true, prefixValue, this, seq.charAt(0) == ' ' ? index - 1 : index));
                    }
                    return states;
                } else {
                    States<T> states = firstChild.get(seq, index + 1);
                    if (hasPrefix) {
                        states.addPrefix(new State<>(true, prefixValue, this, seq.charAt(0) == ' ' ? index - 1 : index));
                    }
                    return states;
                }
                
            }
        } else {
            return (next != null) ? next.get(seq, index) : new States<>(new State<T>(false, null, null));
        }
    }
    
    public States<T> getNext(CharSequence seq, int index) {
        return (firstChild != null) ? firstChild.get(seq, index) : new States<>(new State<T>(false, null, null));
    }

}
