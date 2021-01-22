/**
 * 
 */
package querqy.model;

import lombok.EqualsAndHashCode;

/**
 * @author René Kriegler, @renekrie
 *
 */
@EqualsAndHashCode
public class BoostQuery {

   final QuerqyQuery<?> query;
   final float boost;

   public BoostQuery(QuerqyQuery<?> query, float boost) {

      this.boost = boost;
      this.query = query;

   }

   @Override
   public String toString() {
      return "BoostQuery [query=" + query + ", boost=" + boost + "]";
   }

   public float getBoost() {
      return boost;
   }

   public QuerqyQuery<?> getQuery() {
      return query;
   }

}
