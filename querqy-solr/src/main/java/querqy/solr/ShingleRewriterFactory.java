package querqy.solr;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;

import java.io.IOException;

/**
 * FactoryAdapter for {@link ShingleRewriterFactory}
 */
public class ShingleRewriterFactory implements RewriterFactoryAdapter {

    @Override
    public RewriterFactory createRewriterFactory(NamedList<?> args, ResourceLoader resourceLoader) throws IOException {
        return new querqy.rewrite.contrib.ShingleRewriterFactory();
    }
}
