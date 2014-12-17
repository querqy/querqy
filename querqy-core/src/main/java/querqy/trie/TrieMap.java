/**
 * 
 */
package querqy.trie;


/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMap<T> {
    
    Node<T> root;
    
    public void put(CharSequence seq, T value) {
        int length = seq.length();
        if (length == 0) {
            throw new IllegalArgumentException("Must not put empty sequence into trie");
        }
        if (root == null) {
            synchronized (this) {
                if (root == null) {
                    root = new Node<T>(seq.charAt(0));
                }
            }
        }
        
        root.put(seq, 0, value);
    }
    
    public State<T> get(CharSequence seq) {
        if (seq.length() == 0) {
            return new State<T>(false, null, null);
        }
        return (root == null) ? new State<T>(false, null, null) : root.get(seq, 0);
    }
    

    public State<T> get(CharSequence seq, State<T> stateInfo) {
        if (!stateInfo.isKnown()) {
            throw new IllegalArgumentException("Known state expected");
        }
        if (seq.length() == 0) {
            return new State<T>(false, null, null);
        }
        return stateInfo.node.getNext(seq, 0);
    }

}
