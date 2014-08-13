/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;

import querqy.parser.QuerqyParser;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class SimpleQuerqyQParserFactory implements SolrQuerqyParserFactory {

	protected Class<? extends QuerqyParser> querqyParserClass;
	
	/* (non-Javadoc)
	 * @see querqy.solr.QuerqyQParserFactory#init(org.apache.solr.common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
	 */
	@Override
	public void init(@SuppressWarnings("rawtypes") NamedList parserConfig, ResourceLoader loader) throws IOException, SolrException {
		
		 String className = (String) parserConfig.get("class");
	      if (className == null) {
	         throw new IOException("Missing attribute 'class' in querqy parser configuration");
	      }

	      querqyParserClass = loader.findClass(className, QuerqyParser.class);

	}

	/* (non-Javadoc)
	 * @see querqy.solr.QuerqyQParserFactory#createParser(java.lang.String, org.apache.solr.common.params.SolrParams, org.apache.solr.common.params.SolrParams, org.apache.solr.request.SolrQueryRequest)
	 */
	@Override
	public QuerqyParser createParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		try {
			return querqyParserClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} 
	}

}
