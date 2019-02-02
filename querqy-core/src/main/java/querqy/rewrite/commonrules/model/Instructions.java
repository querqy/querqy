/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

/**
 * A list of Instructions. This represents all actions that are triggered by a
 * single matching rule.
 * 
 * @author rene
 *
 */
public class Instructions extends LinkedList<Instruction> {

    /**
      *
      */
    private static final long serialVersionUID = 1L;

    /**
     * Properties that are applicable to all {@link Instruction}s in this collection
     */
    private final Map<String, String> properties;

    public Instructions() {
        super();
        properties = new HashMap<>();
    }

    public Instructions(final Collection<Instruction> instructions) {
        super(instructions);
        properties = new HashMap<>();
    }

    public void addProperty(final String name, final String value) {
        properties.put(name, value);
    }

    public Optional<String> getProperty(final String name) {
        return Optional.ofNullable(properties.get(name));
    }

}
