package querqy.rewrite.commonrules.model;

import java.util.Collection;
import java.util.LinkedList;

public class Criteria extends LinkedList<Criterion> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public Criteria() {
        super();
    }

    public Criteria(Collection<Criterion> criterion) {
        super(criterion);
    }

}
