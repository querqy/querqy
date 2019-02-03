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

    /**
     * This property is used for ordering Instructions objects. The creator of Instructions objects must ensure to
     * set a unique order value per Instructions. ord can be used to order rules, for example, by the order found in
     * rules.txt.
     *
     */
    public final int ord;

    public Instructions(int ord) {
        super();
        this.ord = ord;
        properties = new HashMap<>();
    }

    public Instructions(final int ord, final Collection<Instruction> instructions) {
        super(instructions);
        this.ord = ord;
        properties = new HashMap<>();
    }

    public void addProperty(final String name, final String value) {
        properties.put(name, value);
    }

    public Optional<String> getProperty(final String name) {
        return Optional.ofNullable(properties.get(name));
    }

}
