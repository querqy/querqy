/**
 *
 */
package querqy.model;

import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.LinkedList;

/**
 *
 *
 * @author René Kriegler, @renekrie
 *
 *         Note: this class does not synchronize access to filterQueries and
 *         boostQueries.
 *         TODO: Should return empty lists instead of null
 *
 */
@EqualsAndHashCode
public class ExpandedQuery {

   private QuerqyQuery<?> userQuery;
   protected Collection<QuerqyQuery<?>> filterQueries;
   protected Collection<BoostQuery> boostUpQueries;
   protected Collection<BoostQuery> boostDownQueries;
   protected Collection<BoostQuery> multiplicativeBoostQueries;

   public ExpandedQuery(QuerqyQuery<?> userQuery) {
      setUserQuery(userQuery);
   }

   public ExpandedQuery(QuerqyQuery<?> userQuery,
                        Collection<QuerqyQuery<?>> filterQueries,
                        Collection<BoostQuery> boostUpQueries,
                        Collection<BoostQuery> boostDownQueries,
                        Collection<BoostQuery> multiplicativeBoostQueries) {
       setUserQuery(userQuery);
       filterQueries.forEach(this::addFilterQuery);
       boostUpQueries.forEach(this::addBoostUpQuery);
       boostDownQueries.forEach(this::addBoostDownQuery);
       multiplicativeBoostQueries.forEach(this::addMultiplicativeBoostQuery);
   }

   public QuerqyQuery<?> getUserQuery() {
      return userQuery;
   }

   public final void setUserQuery(final QuerqyQuery<?> userQuery) {
      if (userQuery == null) {
         throw new IllegalArgumentException("userQuery required");
      }
      this.userQuery = userQuery;
   }

   public Collection<QuerqyQuery<?>> getFilterQueries() {
      return filterQueries;
   }

   public void addFilterQuery(final QuerqyQuery<?> filterQuery) {
      if (filterQueries == null) {
         filterQueries = new LinkedList<>();
      }
      filterQueries.add(filterQuery);
   }

   public Collection<BoostQuery> getBoostUpQueries() {
      return boostUpQueries;
   }

   public void addBoostUpQuery(final BoostQuery boostUpQuery) {
      if (boostUpQueries == null) {
         boostUpQueries = new LinkedList<>();
      }
      boostUpQueries.add(boostUpQuery);
   }

   public Collection<BoostQuery> getBoostDownQueries() {
      return boostDownQueries;
   }

   public void addBoostDownQuery(final BoostQuery boostDownQuery) {
      if (boostDownQueries == null) {
         boostDownQueries = new LinkedList<>();
      }
      boostDownQueries.add(boostDownQuery);
   }

   public Collection<BoostQuery> getMultiplicativeBoostQueries() {
      return multiplicativeBoostQueries;
   }

   public void addMultiplicativeBoostQuery(final BoostQuery multiplicativeBoostQuery) {
      if (multiplicativeBoostQueries == null) {
         multiplicativeBoostQueries = new LinkedList<>();
      }
      multiplicativeBoostQueries.add(multiplicativeBoostQuery);
   }

}
