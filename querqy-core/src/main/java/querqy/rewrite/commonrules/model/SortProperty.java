package querqy.rewrite.commonrules.model;

import lombok.Data;

@Data
public class SortProperty {

  private String propertyName;
  private boolean asc;

  public SortProperty(String propertyName, boolean asc) {
    this.propertyName = propertyName;
    this.asc = asc;
  }

  public SortProperty(String sortPropertyStr) {
    if (sortPropertyStr == null || !sortPropertyStr.contains(" ")) {
      throw new IllegalArgumentException("sortPropertyStr must not be null");
    }

    String[] property = sortPropertyStr.split("\\s+");
    if (property.length < 2) {
      throw new IllegalArgumentException("sortPropertyStr must contain the type asc or desc");
    }
    propertyName = property[0];
    String propertyOrder = property[1].toLowerCase();

    switch (propertyOrder) {
      case "asc":
        this.asc = true;
        break;
      case "desc":
        this.asc = false;
        break;
      default:
        throw new IllegalArgumentException("sortPropertyStr must contain the type asc or desc");
    }
  }

}
