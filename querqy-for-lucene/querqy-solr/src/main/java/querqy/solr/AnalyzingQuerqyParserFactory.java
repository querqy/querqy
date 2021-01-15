package querqy.solr;

import com.google.common.base.Preconditions;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import querqy.parser.QuerqyParser;

/**
 * @author René Kriegler, @renekrie
 */
public class AnalyzingQuerqyParserFactory implements SolrQuerqyParserFactory {

    protected String synonymsfieldType = null;

    protected String queryParsingFieldType = null;

    @Override
    public void init(@SuppressWarnings("rawtypes") NamedList parserConfig, ResourceLoader loader) throws SolrException {

        synonymsfieldType = (String) parserConfig.get("synonymFieldType");
        queryParsingFieldType = (String) parserConfig
                .get("queryParsingFieldType");

        Preconditions.checkNotNull(queryParsingFieldType,
                "queryParsingFieldType configuration missing");

    }


    @Override
    public QuerqyParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {

        IndexSchema schema = req.getSchema();

        Analyzer rewriteAnalyzer = schema.getFieldTypeByName(queryParsingFieldType).getQueryAnalyzer();
        Analyzer synonymAnalyzer = (synonymsfieldType != null) ?
                schema.getFieldTypeByName(synonymsfieldType).getQueryAnalyzer() : null;

        return new AnalyzingQuerqyParser(rewriteAnalyzer, synonymAnalyzer);
    }

}