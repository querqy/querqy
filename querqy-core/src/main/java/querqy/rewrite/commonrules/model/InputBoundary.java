/**
 * 
 */
package querqy.rewrite.commonrules.model;

import querqy.model.InputSequenceElement;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class InputBoundary implements InputSequenceElement {
    
    public enum Type {LEFT, RIGHT}
    public final Type type; 

    public InputBoundary(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Boundary type must not be null");
        }
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 47;
        int result = prime + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        InputBoundary other = (InputBoundary) obj;
        if (type != other.type)
            return false;
        return true;
    }


    
}
