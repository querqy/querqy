package querqy.model;


import org.apache.commons.collections4.CollectionUtils;
import querqy.rewrite.commonrules.model.Action;

import java.util.List;
import java.util.stream.Collectors;

public class FilterCriteria implements Criteria {

    String field;
    String value;

    public FilterCriteria(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean isValid(Action action) {
        if (CollectionUtils.isEmpty(action.getProperties())) {
            return false;
        }
        return action.getProperties().get(0).getPropertyMap().getOrDefault(field, "").equals(value);
    }

    @Override
    public List<Action> apply(List<Action> actions) {
        return actions.parallelStream().filter(this::isValid).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "FilterCriteria{" +
                "field='" + field + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
