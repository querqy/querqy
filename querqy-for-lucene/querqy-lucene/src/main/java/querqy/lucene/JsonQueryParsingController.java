package querqy.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.ProductFloatFunction;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import querqy.lucene.LuceneSearchEngineRequestAdapter.SyntaxException;
import querqy.lucene.rewrite.AdditiveBoostFunction;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.LuceneQueryBuilder;
import querqy.lucene.rewrite.LuceneTermQueryBuilder;
import querqy.lucene.rewrite.SearchFieldsAndBoosting;
import querqy.lucene.rewrite.SearchFieldsAndBoosting.FieldBoostModel;
import querqy.lucene.rewrite.TermQueryBuilder;
import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.RawQuery;
import querqy.model.builder.impl.ExpandedQueryBuilder;
import querqy.parser.QuerqyParser;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.ContextAwareQueryRewriter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static querqy.model.builder.impl.ExpandedQueryBuilder.expanded;

/**
 * Created by rene on 23/05/2017.
 */
public class JsonQueryParsingController extends QueryParsingController {

    private final Map request;

    public JsonQueryParsingController(final Map request, final LuceneSearchEngineRequestAdapter requestAdapter) {
        super(requestAdapter);

        this.request = request;
    }

    @Override
    public ExpandedQuery createExpandedQuery() {

        final ExpandedQueryBuilder expanded = new ExpandedQueryBuilder(request);
        return expanded.build();
    }
}
