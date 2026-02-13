/**
 * 
 */
package querqy.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMap<T> implements Iterable<T> {
    
    Node<T> root;
    
    public void put(final CharSequence seq, final T value) {
        final int length = seq.length();
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
    
    public Iterator<T> iterator() {
        if (root == null) {
            return new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public T next() {
                    throw new NoSuchElementException();
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return root.iterator();
        }
    }
    
    public void putPrefix(final CharSequence seq, final T value) {
        final int length = seq.length();
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
        
        root.putPrefix(seq, 0, value);
    }
    
    public States<T> get(final CharSequence seq) {
        if (seq.length() == 0) {
            return new States<>(new State<T>(false, null, null));
        }
        return (root == null) ? new States<>(new State<T>(false, null, null)) : root.get(seq, 0);
    }

    public States<T> get(final CharSequence seq, final State<T> stateInfo) {
        if (!stateInfo.isKnown()) {
            throw new IllegalArgumentException("Known state expected");
        }
        if (seq.length() == 0) {
            return new States<>(new State<T>(false, null, null));
        }
        return stateInfo.node.getNext(seq, 0);
    }

    public List<T> collectPartialMatchValues(final CharSequence seq) {
        if (seq.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        Node<T> current = root;
        int pos = 0;
        final int len = seq.length();
        while (pos < len && current != null) {
            final char ch = seq.charAt(pos);
            if (current.character == ch) {
                if (current.value != null) {
                    result.add(current.value);
                }
                current = current.firstChild;
                pos++;
            } else {
                current = current.next;
            }
        }
        return result;

    }

}
