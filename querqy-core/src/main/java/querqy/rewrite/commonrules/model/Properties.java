package querqy.rewrite.commonrules.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Properties {
  private Instructions instructions;
  private Map<String, String> propertyMap;

  public Properties(Instructions instructions) {
    this.instructions = instructions;
  }

}
