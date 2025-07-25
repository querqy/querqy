/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import querqy.model.Term;

/**
 * <p>A FieldBoost provides the boost factors for all Lucene queries that are created from a single Querqy query Term.</p>
 * <p>FieldBoosts must implement the equals and hashCode methods</p> 
 * 
 * @author rene
 *
 */
public interface FieldBoost {
    
    float getBoost(String fieldname, IndexReader indexReader) throws IOException;
    
    void registerTermSubQuery(TermSubQueryFactory termSubQueryFactory);
    
    String toString(String fieldname);
    
    int hashCode();
    
    boolean equals(Object obj);

}
