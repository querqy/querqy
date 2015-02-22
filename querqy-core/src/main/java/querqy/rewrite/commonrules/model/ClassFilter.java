/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class filters the elements of an {@link Iterable} by class. 
 * 
 * @deprecated This class will be removed and can be replaced by a Lambda expression when Querqy moves to Java 8
 * @author René Kriegler, @renekrie
 *
 */
public class ClassFilter<T, F extends T> implements Iterable<F> {
    
    final Iterable<T> delegateIterable;
    final Class<F> requiredClass;
    
    public ClassFilter(Iterable<T> delegateIterable, Class<F> requiredClass) {
        this.delegateIterable = delegateIterable;
        this.requiredClass = requiredClass;
    }

    @Override
    public Iterator<F> iterator() {
        return new ClassFilterIterator(delegateIterable.iterator(), requiredClass);
    }
    
    
    class ClassFilterIterator implements Iterator<F> {
        
        final Iterator<T> delegate;
        final Class<F> requiredClass;
        F nextElement = null;
        boolean nextAvail = false;
        
        public ClassFilterIterator(Iterator<T> delegate, Class<F> requiredClass) {
            this.delegate = delegate;
            this.requiredClass = requiredClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean hasNext() {
            if (nextAvail) {
                return true;
            }
            while (delegate.hasNext()) {
                T element = delegate.next();
                if (element == null || requiredClass.equals(element.getClass())) {
                    nextElement = (F) element;
                    nextAvail = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public F next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            nextAvail = false;
            return nextElement;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
            
        }
        
    }

}
