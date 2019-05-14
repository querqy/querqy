package querqy.solr.contrib;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;
import querqy.rewrite.contrib.ShingleRewriter;
import querqy.solr.FactoryAdapter;

import java.io.IOException;

/**
 * FactoryAdapter for {@link ShingleRewriterFactory}
 */
public class ShingleRewriterFactory implements FactoryAdapter<RewriterFactory> {

    @Override
    public RewriterFactory createFactory(final String id, NamedList<?> args, ResourceLoader resourceLoader) {
        Boolean acceptGeneratedTerms = args.getBooleanArg("acceptGeneratedTerms");
        boolean t = (acceptGeneratedTerms == null) ? false : acceptGeneratedTerms;
        return new querqy.rewrite.contrib.ShingleRewriterFactory(id, t);
    }

    @Override
    public Class<?> getCreatedClass() {
        return ShingleRewriter.class;
    }
}
