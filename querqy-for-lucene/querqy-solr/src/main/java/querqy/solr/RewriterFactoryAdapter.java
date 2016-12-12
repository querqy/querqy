package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;

public interface RewriterFactoryAdapter {

   RewriterFactory createRewriterFactory(NamedList<?> args, ResourceLoader resourceLoader) throws IOException;

}
