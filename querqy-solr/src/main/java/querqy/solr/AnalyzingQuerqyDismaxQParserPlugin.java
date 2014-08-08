/**
 * 
 */
package querqy.solr;

import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.QParser;

import querqy.lucene.parser.AnalyzingQuerqyParser;
import querqy.parser.QuerqyParser;

import com.google.common.base.Preconditions;

/**
 * @author rene
 *
 */
public class AnalyzingQuerqyDismaxQParserPlugin extends AbstractQuerqyDismaxQParserPlugin {

   String synonymsfieldType = null;
   String queryParsingFieldType = null;

   @Override
   public void init(NamedList args) {
      super.init(args);
      NamedList<?> parserList = (NamedList<?>) args.get("parser");
      
      Preconditions.checkNotNull(parserList, "parser configuration missing");

      synonymsfieldType = (String) parserList.get("synonymFieldType");
      queryParsingFieldType = (String) parserList.get("queryParsingFieldType");

      Preconditions.checkNotNull(queryParsingFieldType, "queryParsingFieldType configuration missing");
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String,
    * org.apache.solr.common.params.SolrParams,
    * org.apache.solr.common.params.SolrParams,
    * org.apache.solr.request.SolrQueryRequest)
    */
   @Override
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params,
         SolrQueryRequest req) {
      IndexSchema schema = req.getSchema();

      
      Analyzer rewriteAnalyzer = schema.getFieldTypeByName(queryParsingFieldType).getQueryAnalyzer();
      Analyzer synonymAnalyzer = (synonymsfieldType != null) ? schema.getFieldTypeByName(synonymsfieldType)
            .getQueryAnalyzer() : null;

      QuerqyParser querqyParser = new AnalyzingQuerqyParser(rewriteAnalyzer, synonymAnalyzer);

      return new QuerqyDismaxQParser(qstr, localParams, params, req, rewriteChain,
            new SolrIndexStats(req.getSearcher()), querqyParser);

   }
}
