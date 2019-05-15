package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface InstructionsTestSupport {

    static Instructions instructions(final int ord, final String name, final Object value) {
        final Map<String, Object> props = new HashMap<>(1);
        props.put(name, value);
        return new Instructions(ord, Integer.toString(ord), Collections.emptyList(), props);
    }

    static Instructions instructions(final int ord, final Collection<Instruction> instructions) {
        return new Instructions(ord, Integer.toString(ord), instructions, Collections.emptyMap());
    }

    static Instructions instructions(final int ord) {
        return new Instructions(ord, Integer.toString(ord), Collections.emptyList(), Collections.emptyMap());
    }

}
