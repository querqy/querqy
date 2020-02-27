package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;

public interface FactoryAdapter<T> {

    /**
     * @param id  The id of the factory (and of the objects it creates)
     * @param args The configuration
     * @param resourceLoader A {@link ResourceLoader} to access resources in the context of the {@link org.apache.solr.core.SolrCore} or collection
     * @return The factory instance
     * @throws IOException If the factory cannot be loaded
     */
    T createFactory(String id, NamedList<?> args, ResourceLoader resourceLoader) throws IOException;

    /**
     * The class of the object that is finally created by the adapted factory
     *
     * @return The class object
     */
    Class<?> getCreatedClass();

}
