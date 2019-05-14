/**
 * 
 */
package querqy.rewrite.commonrules.model;

import static querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames.ID;

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

    public interface StandardPropertyNames {
        String ID = "_id";
        String LOG_MESSAGE = "_log";
    }

    /**
      *
      */
    private static final long serialVersionUID = 1L;

    /**
     * Properties that are applicable to all {@link Instruction}s in this collection
     */
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * This property is used for ordering Instructions objects. The creator of Instructions objects must ensure to
     * set a unique order value per Instructions. ord can be used to order rules, for example, by the order found in
     * rules.txt.
     *
     */
    public final int ord;

    public Instructions(final int ord) {
        super();
        this.ord = ord;
    }

    public Instructions(final int ord, final Collection<Instruction> instructions) {
        super(instructions);
        this.ord = ord;
    }

    public void addProperty(final String name, final Object value) {
        properties.put(name, value);
    }

    // TODO We rely on someone setting the ID property (i.e. SimpleCommonRulesParser). Change the parser so that
    // we can pass the ID to the constructor of Instructions
    public Object getId() {
        return properties.get(ID);
    }

    public Optional<Object> getProperty(final String name) {
        return Optional.ofNullable(properties.get(name));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

}
