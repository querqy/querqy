package querqy.model.logging;

import java.util.LinkedList;
import java.util.List;

public class RewriteLogging {

    private final List<Entry> rewriteChain = new LinkedList<>();

    public void put(final String rewriterId, final List<ActionLogging> actions) {
        rewriteChain.add(new Entry(rewriterId, actions));
    }

    public static class Entry {
        private final String rewriterId;
        private final List<ActionLogging> actions;

        public Entry(String rewriterId, List<ActionLogging> actions) {
            this.rewriterId = rewriterId;
            this.actions = actions;
        }
    }

}
