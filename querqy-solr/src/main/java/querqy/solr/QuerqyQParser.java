/**
 * 
 */
package querqy.solr;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.ExtendedDismaxQParser;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.SolrPluginUtils;

import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.lucene.IndexStats;
import querqy.rewrite.lucene.LuceneQueryBuilder;

/**
 * @author rene
 *
 */
public class QuerqyQParser extends ExtendedDismaxQParser {
    
    static final String MATCH_ALL = "*:*";
    
    // the QF parsing code is copied from org.apache.solr.util.SolrPluginUtils and 
    // org.apache.solr.search.DisMaxQParser, replacing the default boost factor null with 1f
    private static final Pattern whitespacePattern = Pattern.compile("\\s+");
    private static final Pattern caratPattern = Pattern.compile("\\^");
    
    final Analyzer queryAnalyzer;
    final RewriteChain rewriteChain;
    final IndexStats indexStats;
    final QuerqyParser querqyParser;
    
    protected PublicExtendedDismaxConfiguration config;
    protected List<Query> boostQueries;

    public QuerqyQParser(String qstr, SolrParams localParams, SolrParams params,
            SolrQueryRequest req, RewriteChain rewriteChain, IndexStats indexStats, QuerqyParser querqyParser) {
    	
        super(qstr, localParams, params, req);
        
        this.querqyParser = querqyParser;
        
        if (config == null) {
        	// this is a hack that works around ExtendedDismaxQParser keeping the config member var private
        	// and avoids calling createConfiguration() twice
        	config = createConfiguration(qstr, localParams, params, req);
        }
        IndexSchema schema = req.getSchema();
        queryAnalyzer = schema.getQueryAnalyzer();
        this.rewriteChain = rewriteChain;
        this.indexStats = indexStats;
    }
    
    @Override
    protected PublicExtendedDismaxConfiguration createConfiguration(String qstr,
    		SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    	// this is a hack that works around ExtendedDismaxQParser keeping the config member var private
    	// and avoids calling createConfiguration() twice
    	this.config = new PublicExtendedDismaxConfiguration(localParams, params, req);//super.createConfiguration(qstr, localParams, params, req);
    	return config;
    }

    /* (non-Javadoc)
     * @see org.apache.solr.search.QParser#parse()
     */
    @Override
    public Query parse() throws SyntaxError {
        
        // TODO q.alt
        String userQuery = getString();
        if (userQuery == null) {
            throw new SyntaxError("query string is null");
        }
        userQuery = userQuery.trim();
        if (userQuery.length() == 0) {
            throw new SyntaxError("query string is empty");
        }
        
        Query mainQuery = makeMainQuery(userQuery);
        applyMinShouldMatch(mainQuery);

        boostQueries = getBoostQueries();
        List<Query> boostFunctions = getBoostFunctions();

        boolean hasBoost = (boostQueries != null && !boostQueries.isEmpty())
        		|| (boostFunctions != null && !boostFunctions.isEmpty());
        
        if (hasBoost) {
        	
        	BooleanQuery bq = new BooleanQuery(true);
        	bq.add(mainQuery, Occur.MUST);
        	
        	if (boostQueries != null) {
        		for(Query f : boostQueries) {
        			bq.add(f, BooleanClause.Occur.SHOULD);
        		}
        	}
        	
        	if (boostFunctions != null) {
        		for(Query f : boostFunctions) {
        			bq.add(f, BooleanClause.Occur.SHOULD);
        		}
        	}
        	
        	mainQuery = bq;
        	
        }
        
        return mainQuery;
        
    }
    
    /**
     * @param query
     */
    public void applyMinShouldMatch(Query query) {
    	
    	if (!(query instanceof BooleanQuery)) {
    		return;
    	}
    	
    	BooleanQuery bq = (BooleanQuery) query;
    	BooleanClause[] clauses = bq.getClauses();
    	if (clauses.length < 2) {
    		return;
    	}
    	
    	for (BooleanClause clause: clauses) {
    		if ((clause.getQuery() instanceof BooleanQuery) && (clause.getOccur() != Occur.MUST)) {
    			return; // seems to be a complex query with sub queries - do not apply mm
    		}
    	}
    	
    	SolrPluginUtils.setMinShouldMatch(bq, config.getMinShouldMatch());
    	
    	
    }
        
    public Query makeMainQuery(String queryString) throws SyntaxError {
    	if ((queryString.charAt(0) == '*') && (queryString.length() == 1 || MATCH_ALL.equals(queryString))) {
            return new MatchAllDocsQuery();
        }
        
        SolrParams solrParams = SolrParams.wrapDefaults(localParams, params);
        Map<String, Float> queryFields = parseQueryFields(req.getSchema(), solrParams);
        
        LuceneQueryBuilder builder = new LuceneQueryBuilder(queryAnalyzer, queryFields, indexStats, config.getTieBreaker());
        
        querqy.model.Query q = querqyParser.parse(qstr);
        
        return builder.createQuery(rewriteChain.rewrite(q, Collections.<String,Object>emptyMap()));
    }
    
    /**
     * Copied from DisMaxQParser
     * @param solrParams
     * @return
     * @throws SyntaxError
     */
    public Query getAlternateUserQuery(SolrParams solrParams) throws SyntaxError {
        String altQ = solrParams.get(DisMaxParams.ALTQ);
        if (altQ != null) {
          QParser altQParser = subQuery(altQ, null);
          return altQParser.getQuery();
        } else {
          return null;
        }
      }
    
    
    
    /**
     * Given a string containing fieldNames and boost info,
     * converts it to a Map from field name to boost info.
     *
     * <p>
     * Doesn't care if boost info is negative, you're on your own.
     * </p>
     * <p>
     * Doesn't care if boost info is missing, again: you're on your own.
     * </p>
     *
     * @param in a String like "fieldOne^2.3 fieldTwo fieldThree^-0.4"
     * @return Map of fieldOne =&gt; 2.3, fieldTwo =&gt; null, fieldThree =&gt; -0.4
     */
    public static Map<String,Float> parseFieldBoosts(String in) {
      return parseFieldBoosts(new String[]{in});
    }
    
    /**
     * Like <code>parseFieldBoosts(String)</code>, but parses all the strings
     * in the provided array (which may be null).
     *
     * @param fieldLists an array of Strings eg. <code>{"fieldOne^2.3", "fieldTwo", fieldThree^-0.4}</code>
     * @return Map of fieldOne =&gt; 2.3, fieldTwo =&gt; null, fieldThree =&gt; -0.4
     */
    public static Map<String,Float> parseFieldBoosts(String[] fieldLists) {
      if (null == fieldLists || 0 == fieldLists.length) {
        return new HashMap<>();
      }
      Map<String, Float> out = new HashMap<>(7);
      for (String in : fieldLists) {
        if (null == in) {
          continue;
        }
        in = in.trim();
        if(in.length()==0) {
          continue;
        }
        
        String[] bb = whitespacePattern.split(in);
        for (String s : bb) {
          String[] bbb = caratPattern.split(s);
          out.put(bbb[0], 1 == bbb.length ? 1f : Float.valueOf(bbb[1]));
        }
      }
      return out;
    }
    
    
    /**
     * Copied from DisMxQParser (as we don't handle user fields/aliases yet)
     * 
     * Uses {@link SolrPluginUtils#parseFieldBoosts(String)} with the 'qf' parameter. Falls back to the 'df' parameter
     * or {@link org.apache.solr.schema.IndexSchema#getDefaultSearchFieldName()}.
     */
    public static Map<String, Float> parseQueryFields(final IndexSchema indexSchema, final SolrParams solrParams)
        throws SyntaxError {
      Map<String, Float> queryFields = parseFieldBoosts(solrParams.getParams(DisMaxParams.QF));
      if (queryFields.isEmpty()) {
        String df = QueryParsing.getDefaultField(indexSchema, solrParams.get(CommonParams.DF));
        if (df == null) {
          throw new SyntaxError("Neither "+DisMaxParams.QF+", "+CommonParams.DF +", nor the default search field are present.");
        }
        queryFields.put(df, 1.0f);
      }
      return queryFields;
    }
    
    public class PublicExtendedDismaxConfiguration extends ExtendedDismaxConfiguration {

    	/**
         * @param localParams
         * @param params
         * @param req
         */
    	public PublicExtendedDismaxConfiguration(SolrParams localParams,
    			SolrParams params, SolrQueryRequest req) {
    		super(localParams, params, req);
    	}
    	
    	public float getTieBreaker() { 
    		return tiebreaker;
    	}
    	
    	public String getMinShouldMatch() {
    		return minShouldMatch;
    	}
    	
    	
    }
    
}
