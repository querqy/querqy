package querqy.model;

import java.util.Collection;
import java.util.LinkedList;

public class Criterion extends LinkedList<Criteria> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public Criterion() {
        super();
    }

    public Criterion(Collection<Criteria> criterion) {
        super(criterion);
    }

}
