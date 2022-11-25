package querqy.solr;

import querqy.lucene.rewrite.infologging.Sink;
import querqy.rewrite.RewriterFactory;

import java.util.List;

public class RewriterFactoryContext {

    private final RewriterFactory rewriterFactory;
    private final List<Sink> sinks;

    public RewriterFactoryContext(RewriterFactory rewriterFactory, List<Sink> sinks) {
        this.rewriterFactory = rewriterFactory;
        this.sinks = sinks;
    }

    public RewriterFactory getRewriterFactory() {
        return rewriterFactory;
    }

    public List<Sink> getSinks() {
        return sinks;
    }
}
