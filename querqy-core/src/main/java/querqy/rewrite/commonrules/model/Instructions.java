/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
    private static final long serialVersionUID = 2L;

    /**
     * Properties that are applicable to all {@link Instruction}s in this collection
     */
    private final InstructionsProperties properties;

    private final Object id;

    /**
     * This property is used for ordering Instructions objects. The creator of Instructions objects must ensure to
     * set a unique order value per Instructions. ord can be used to order rules, for example, by the order found in
     * rules.txt.
     *
     */
    private final int ord;

    public Instructions(final int ord, Object id, final Collection<Instruction> instructions,
                        final InstructionsProperties properties) {
        super();

        instructions.forEach(instruction -> {
            if (instruction instanceof DeleteInstruction) {
                this.addLast(instruction);
            } else {
                this.addFirst(instruction);
            }
        });

        this.ord = ord;
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        this.id = id;
        this.properties = properties;
    }

    public Instructions(final int ord, final Object id, final Collection<Instruction> instructions) {
        this(ord, id, instructions, new InstructionsProperties(Collections.emptyMap()));
    }

    public int getOrd() {
        return ord;
    }

    public Object getId() {
        return id;
    }

    public Optional<Object> getProperty(final String name) {
        return properties.getProperty(name);
    }

    public InstructionsProperties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "Instructions{" +
                "ord=" + ord +
                ", List=" + super.toString() +
                '}';
    }
}
