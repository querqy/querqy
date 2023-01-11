/**
 * 
 */
package querqy.trie;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
    
    public Node(final char character, final T value) {
        this.character = character;
        this.value = value;
    }
    public Node(final char character) {
        this(character, null);
    }
    
    public void putPrefix(final CharSequence seq, final int index, final T value) {
        put(seq, index, value, true);
    }
    
    public void put(final CharSequence seq, final int index, final T value) {
        put(seq, index, value, false);
    }
    
    public void put(final CharSequence seq, final int index, T value, final boolean isPrefix) {
        
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
    
    public States<T> get(final CharSequence seq, final int index) {
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

    public States<T> get(final char ch) {
        if (ch == character) {
            return new States<>(new State<T>(true, value, this, 0));
                // do not add prefix match here, as we should have at least one char matching the wildcard
        } else {
            return (next != null) ? next.get(ch) : new States<>(new State<T>(false, null, null));
        }
    }

    public States<T> getNext(final char ch) {
        return (firstChild != null) ? firstChild.get(ch) : new States<>(new State<T>(false, null, null));
    }

    public States<T> getNext(final CharSequence seq, final int index) {
        return (firstChild != null) ? firstChild.get(seq, index) : new States<>(new State<T>(false, null, null));
    }
    
    public ValueIterator iterator() {
        return new ValueIterator();
    }
    
    enum IterationState {THIS, THIS_PREFIX, CHILD, NEXT}
    public class ValueIterator implements Iterator<T> {
        
        IterationState iterationState = IterationState.THIS;
        
        ValueIterator childIterator = null;
        ValueIterator nextIterator = null;

        @Override
        public boolean hasNext() {
            switch (iterationState) {
            case THIS: 
                if (value != null) return true;
                iterationState = IterationState.THIS_PREFIX;
                return hasNext();
            case THIS_PREFIX:
                if (prefixValue != null) {
                    return true;
                }
                iterationState = IterationState.CHILD;
                return hasNext();
            case CHILD:
                if (childIterator == null) {
                    if (firstChild == null) {
                        iterationState = IterationState.NEXT;
                        return hasNext();
                    }
                    childIterator = firstChild.iterator();
                }
                if (childIterator.hasNext()) {
                    return true;
                }
                iterationState = IterationState.NEXT;
                return hasNext();
            case NEXT:
                if (nextIterator == null) {
                    if (next == null) {
                        return false;
                    }
                    nextIterator = next.iterator();
                }
                return nextIterator.hasNext();
            }
            return false;
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            switch (iterationState) {
            case THIS: iterationState = IterationState.THIS_PREFIX; return value;
            case THIS_PREFIX: iterationState = IterationState.CHILD; return prefixValue;
            case CHILD: return childIterator.next();
            case NEXT: return nextIterator.next();
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
            
        }
        
    }

}
