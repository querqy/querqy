package querqy.model;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import querqy.rewrite.commonrules.model.Action;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
public class SortCriteria implements Criteria {

  private String field;
  private String type;

  @Override
  public List<Action> apply(List<Action> actions) {
    Collections.sort(actions, new Comparator<Action>() {
      @Override
      public int compare(Action o1, Action o2) {

        if (!(CollectionUtils.isNotEmpty(o1.getProperties())
            && CollectionUtils.isNotEmpty(o2.getProperties())
            && o1.getProperties().get(0) != null
            && o2.getProperties().get(0) != null
            && o1.getProperties().get(0).getPropertyMap() != null
            && o2.getProperties().get(0).getPropertyMap() != null)) {
          return 0;
        }
        String o1Value = o1.getProperties().get(0).getPropertyMap().getOrDefault(field, "");
        String o2Value = o2.getProperties().get(0).getPropertyMap().getOrDefault(field, "");

        if (type.equals("asc")) {
          return o1Value.compareTo(o2Value);
        } else if (type.equals("desc")) {
          return o2Value.compareTo(o1Value);
        }
        return 0;
      }
    });
    return actions;
  }

  @Override
  public boolean isValid(Action action) {

    return (CollectionUtils.isNotEmpty(action.getProperties())
        && action.getProperties().get(0) != null
        && action.getProperties().get(0).getPropertyMap() != null
        && action.getProperties().get(0).getPropertyMap().containsKey(field));
  }

  @Override
  public String toString() {
    return "SortCriteria{" +
        "field='" + field + '\'' +
        ", type='" + type + '\'' +
        '}';
  }
}


