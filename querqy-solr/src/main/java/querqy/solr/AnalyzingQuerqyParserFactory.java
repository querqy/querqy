/**
 * 
 */
package querqy.solr;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;

import com.google.common.base.Preconditions;

import querqy.lucene.parser.AnalyzingQuerqyParser;
import querqy.parser.QuerqyParser;

/**
 * @author René Kriegler, @renekrie
 * 
 */
public class AnalyzingQuerqyParserFactory implements SolrQuerqyParserFactory {

	protected String synonymsfieldType = null;

	protected String queryParsingFieldType = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * querqy.solr.SolrQuerqyParserFactory#init(org.apache.solr.common.util.
	 * NamedList, org.apache.lucene.analysis.util.ResourceLoader)
	 */
	@Override
	public void init(@SuppressWarnings("rawtypes") NamedList parserConfig, ResourceLoader loader)
			throws IOException, SolrException {

		synonymsfieldType = (String) parserConfig.get("synonymFieldType");
		queryParsingFieldType = (String) parserConfig
				.get("queryParsingFieldType");

		Preconditions.checkNotNull(queryParsingFieldType,
				"queryParsingFieldType configuration missing");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see querqy.solr.SolrQuerqyParserFactory#createParser(java.lang.String,
	 * org.apache.solr.common.params.SolrParams,
	 * org.apache.solr.common.params.SolrParams,
	 * org.apache.solr.request.SolrQueryRequest)
	 */
	@Override
	public QuerqyParser createParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		
		IndexSchema schema = req.getSchema();

		Analyzer rewriteAnalyzer = schema.getFieldTypeByName(
				queryParsingFieldType).getQueryAnalyzer();
		Analyzer synonymAnalyzer = (synonymsfieldType != null) ? schema.getFieldTypeByName(synonymsfieldType).getQueryAnalyzer() : null;

		return new AnalyzingQuerqyParser(rewriteAnalyzer, synonymAnalyzer);
	}

}
