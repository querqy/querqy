/**
 * 
 */
package querqy.rewrite.commonrules.model;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class PlaceHolder {
    
    public final int start;
    public final int length;
    public final int ref;
    
    public PlaceHolder(int start, int length, int ref) {
        this.start = start;
        this.length = length;
        this.ref = ref;
    }
    
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + length;
        result = prime * result + ref;
        result = prime * result + start;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlaceHolder other = (PlaceHolder) obj;
        if (length != other.length)
            return false;
        if (ref != other.ref)
            return false;
        if (start != other.start)
            return false;
        return true;
    }



    @Override
    public String toString() {
        return "PlaceHolder [start=" + start + ", length=" + length + ", ref="
                + ref + "]";
    }

}
