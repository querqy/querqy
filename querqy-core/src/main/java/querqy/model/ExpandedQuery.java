/**
 *
 */
package querqy.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 * <p>
 * Note: this class does not synchronize access to filterQueries and
 * boostQueries.
 */
public class ExpandedQuery {

    private QuerqyQuery<?> userQuery;
    protected Collection<QuerqyQuery<?>> filterQueries;
    protected Collection<BoostQuery> boostUpQueries;
    protected Collection<BoostQuery> boostDownQueries;
    protected Criterion criterion;
    private List<String> appliedRuleIdList;

    public ExpandedQuery(QuerqyQuery<?> userQuery) {
        setUserQuery(userQuery);
        criterion = new Criterion();
        appliedRuleIdList = new ArrayList<>();
    }

    public QuerqyQuery<?> getUserQuery() {
        return userQuery;
    }

    public final void setUserQuery(QuerqyQuery<?> userQuery) {
        if (userQuery == null) {
            throw new IllegalArgumentException("userQuery required");
        }
        this.userQuery = userQuery;
    }

    public Collection<QuerqyQuery<?>> getFilterQueries() {
        return filterQueries;
    }

    public void addFilterQuery(QuerqyQuery<?> filterQuery) {
        if (filterQueries == null) {
            filterQueries = new LinkedList<>();
        }
        filterQueries.add(filterQuery);
    }

    public Collection<BoostQuery> getBoostUpQueries() {
        return boostUpQueries;
    }

    public void addBoostUpQuery(BoostQuery boostUpQuery) {
        if (boostUpQueries == null) {
            boostUpQueries = new LinkedList<>();
        }
        boostUpQueries.add(boostUpQuery);
    }

    public Collection<BoostQuery> getBoostDownQueries() {
        return boostDownQueries;
    }

    public void addBoostDownQuery(BoostQuery boostDownQuery) {
        if (boostDownQueries == null) {
            boostDownQueries = new LinkedList<>();
        }
        boostDownQueries.add(boostDownQuery);
    }

    public void addCrieteria(Criteria value) {
        if (value != null) {
            criterion.add(value);
        }
    }

    public Criterion getCriterion() {
        return criterion;
    }

    public String getAppliedRuleIds() {
        return appliedRuleIdList.toString();
    }

    public void addAppliedRuleId(String id) {
        appliedRuleIdList.add(id);
    }

}
