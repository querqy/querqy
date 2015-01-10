/**
 * 
 */
package querqy.rewrite.commonrules.model;

import querqy.ComparableCharSequence;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TermMatch {
    
    final querqy.model.Term queryTerm;
    final boolean isPrefix;
    final ComparableCharSequence wildcardMatch;
    //final Term ruleInputTerm;
    
    public TermMatch(querqy.model.Term queryTerm) {
        this(queryTerm, false, null);
    }
    
    public TermMatch(querqy.model.Term queryTerm, boolean isPrefix, ComparableCharSequence wildcardMatch) {
        if (isPrefix) {
            if ((wildcardMatch == null) || (wildcardMatch.length() == 0)) {
                throw new IllegalArgumentException("Need a wildcard match if isPrefix for " + queryTerm.toString());
            }
        }
        this.queryTerm = queryTerm;
        this.isPrefix = isPrefix;
        this.wildcardMatch = wildcardMatch;
    }

    public querqy.model.Term getQueryTerm() {
        return queryTerm;
    }

    public boolean isPrefix() {
        return isPrefix;
    }

    public ComparableCharSequence getWildcardMatch() {
        return wildcardMatch;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isPrefix ? 1231 : 1237);
        result = prime * result
                + ((queryTerm == null) ? 0 : queryTerm.hashCode());
        result = prime * result
                + ((wildcardMatch == null) ? 0 : wildcardMatch.hashCode());
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
        TermMatch other = (TermMatch) obj;
        if (isPrefix != other.isPrefix)
            return false;
        if (queryTerm == null) {
            if (other.queryTerm != null)
                return false;
        } else if (!queryTerm.equals(other.queryTerm))
            return false;
        if (wildcardMatch == null) {
            if (other.wildcardMatch != null)
                return false;
        } else if (!wildcardMatch.equals(other.wildcardMatch))
            return false;
        return true;
    }

   
    
    

}
