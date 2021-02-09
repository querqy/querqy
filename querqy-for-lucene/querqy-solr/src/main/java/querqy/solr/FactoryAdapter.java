package querqy.solr;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.util.ResourceLoader;

public interface FactoryAdapter<T> {

    /**
     * @param id  The id of the factory (and of the objects it creates)
     * @param args The configuration
     * @return The factory instance
     * @throws IOException If the factory cannot be loaded
     */
    T createFactory(String id, Map<String, Object> args) throws IOException;

    /**
     * The class of the object that is finally created by the adapted factory
     *
     * @return The class object
     */
    Class<?> getCreatedClass();

}
